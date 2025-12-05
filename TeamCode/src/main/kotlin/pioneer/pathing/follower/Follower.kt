package pioneer.pathing.follower

import com.qualcomm.robotcore.util.ElapsedTime
import pioneer.Constants
import pioneer.hardware.MecanumBase
import pioneer.helpers.FileLogger
import pioneer.helpers.MathUtils
import pioneer.helpers.PIDController
import pioneer.helpers.Pose
import pioneer.localization.Localizer
import pioneer.pathing.motionprofile.MotionProfile
import pioneer.pathing.motionprofile.MotionProfileGenerator
import pioneer.pathing.motionprofile.MotionState
import pioneer.pathing.paths.Path
import kotlin.math.*

class Follower(
    private val localizer: Localizer,
    private val mecanumBase: MecanumBase,
) {
    private var motionProfile: MotionProfile? = null
    private var headingProfile: MotionProfile? = null

    private var elapsedTime: ElapsedTime = ElapsedTime()
    private var xPID =
        PIDController(
            kp = Constants.Follower.X_KP,
            ki = Constants.Follower.X_KI,
            kd = Constants.Follower.X_KD,
        )
    private var yPID =
        PIDController(
            kp = Constants.Follower.Y_KP,
            ki = Constants.Follower.Y_KI,
            kd = Constants.Follower.Y_KD,
        )

    private var headingPID =
        PIDController(
            kp = Constants.Follower.THETA_KP,
            ki = Constants.Follower.THETA_KI,
            kd = Constants.Follower.THETA_KD,
        )

    var path: Path? = null
        set(value) {
            field = value
            // Reset the follower
            reset()
        }

    private var poseAtStartTheta: Double = 0.0

    val done: Boolean get() {
        // Ensure path and motion profile are set
        val path = this.path ?: return false
        val motionProfile = this.motionProfile ?: return false

        val targetPose = path.endPose
        val withinPositionTolerance = localizer.pose.distanceTo(targetPose) < Constants.Follower.POSITION_THRESHOLD
        val withinThetaTolerance = abs(localizer.pose.theta - targetPose.theta) < Constants.Follower.ROTATION_THRESHOLD
        val isTimeUp = elapsedTime.seconds() > motionProfile.duration()
        return withinThetaTolerance && isTimeUp // Could add position tolerance here
    }

    val targetState: MotionState?
        get() {
            val motionProfile =
                this.motionProfile ?: return run {
                    FileLogger.error("Follower", "No motion profile set")
                    null
                }
            // Get the current time in seconds since the follower started
            val t = elapsedTime.seconds()
            return motionProfile[t]
        }

    fun update(dt: Double) {
        // Ensure motion profile and path are set
        val motionProfile = this.motionProfile ?: return
        val path = this.path ?: return
        val headingProfile = this.headingProfile ?: return

        // Get the time since the follower started
        val t = elapsedTime.seconds().coerceAtMost(motionProfile.duration())
        // Get the target state from the motion profile
        val targetState = motionProfile[t]

        val headingTargetRaw = headingProfile[t]
        val startTheta = poseAtStartTheta

        val headingTarget =
            MotionState(
                x = startTheta + headingTargetRaw.x,
                v = headingTargetRaw.v,
                a = headingTargetRaw.a,
            )

        // Calculate the parameter t for the path based on the target state
        val pathT = path.getTFromLength(targetState.x)

        // Get the target point, first derivative (tangent), and second derivative (acceleration) from the path
        val pathTargetPose = path.getPose(pathT)
        val tangent =
            Pose(pathTargetPose.vx, pathTargetPose.vy) /
                sqrt(pathTargetPose.vx * pathTargetPose.vx + pathTargetPose.vy * pathTargetPose.vy)
        val curvature = path.getCurvature(pathT)

        // Calculate 2D target velocity and acceleration based on path derivatives
        val targetPose =
            Pose(
                x = pathTargetPose.x,
                y = pathTargetPose.y,
                vx = tangent.x * targetState.v,
                vy = tangent.y * targetState.v,
                ax = targetState.a * tangent.x + targetState.v.pow(2) * curvature * -tangent.y,
                ay = targetState.a * tangent.y + targetState.v.pow(2) * curvature * tangent.x,
                theta = headingTarget.x,
                omega = headingTarget.v,
                alpha = headingTarget.a,
            )

        // Calculate the error and convert to robot-centric coordinates
        val error =
            Pose(
                x = targetPose.x - localizer.pose.x,
                y = targetPose.y - localizer.pose.y,
                theta = MathUtils.normalizeRadians(headingTarget.x - localizer.pose.theta),
            )

        // Calculate the PID outputs
        val xCorrection = xPID.update(error.x, dt)
        val yCorrection = yPID.update(error.y, dt)
        val turnCorrection = headingPID.update(error.theta, dt)

        // Apply corrections to velocity directly
        // Rotate to convert to robot-centric coordinates
        val (vxRobot, vyRobot) =
            MathUtils.rotateVector(
                x = targetPose.vx + xCorrection,
                y = targetPose.vy + yCorrection,
                heading = -localizer.pose.theta,
            )

        val correctedPose =
            targetPose.copy(
                vx = vxRobot,
                vy = vyRobot,
                omega = targetPose.omega + turnCorrection,
            )

        mecanumBase.setDriveVA(correctedPose)
    }

    fun start() {
        // Reset the elapsed time
        elapsedTime.reset()
        // Reset the PID controllers
        xPID.reset()
        yPID.reset()
        headingPID.reset()
    }

    private fun reset() {
        // Recalculate the motion profile when the path is set
        motionProfile = calculateMotionProfile()
        headingProfile = calculateHeadingProfile()
        FileLogger.debug("Follower", "Motion Profile Time: ${motionProfile?.duration()}")
        FileLogger.debug("Follower", "Heading Profile Time: ${headingProfile?.duration()}")
        FileLogger.debug("Follower", "Heading Profile Start: ${headingProfile?.start()}")
    }

    private fun calculateVelocityConstraint(
        s: Double,
        path: Path,
    ): Double {
        val t = s / path.getLength()
        val k = path.getCurvature(t)
        val curveMaxVelocity = sqrt(Constants.Follower.MAX_CENTRIPETAL_ACCELERATION / abs(k))
        return if (curveMaxVelocity.isNaN()) {
            Constants.Follower.MAX_DRIVE_VELOCITY
        } else {
            min(Constants.Follower.MAX_DRIVE_VELOCITY, curveMaxVelocity)
        }
    }

    private fun calculateMotionProfile(): MotionProfile? {
        val path = this.path ?: return null
        val totalDistance = path.getLength()
        val startState = MotionState(0.0, 0.0, 0.0)
        val endState = MotionState(totalDistance, 0.0, 0.0)
        val velocityConstraint = { s: Double ->
            calculateVelocityConstraint(s, path)
        }
        val accelerationConstraint = { s: Double ->
            // Constant acceleration constraint
            Constants.Follower.MAX_DRIVE_ACCELERATION
        }
        return MotionProfileGenerator.generateMotionProfile(
            startState,
            endState,
            velocityConstraint,
            accelerationConstraint,
        )
    }

    private fun calculateHeadingProfile(): MotionProfile? {
        val path = this.path ?: return null

        val startTheta = localizer.pose.theta
        poseAtStartTheta = startTheta
        val endTheta = path.endPose.theta

        // Offset everything so profile is generated from 0 → Δθ
        val deltaTheta = MathUtils.normalizeRadians(endTheta - startTheta)

        // Raw profile always starts from 0 → deltaTheta
        val rawProfile =
            MotionProfileGenerator.generateMotionProfile(
                MotionState(0.0, 0.0, 0.0),
                MotionState(deltaTheta, 0.0, 0.0),
                { Constants.Follower.MAX_ANGULAR_VELOCITY },
                { Constants.Follower.MAX_ANGULAR_ACCELERATION },
            )

        return rawProfile
    }
}

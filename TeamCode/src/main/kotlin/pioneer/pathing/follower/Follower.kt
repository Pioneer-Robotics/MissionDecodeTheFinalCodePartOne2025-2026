package pioneer.pathing.follower

import com.qualcomm.robotcore.util.ElapsedTime
import pioneer.hardware.MecanumBase
import pioneer.helpers.FileLogger
import pioneer.helpers.PIDController
import pioneer.helpers.Pose
import pioneer.localization.Localizer
import pioneer.pathing.motionprofile.MotionProfile
import pioneer.pathing.motionprofile.MotionProfileGenerator
import pioneer.pathing.motionprofile.MotionState
import pioneer.pathing.paths.Path
import kotlin.math.*
import pioneer.constants.Follower as FollowerConstants

class Follower(
    private val localizer: Localizer,
    private val mecanumBase: MecanumBase,
) {
    private var motionProfile: MotionProfile? = null
    private var elapsedTime: ElapsedTime = ElapsedTime()
    private var xPID =
        PIDController(
            kp = FollowerConstants.X_KP,
            ki = FollowerConstants.X_KI,
            kd = FollowerConstants.X_KD,
        )
    private var yPID =
        PIDController(
            kp = FollowerConstants.Y_KP,
            ki = FollowerConstants.Y_KI,
            kd = FollowerConstants.Y_KD,
        )

    private var thetaPID =
        PIDController(
            kp = FollowerConstants.THETA_KP,
            ki = FollowerConstants.THETA_KI,
            kd = FollowerConstants.THETA_KD,
        )

    var path: Path? = null
        set(value) {
            field = value
            // Reset the follower
            reset()
        }

    val done: Boolean get() {
        // Ensure path and motion profile are set
        val path = this.path ?: return false
        val motionProfile = this.motionProfile ?: return false

        val targetPose = path.endPose
        val withinPositionTolerance = localizer.pose.distanceTo(targetPose) < FollowerConstants.POSITION_TOLERANCE
        val withinThetaTolerance = abs(localizer.pose.theta - targetPose.theta) < FollowerConstants.ROTATION_TOLERANCE
        val isTimeUp = elapsedTime.seconds() > motionProfile.duration()
        return withinThetaTolerance && isTimeUp // Could add position tolerance here
    }

    val targetState: MotionState?
        get() {
            val motionProfile = this.motionProfile ?: return run {
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

        // Get the time since the follower started
        val t = elapsedTime.seconds().coerceAtMost(motionProfile.duration())
        // Get the target state from the motion profile
        val targetState = motionProfile[t]

//        FileLogger.debug("Follower", "Target Velocity: " + targetState.v)
//        FileLogger.debug("Follower", "Target Acceleration " + targetState.a)

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
                theta = pathTargetPose.theta,
            )

        // Calculate the position error and convert to robot-centric coordinates
        val positionError =
            Pose(
                x = targetPose.x - localizer.pose.x,
                y = targetPose.y - localizer.pose.y,
                theta = targetPose.theta - localizer.pose.theta
            )

        // Calculate the PID outputs
        val xCorrection = xPID.update(positionError.x, dt)
        val yCorrection = yPID.update(positionError.y, dt)
        val thetaCorrection = thetaPID.update(positionError.theta, dt)

        // Apply corrections to velocity directly
        // Rotate to convert to robot-centric coordinates
        val correctedPose =
            targetPose
                .copy(
                    vx = targetPose.vx + xCorrection,
                    vy = targetPose.vy + yCorrection,
                    omega = targetPose.omega + thetaCorrection
                ).rotate(-localizer.pose.theta)

//        FileLogger.debug("Follower", "Target pose: $targetPose")
//        FileLogger.debug("Follower", "Corrected pose: $correctedPose")

        mecanumBase.setDriveVA(correctedPose)
    }

    fun start() {
        // Reset the elapsed time
        elapsedTime.reset()
        // Reset the PID controllers
        xPID.reset()
        yPID.reset()
    }

    private fun reset() {
        // Recalculate the motion profile when the path is set
        calculateMotionProfile()
    }

    private fun calculateVelocityConstraint(s: Double, path: Path): Double {
        val t = s / path.getLength()
        val k = path.getCurvature(t)
        val curveMaxVelocity = sqrt(FollowerConstants.MAX_CENTRIPETAL_ACCELERATION / abs(k))
        return if (curveMaxVelocity.isNaN()) {
            FollowerConstants.MAX_DRIVE_VELOCITY
        } else {
            min(FollowerConstants.MAX_DRIVE_VELOCITY, curveMaxVelocity)
        }
    }

    private fun calculateMotionProfile() {
        val path = this.path ?: return run { motionProfile = null }
        val totalDistance = path.getLength()
        val startState = MotionState(0.0, 0.0, 0.0)
        val endState = MotionState(totalDistance, 0.0, 0.0)
        val velocityConstraint = { s: Double ->
            calculateVelocityConstraint(s, path)
        }
        val accelerationConstraint = { s: Double ->
            // Constant acceleration constraint
            FollowerConstants.MAX_DRIVE_ACCELERATION
        }
        motionProfile =
            MotionProfileGenerator.generateMotionProfile(
                startState,
                endState,
                velocityConstraint,
                accelerationConstraint,
            )
    }
}

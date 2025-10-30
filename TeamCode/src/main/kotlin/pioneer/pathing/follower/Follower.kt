package pioneer.pathing.follower

import com.qualcomm.robotcore.util.ElapsedTime
import pioneer.Bot
import pioneer.constants.Follower as FollowerConstants
import pioneer.helpers.FileLogger
import pioneer.helpers.PIDController
import pioneer.helpers.Pose
import pioneer.pathing.motionprofile.MotionProfile
import pioneer.pathing.motionprofile.MotionProfileGenerator
import pioneer.pathing.motionprofile.MotionState
import pioneer.pathing.paths.Path
import kotlin.math.*

class Follower(private val bot: Bot) {
    var motionProfile: MotionProfile? = null
    private var elapsedTime: ElapsedTime = ElapsedTime()
    private var xPID = PIDController(
        kp = FollowerConstants.X_KP,
        ki = FollowerConstants.X_KI,
        kd = FollowerConstants.X_KD
    )
    private var yPID = PIDController(
        kp = FollowerConstants.Y_KP,
        ki = FollowerConstants.Y_KI,
        kd = FollowerConstants.Y_KD
    )

    var path: Path? = null
        set(value) {
            field = value
            // Reset the follower
            reset()
        }

    var done: Boolean = false
        get() {
            if (motionProfile == null || path == null) {
                FileLogger.error("Follower", "No path or motion profile set")
                return false
            }
            return elapsedTime.seconds() > motionProfile!!.duration()
            // Maybe add position or velocity tolerance
        }

    var targetState: MotionState? = null
        get() {
            if (motionProfile == null) {
                FileLogger.error("Follower", "Motion profile is not set")
                return null
            }
            // Get the current time in seconds since the follower started
            val t = elapsedTime.seconds()
            return motionProfile!![t]
        }

    fun update(dt: Double) {
        if (motionProfile == null || path == null) {
            FileLogger.error("Follower", "No path or motion profile set")
            return
        }
        // Get the time since the follower started
        val t = elapsedTime.seconds().coerceAtMost(motionProfile!!.duration())
        // Get the target state from the motion profile
        val targetState = motionProfile!![t]

        // Calculate the parameter t for the path based on the target state
        val pathT = path!!.getTFromLength(targetState.x)

        // Get the target point, first derivative (tangent), and second derivative (acceleration) from the path
        val targetPoint = path!!.getPoint(pathT)
        val tangent = path!!.getTangent(pathT).normalize()
        val targetPointSecondDerivative = path!!.getSecondDerivative(pathT)

        // Calculate the position error and convert to robot-centric coordinates
        var positionError = Pose(
            x = targetPoint.x - bot.localizer.pose.x,
            y = targetPoint.y - bot.localizer.pose.y
        ).rotate(-bot.localizer.pose.theta)

        // Calculate 2D target velocity and acceleration based on path derivatives
        val targetPose = Pose(
            x = targetPoint.x,
            y = targetPoint.y,
            vx = tangent.x * targetState.v,
            vy = tangent.y * targetState.v,
            ax = targetPointSecondDerivative.x * (targetState.v * targetState.v) + tangent.x * targetState.a,
            ay = targetPointSecondDerivative.y * (targetState.v * targetState.v) + tangent.y * targetState.a,
            theta = bot.localizer.pose.theta
        )

        // Convert target velocity and acceleration to robot-centric coordinates
        val rotatedTargetPose = targetPose.rotate(-bot.localizer.pose.theta)

        // Calculate the PID outputs
        val xCorrection = xPID.update(positionError.x, dt)
        val yCorrection = yPID.update(positionError.y, dt)

        // Apply corrections to velocity directly
        val correctedPose = rotatedTargetPose.copy(
            vx = rotatedTargetPose.vx + xCorrection,
            vy = rotatedTargetPose.vy + yCorrection
        )

        // TODO: Heading interpolation
        // TODO: Add heading error correction

        bot.mecanumBase.setDriveVA(correctedPose)
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

    private fun calculateMotionProfile() {
        if (path != null) {
            val totalDistance = path!!.getLength()
            val startState = MotionState(0.0, 0.0, 0.0)
            val endState = MotionState(totalDistance, 0.0, 0.0)
            val velocityConstraint = { s: Double ->
                // Velocity constraint based on path curvature
                val t = s / totalDistance
                val k = path!!.getCurvature(t)
                val curveMaxVelocity = sqrt(FollowerConstants.MAX_CENTRIPETAL_ACCELERATION / abs(k))
                if (curveMaxVelocity.isNaN()) {
                    FollowerConstants.MAX_DRIVE_VELOCITY
                } else {
                    min(FollowerConstants.MAX_DRIVE_VELOCITY, curveMaxVelocity)
                }
            }
            val accelerationConstraint = { s: Double ->
                // Constant acceleration constraint
                FollowerConstants.MAX_DRIVE_ACCELERATION
            }
            motionProfile = MotionProfileGenerator.generateMotionProfile(
                startState,
                endState,
                velocityConstraint,
                accelerationConstraint,
            )
        } else {
            motionProfile = null
        }
    }
}

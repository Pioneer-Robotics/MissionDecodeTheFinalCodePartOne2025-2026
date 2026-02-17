package pioneer.pathing.follower

import com.qualcomm.robotcore.util.ElapsedTime
import pioneer.Constants
import pioneer.hardware.MecanumBase
import pioneer.helpers.Chrono
import pioneer.helpers.FileLogger
import pioneer.helpers.MathUtils
import pioneer.helpers.PIDController
import pioneer.helpers.Pose
import pioneer.localization.Localizer
import pioneer.pathing.motionprofile.*
import pioneer.pathing.paths.Path
import kotlin.math.*

class Follower(
    private val localizer: Localizer,
    private val drive: MecanumBase,
) {
    private val timer = ElapsedTime()
    private val chrono = Chrono(false)

    private val pidVX = PIDController(Constants.Follower.X_KP, Constants.Follower.X_KI, Constants.Follower.X_KD)
    private val pidVY = PIDController(Constants.Follower.Y_KP, Constants.Follower.Y_KI, Constants.Follower.Y_KD)
    private val pidVHeading = PIDController(Constants.Follower.THETA_KP, Constants.Follower.THETA_KI, Constants.Follower.THETA_KD)

    // Second set of PIDs for direct position control to be used when path is done (if needed)
    private val pidXPos = PIDController(Constants.Follower.POS_X_KP, Constants.Follower.POS_X_KI, Constants.Follower.POS_X_KD)
    private val pidYPos = PIDController(Constants.Follower.POS_Y_KP, Constants.Follower.POS_Y_KI, Constants.Follower.POS_Y_KD)
    private val pidHeadingPos = PIDController(Constants.Follower.POS_THETA_KP, Constants.Follower.POS_THETA_KI, Constants.Follower.POS_THETA_KD)

    private var path: Path? = null
    private var profile: MotionProfile? = null

    var maxVelocity = Double.MAX_VALUE

    val targetState: MotionState?
        get() {
            val profile = profile ?: return null
            val t = timer.seconds()
            return profile[t]
        }

    val currentPath: Path?
        get() = path

    val isFollowing: Boolean
        get() = path != null

    val done: Boolean
        get() {
            val path = path ?: return true
            val profile = profile ?: return true
            val t = timer.seconds()
            if (t > profile.duration()) {
                // path time complete
                // check if within tolerances
                val withinPositionTolerance = localizer.pose.distanceTo(path.endPose) < Constants.Follower.POSITION_THRESHOLD
                val withinHeadingTolerance = abs(MathUtils.normalizeRadians(localizer.pose.theta - path.endPose.theta)) < Constants.Follower.ROTATION_THRESHOLD
                if (withinPositionTolerance && withinHeadingTolerance) {
                    return true
                }
            }
            return false
        }

    fun followPath(path: Path?, maxVelocity: Double = Double.MAX_VALUE) {
        this.maxVelocity = maxVelocity
        // Set new path and calculate motion profile
        this.path = path
        this.profile = calculateMotionProfile(path)
        // Reset PID controllers and timer
        pidVX.reset()
        pidVY.reset()
        pidVHeading.reset()
        timer.reset()

        FileLogger.debug("Follower", "Profile duration=${profile?.duration()}")
    }

    fun reset() {
        path = null
        profile = null
        pidVX.reset()
        pidVY.reset()
        pidVHeading.reset()
        drive.stop()
    }

    /**
     * Updates the follower and returns true if the path is complete.
     */
    fun update(): Boolean {
        val path = path ?: return true
        val profile = profile ?: return true
        val t = timer.seconds()

//        if (t > profile.duration()) { // Untested
//            // If path is done but not within tolerances
//            if (!done) {
//                // Switch to position PID control to correct final pose
//                val errorX = path.endPose.x - localizer.pose.x
//                val errorY = path.endPose.y - localizer.pose.y
//                val errorHeading = MathUtils.normalizeRadians(path.endPose.theta - localizer.pose.theta)
//                val vxCorrect = pidXPos.update(errorX, chrono.dt)
//                val vyCorrect = pidYPos.update(errorY, chrono.dt)
//                val omegaCorrect = pidHeadingPos.update(errorHeading, chrono.dt)
//                val driveCommand = Pose(
//                    vx = vxCorrect,
//                    vy = vyCorrect,
//                    omega = omegaCorrect,
//                    ax = 0.0,
//                    ay = 0.0,
//                    alpha = 0.0,
//                )
//                FileLogger.debug("Follower", "Final Pose Correction Drive Command: $driveCommand")
//                drive.setDriveVA(driveCommand)
//                return false
//            }
//        }

//        if (done) {
//            reset()
//            drive.stop()
//            return true
//        }

        val state = profile[t] // MotionState(x = s)
        val s = state.x

        // get the path parameter t from arc length s
        val pathT = path.getTFromLength(s)

        // desired pose
        val desired = path.getPose(pathT)

        // tangent -> direction of velocity
        val (tx, ty) = desired.run {
            // normalized tangent vector
            val mag = sqrt(vx * vx + vy * vy)
            if (mag != 0.0) (vx / mag) to (vy / mag) else 0.0 to 0.0
        }

        // desired field velocity components
        val vFieldX = tx * state.v
        val vFieldY = ty * state.v
        val aFieldX = tx * state.a
        val aFieldY = ty * state.a

        // position errors
        val errorFieldX = desired.x - localizer.pose.x
        val errorFieldY = desired.y - localizer.pose.y
        val errorHeading = MathUtils.normalizeRadians(desired.theta - localizer.pose.theta)

        // convert field velocity and acceleration to robot frame
        val (vx, vy) = MathUtils.rotateVector(vFieldX, vFieldY, -localizer.pose.theta)
        val (ax, ay) = MathUtils.rotateVector(aFieldX, aFieldY, -localizer.pose.theta)

        // convert position errors to robot frame
        val (errorX, errorY) = MathUtils.rotateVector(errorFieldX, errorFieldY, -localizer.pose.theta)

//        FileLogger.debug("Follower", "Position Error: $errorX, $errorY")

        // PID corrections
        chrono.update()
        val vxCorrect = pidVX.update(errorX, chrono.dt)
        val vyCorrect = pidVY.update(errorY, chrono.dt)
        val omegaCorrect = pidVHeading.update(errorHeading, chrono.dt)

        // final drive command
        val driveCommand = Pose(
            vx = vx + vxCorrect,
            vy = vy + vyCorrect,
            omega = omegaCorrect, // TODO: Optional heading profile
            ax = ax,
            ay = ay,
            alpha = 0.0,
        )

//        FileLogger.debug("Follower", "Drive Command: $driveCommand")
        drive.setDriveVA(driveCommand)

        return false // path not complete
    }

    private fun calculateVelocityConstraint(
        s: Double,
        path: Path,
    ): Double {
        val t = s / path.getLength()
        val k = path.getCurvature(t)
        val pathConstraint = path.velocityConstraint[s]
        val curveMaxVelocity = sqrt(Constants.Follower.MAX_CENTRIPETAL_ACCELERATION / abs(k))
        val velocityLimit = min(
            pathConstraint,
            min(
                maxVelocity, 
                Constants.Follower.MAX_DRIVE_VELOCITY
            )
        )
        return if (curveMaxVelocity.isNaN()) {
            velocityLimit
        } else {
            min(velocityLimit, curveMaxVelocity)
        }
    }

    private fun calculateMotionProfile(path: Path?): MotionProfile? {
        val path = path ?: return null

        // For now, paths always start and end at v=0.0, a=0.0
        val totalDistance = path.getLength()
        val startState = MotionState(0.0, 0.0, 0.0)
        val endState = MotionState(totalDistance, 0.0, 0.0)

        val velocityConstraint = { s: Double ->
            // Dynamic velocity constraint based on path curvature
            calculateVelocityConstraint(s, path)
        }

        val accelerationConstraint = { s: Double ->
            // Constant acceleration constraint
            Constants.Follower.MAX_DRIVE_ACCELERATION
        }

        FileLogger.debug(
            "Follower",
            "Start State: $startState" +
                    "End State: $endState"
        )

        return MotionProfileGenerator.generateMotionProfile(
            startState,
            endState,
            velocityConstraint,
            accelerationConstraint,
        )
    }
}

package pioneer

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.robotcore.hardware.DcMotorSimple
import pioneer.helpers.Pose
import kotlin.math.PI

@Config
object Constants {

    object HardwareNames{
        // Drive motors
        const val DRIVE_LEFT_FRONT = "driveLF"
        const val DRIVE_LEFT_BACK = "driveLB"
        const val DRIVE_RIGHT_FRONT = "driveRF"
        const val DRIVE_RIGHT_BACK = "driveRB"

        // Odometry
        const val ODO_LEFT = "odoLeft"
        const val ODO_RIGHT = "odoRight"
        const val ODO_CENTER = "odoCenter"

        // Pinpoint
        const val PINPOINT = "pinpoint"
    }

    // -------- Odometry (3-wheel) --------
    object Odometry {
        // geometry (cm)
        const val TRACK_WIDTH_CM = 26.5
        const val FORWARD_OFFSET_CM = 15.1
        const val WHEEL_DIAMETER_CM = 4.8

        // encoder
        const val TICKS_PER_REV = 2000.0
        val TICKS_TO_CM: Double = (WHEEL_DIAMETER_CM * PI) / TICKS_PER_REV
    }

    // -------- Drivebase (mecanum) --------
    object Drive {
        // geometry (cm)
        const val TRACK_WIDTH_CM = 0.0
        const val WHEEL_BASE_CM  = 0.0

        // limits
        const val MAX_DRIVE_MOTOR_VELOCITY_TPS = 2500.0
        const val MAX_FWD_VEL_CMPS = 150.0
        const val MAX_STRAFE_VEL_CMPS = 125.0
        const val DEFAULT_DRIVE_POWER = 0.7

        // motor directions (LF, LB, RF, RB)
        val MOTOR_DIRECTIONS = arrayOf(
            DcMotorSimple.Direction.REVERSE,
            DcMotorSimple.Direction.REVERSE,
            DcMotorSimple.Direction.FORWARD,
            DcMotorSimple.Direction.FORWARD
        )

        // Feedforward gains using Pose(x,y,theta)
        val kV = Pose(x = 0.0067, y = 0.0067, theta = 0.25)
        val kA = Pose(x = 0.0,    y = 0.0,    theta = 0.0)
        val kS = Pose(x = 0.0,    y = 0.0,    theta = 0.0)
    }

    // -------- Pinpoint (odometry pods) --------
    object Pinpoint {
        // offsets from tracking point (mm): +forward, +left
        const val Y_POD_OFFSET_MM = 0.0
        const val X_POD_OFFSET_MM = 134.0
        
        // encoder configuration. Y should increase left, X should increase forward
        val Y_ENCODER_DIRECTION = GoBildaPinpointDriver.EncoderDirection.FORWARD
        val X_ENCODER_DIRECTION = GoBildaPinpointDriver.EncoderDirection.FORWARD

        val ENCODER_RESOLUTION =
            GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_SWINGARM_POD
    }

    // -------- Follower (path following) --------
    object Follower {
        /** The threshold in cm to consider the target reached. */
        const val POSITION_THRESHOLD = 0.5

        /** The threshold in radians to consider the target heading reached. */
        const val ROTATION_THRESHOLD = 0.01

        /** The maximum drive velocity in cm per second. */
        const val MAX_DRIVE_VELOCITY = 100.0

        /** The maximum drive acceleration in cm per second squared. */
        const val MAX_DRIVE_ACCELERATION = 50.0

        /** The maximum centripetal acceleration that the robot can handle in cm/s^2. */
        const val MAX_CENTRIPETAL_ACCELERATION = (70.0 * 70.0) / 25.0

        // X-axis PID coefficients for the trajectory follower
        @JvmField var X_KP = 0.0
        @JvmField var X_KI = 0.0
        @JvmField var X_KD = 0.0

        // Y-axis PID coefficients for the trajectory follower
        @JvmField var Y_KP = 0.0
        @JvmField var Y_KI = 0.0
        @JvmField var Y_KD = 0.0
    }
}
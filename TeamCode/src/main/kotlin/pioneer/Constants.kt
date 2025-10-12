package pioneer

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.robotcore.hardware.DcMotorSimple
import pioneer.localization.Pose
import kotlin.math.PI

object Constants {

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
}
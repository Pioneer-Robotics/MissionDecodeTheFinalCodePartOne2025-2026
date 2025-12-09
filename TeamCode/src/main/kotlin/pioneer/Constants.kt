package pioneer

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.robotcore.hardware.DcMotorSimple
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import pioneer.general.AllianceColor
import pioneer.helpers.Pose
import kotlin.math.PI

object Constants {
    object HardwareNames {
        // Drive motors
        const val DRIVE_LEFT_FRONT = "driveLF"
        const val DRIVE_LEFT_BACK = "driveLB"
        const val DRIVE_RIGHT_FRONT = "driveRF"
        const val DRIVE_RIGHT_BACK = "driveRB"

        // Other motors
        const val FLYWHEEL = "flywheelMotor"
        const val INTAKE_MOTOR = "intakeMotor"
        const val TURRET_MOTOR = "turretMotor"
        const val SPINDEXER_MOTOR = "spindexerMotor"

        // Odometry
        const val ODO_LEFT = "odoLeft"
        const val ODO_RIGHT = "odoRight"
        const val ODO_CENTER = "odoCenter"

        // Pinpoint
        const val PINPOINT = "pinpoint"

        // Servos
        const val LAUNCH_SERVO = "launchServo"
        const val LAUNCH_SERVO_L = "launchServoL"
        const val LAUNCH_SERVO_R = "launchServoR"

        // Other
        const val WEBCAM = "Webcam 1"
        const val INTAKE_SENSOR = "intakeSensor"
    }

    // -------- Odometry (3-wheel) --------
    object Odometry {
        // geometry (cm)
        const val TRACK_WIDTH_CM = 26.5
        const val FORWARD_OFFSET_CM = 15.1
        const val WHEEL_DIAMETER_CM = 4.8

        // encoder
        const val TICKS_PER_REV = 2000.0
        const val TICKS_TO_CM = (WHEEL_DIAMETER_CM * PI) / TICKS_PER_REV
    }

    // -------- Drivebase (mecanum) --------
    @Config
    object Drive {
        // geometry (cm)
        const val TRACK_WIDTH_CM = 0.0
        const val WHEEL_BASE_CM = 0.0

        // limits
        const val MAX_MOTOR_VELOCITY_TPS = 2500.0
        const val MAX_FWD_VEL_CMPS = 150.0
        const val MAX_STRAFE_VEL_CMPS = 125.0
        const val DEFAULT_POWER = 0.7

        // Feedforward gains using Pose(x,y,theta)
//        @JvmField var kVX = 0.0
//        @JvmField var kVY = 0.0
//        @JvmField var kVT = 0.0
//        @JvmField var kAX = 0.0
//        @JvmField var kAY = 0.0
//        @JvmField var kAT = 0.0

        val kV = Pose(x = 0.0063, y = 0.0055, theta = -0.147)
        val kA = Pose(x = 0.0025, y = 0.001, theta = 0.0)
        val kS = Pose(x = 0.0, y = 0.0, theta = 0.0)

        val MOTOR_CONFIG =
            mapOf(
                HardwareNames.DRIVE_LEFT_FRONT to DcMotorSimple.Direction.REVERSE,
                HardwareNames.DRIVE_LEFT_BACK to DcMotorSimple.Direction.REVERSE,
                HardwareNames.DRIVE_RIGHT_FRONT to DcMotorSimple.Direction.FORWARD,
                HardwareNames.DRIVE_RIGHT_BACK to DcMotorSimple.Direction.FORWARD,
            )
    }

    // -------- Pinpoint (odometry pods) --------
    object Pinpoint {
        // offsets from tracking point (mm): +forward, +left
        const val Y_POD_OFFSET_MM = -156.7
        const val X_POD_OFFSET_MM = 67.0

        // encoder configuration. Y should increase left, X should increase forward
        val Y_ENCODER_DIRECTION = GoBildaPinpointDriver.EncoderDirection.REVERSED
        val X_ENCODER_DIRECTION = GoBildaPinpointDriver.EncoderDirection.FORWARD

        val ENCODER_RESOLUTION = GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_SWINGARM_POD
    }

    // -------- Follower (path following) --------
    @Config
    object Follower {
        /** The threshold in cm to consider the target reached. */
        const val POSITION_THRESHOLD = 0.5

        /** The threshold in radians to consider the target heading reached. */
        const val ROTATION_THRESHOLD = 0.05

        /** The maximum drive velocity in cm per second. */
        const val MAX_DRIVE_VELOCITY = 100.0

        /** The maximum drive acceleration in cm per second squared. */
        const val MAX_DRIVE_ACCELERATION = 50.0

        /** The maximum centripetal acceleration that the robot can handle in cm/s^2. */
        const val MAX_CENTRIPETAL_ACCELERATION = (70.0 * 70.0) / 25.0

        /** The maximum angular velocity in rad per second. */
        const val MAX_ANGULAR_VELOCITY = 1.0

        /** The maximum angular acceleration in rad per second squared. */
        const val MAX_ANGULAR_ACCELERATION = 10.0

        // X-axis PID coefficients for the trajectory follower
        @JvmField var X_KP = 10.0

        @JvmField var X_KI = 0.0

        @JvmField var X_KD = 0.0

        // Y-axis PID coefficients for the trajectory follower
        @JvmField var Y_KP = 10.0

        @JvmField var Y_KI = 0.0

        @JvmField var Y_KD = 0.0

        // Theta PID coefficients for heading interpolation
        @JvmField var THETA_KP = 0.5

        @JvmField var THETA_KI = 0.0

        @JvmField var THETA_KD = 0.0
    }

    object Camera {
        // Camera position constants (cm)
        val XYZ_UNITS = DistanceUnit.CM
        val XYZ_OFFSET: List<Double> = listOf(0.0, 0.0, 0.0)

        // Camera orientation constants (degrees)
        val RPY_UNITS = AngleUnit.DEGREES
        val RPY_OFFSET: List<Double> = listOf(0.0, -90.0, 0.0) // Pitch=-90 to face forward

        // //Lens Intrinsics
        // const val fx = 955.23
        // const val fy = 962.92
        // const val cx = 330.05
        // const val cy = 186.05

        // val distortionCoefficients = floatArrayOf(0.0573F, 2.0205F, -0.0331F, 0.0021F, -14.6155F, 0F, 0F, 0F)
    }

    @Config
    object Spindexer {
        @JvmField var KP = 0.00045

        @JvmField var KI = 0.0

        @JvmField var KD = 0.04

        @JvmField var KS_START = 0.05
        @JvmField var KS_STEP = 0.01

        @JvmField var MAX_POWER_RATE = 5.0

        const val POSITION_TOLERANCE_TICKS = 100
        const val TICKS_PER_REV = 8192

        // TODO: Tune these values when we test on the real hardware
        // Time required to confirm an artifact has been intaken (ms)
        const val CONFIRM_INTAKE_MS = 50

        // Max time the artifact can disappear without resetting confirmation (ms)
        const val CONFIRM_LOSS_MS = 10
    }

    object Turret {
        const val TICKS_PER_REV = 384.5 * 3
        const val HEIGHT = 0.0 // TODO MEASURE
        const val THETA = 0.93
        const val ANGLE_TOLERANCE_RADIANS = 0.075
    }

    object ServoPositions {
        const val LAUNCHER_REST = 0.067
        const val LAUNCHER_TRIGGERED = 0.315
    }

    object TransferData {
        var allianceColor = AllianceColor.NEUTRAL
        var turretPositionTicks = 0
        var spindexerPositionTicks = 0
    }
}

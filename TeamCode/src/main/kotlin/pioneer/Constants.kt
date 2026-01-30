package pioneer

import com.acmerobotics.dashboard.config.Config
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.robotcore.hardware.DcMotorSimple
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import pioneer.general.AllianceColor
import pioneer.helpers.Pose
import pioneer.opmodes.BaseOpMode.Verbose
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
        const val LED_DRIVER = "prism"
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
        const val MAX_DRIVE_VEL_CMPS = 150.0
        const val DEFAULT_POWER = 0.7

        // Feedforward gains using Pose(x,y,theta)
//        @JvmField var kVX = 0.0
//        @JvmField var kVY = 0.0
//        @JvmField var kVT = 0.0
//        @JvmField var kAX = 0.0
//        @JvmField var kAY = 0.0
//        @JvmField var kAT = 0.0

        val kV = Pose(x = 0.0063, y = 0.0055, theta = -0.147)
        val kA = Pose(x = 0.00125, y = 0.001, theta = 0.0)
        val kS = Pose(x = 0.11, y = 0.06, theta = 0.0)

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
        const val POSITION_THRESHOLD = 1.25

        /** The threshold in radians to consider the target heading reached. */
        const val ROTATION_THRESHOLD = 0.06

        /** The maximum drive velocity in cm per second. */
        const val MAX_DRIVE_VELOCITY = 110.0

        /**
         * The maximum drive acceleration in cm per second squared.
         * UPDATED: Reduced from 50.0 to 35.0 to prevent wheel slip
         */
        const val MAX_DRIVE_ACCELERATION = 35.0  // Was 50.0

        /**
         * The maximum centripetal acceleration that the robot can handle in cm/s^2.
         * UPDATED: Reduced from 196.0 to 60.0 to prevent lateral sliding in turns
         */
        const val MAX_CENTRIPETAL_ACCELERATION = 60.0  // Was (70.0 * 70.0) / 25.0 = 196.0

        /** The maximum angular velocity in rad per second. */
        const val MAX_ANGULAR_VELOCITY = 1.0

        /** The maximum angular acceleration in rad per second squared. */
        const val MAX_ANGULAR_ACCELERATION = 10.0

        // --- Standard Follower PID Tuned to Stay on the Path ---
        // X-axis PID coefficients for the trajectory follower
        // UPDATED: Increased from 5.0 to 10.0 for more aggressive error correction
        @JvmField var X_KP = 10.0  // Was 5.0 (was 7.0 before that)
        @JvmField var X_KI = 0.0
        @JvmField var X_KD = 0.0

        // Y-axis PID coefficients for the trajectory follower
        // UPDATED: Increased from 5.0 to 10.0 for more aggressive error correction
        @JvmField var Y_KP = 10.0  // Was 5.0 (was 7.0 before that)
        @JvmField var Y_KI = 0.0
        @JvmField var Y_KD = 0.0

        // Theta PID coefficients for heading interpolation
        // UPDATED: Increased from 3.0 to 6.0 for better heading control
        @JvmField var THETA_KP = 6.0  // Was 3.0 (was 5.0 before that)
        @JvmField var THETA_KI = 0.0
        @JvmField var THETA_KD = 0.0

        // --- Position PID coefficients tuned for final pose correction ---
        @JvmField var POS_X_KP = 0.0
        @JvmField var POS_X_KI = 0.0
        @JvmField var POS_X_KD = 0.0

        @JvmField var POS_Y_KP = 0.0
        @JvmField var POS_Y_KI = 0.0
        @JvmField var POS_Y_KD = 0.0

        @JvmField var POS_THETA_KP = 0.0
        @JvmField var POS_THETA_KI = 0.0
        @JvmField var POS_THETA_KD = 0.0

    }

    object Camera {
        // Camera position constants (cm)
        val XYZ_UNITS = DistanceUnit.CM
        val XYZ_OFFSET: List<Double> = listOf(17.5, 24.0, 30.5)

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

// ===================================================================
// TUNED SPINDEXER CONSTANTS - Replace in Constants.kt
// ===================================================================
//
// CRITICAL FIXES:
// 1. PID gains scaled up 100x-1000x for 8192 tpr encoder
// 2. Tolerances made coherent (tighter stopping than "reached")
// 3. Velocity tolerance lowered for faster settling
// 4. Added two-stage control (PID far away, gentle close in)
//
// TUNING METHODOLOGY:
// - Start with KP only, increase until slight oscillation
// - Add KD to dampen oscillation (about 10-20x KP)
// - Add small KI if steady-state error remains
// - Tune tolerances from tightest to loosest
//
// ===================================================================

    @Config
    object Spindexer {
        // ============== PID GAINS (MAIN FIX) ==============
        // These are tuned for 8192 ticks/rev encoder
        // Start with these, then tune on robot if needed

        @JvmField var KP = 0.015      // Was 0.000175 (86x increase)
        @JvmField var KI = 0.0001     // Was 0.00001 (10x increase)
        @JvmField var KD = 0.25       // Was 0.00045 (556x increase)

        // Static friction compensation
        @JvmField var KS_START = 0.05  // Was 0.03 (slightly higher)
        @JvmField var KS_STEP = 0.0    // Keep at 0

        // Power ramping (not currently used, keep as is)
        @JvmField var MAX_POWER_RATE = 100.0

        // ============== TOLERANCES (CRITICAL FIX) ==============
        // These MUST be coherent: MOTOR < SHOOTING < DETECTION
        // Rule: Motor stops when closest, "reached" is tighter, "close enough" is loosest

        @JvmField var MOTOR_TOLERANCE_TICKS = 40      // Was 75 - stop motor when very close
        @JvmField var SHOOTING_TOLERANCE_TICKS = 60   // Was 100 - "reached target" threshold
        @JvmField var DETECTION_TOLERANCE_TICKS = 100 // Was 150 - "close enough to detect"

        // For two-stage control (see improved controller)
        @JvmField var PID_TOLERANCE_TICKS = 150       // Was 100 - switch to gentle mode

        // Final gentle power when very close (outtake positions with magnets)
        @JvmField var FINAL_ADJUSTMENT_POWER = 0.08   // Was 0.085

        // ============== VELOCITY SETTLING ==============
        // Lower threshold for faster response
        const val VELOCITY_TOLERANCE_TPS = 300  // Was 750 (lower = settles faster)
        const val VELOCITY_SETTLE_TIME_MS = 200 // Was 300 (faster settling)

        // ============== HARDWARE CONSTANTS ==============
        const val TICKS_PER_REV = 8192  // Keep as is

        // ============== INTAKE CONFIRMATION ==============
        const val CONFIRM_INTAKE_MS = 67.0   // Keep as is
        const val CONFIRM_LOSS_MS = 10       // Keep as is
    }

    // ===================================================================
// RECOMMENDED TUNING PROCEDURE ON ROBOT:
// ===================================================================
//
// 1. Start with these values
// 2. Test one position transition (e.g., INTAKE_1 -> OUTTAKE_1)
// 3. Observe behavior:
//    - Overshoots/oscillates? -> Lower KP by 20%, increase KD by 20%
//    - Too slow to reach? -> Increase KP by 20%
//    - Settles but with offset? -> Increase KI slightly
//    - Jerky motion? -> Increase KD
// 4. Once smooth, test all 6 positions
// 5. Fine-tune MOTOR_TOLERANCE_TICKS:
//    - Too tight (never stops)? -> Increase by 10
//    - Too loose (stops far away)? -> Decrease by 10
//
// ===================================================================

    object Turret {
        const val TICKS_PER_REV = 384.5 * 3
        const val HEIGHT = 30.48
        const val THETA = 0.93
        const val ANGLE_TOLERANCE_RADIANS = 0.075
        const val LAUNCH_TIME = 0.125
        const val OFFSET = -10.0
    }

    object ServoPositions {
//        const val LAUNCHER_REST = 0.235
//        //Was 0.3
//        const val LAUNCHER_TRIGGERED = 0.75

        const val LAUNCHER_REST = 0.47
        //Was 0.3
        const val LAUNCHER_TRIGGERED = 0.235
    }

    object TransferData {
        fun reset() {
            allianceColor = AllianceColor.NEUTRAL
            pose = Pose()
            turretMotorTicks = 0
            spindexerMotorTicks = 0
        }

        var allianceColor = AllianceColor.NEUTRAL
        var pose = Pose()
        var turretMotorTicks = 0
        var spindexerMotorTicks = 0
    }

    @Config
    object Flywheel {
        @JvmField var KP = 0.0075
        @JvmField var KI = 0.0
        @JvmField var KD = 0.0
        @JvmField var KF = 0.000415
    }

    object Misc {
        val VERBOSE_LEVEL = Verbose.DEBUG
    }
}
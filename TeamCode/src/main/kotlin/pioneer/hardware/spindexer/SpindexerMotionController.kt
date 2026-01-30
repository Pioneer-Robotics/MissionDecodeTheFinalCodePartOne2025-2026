package pioneer.hardware.spindexer

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.util.ElapsedTime
import pioneer.Constants
import pioneer.helpers.Chrono
import pioneer.helpers.FileLogger
import pioneer.helpers.PIDController
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sign

/**
 * IMPROVED Spindexer Motion Controller
 *
 * Key improvements over original:
 * 1. Two-stage control: PID when far, gentle power when close
 * 2. Better velocity settling detection
 * 3. Improved static friction compensation
 * 4. Coherent tolerance hierarchy
 * 5. Diagnostic logging for debugging
 */
class SpindexerMotionController(
    private val motor: DcMotorEx,
) {

    // --- Motor Positions --- //
    enum class MotorPosition(val radians: Double) {
        OUTTAKE_1(0 * PI / 3),
        INTAKE_1(3 * PI / 3),

        OUTTAKE_2(2 * PI / 3),
        INTAKE_2(5 * PI / 3),

        OUTTAKE_3(4 * PI / 3),
        INTAKE_3(1 * PI / 3),
    }

    // --- Position Lists --- //
    private val intakePositions = listOf(
        MotorPosition.INTAKE_1,
        MotorPosition.INTAKE_2,
        MotorPosition.INTAKE_3
    )

    private val outtakePositions = listOf(
        MotorPosition.OUTTAKE_1,
        MotorPosition.OUTTAKE_2,
        MotorPosition.OUTTAKE_3
    )

    // --- Configuration --- //
    private val ticksPerRadian = (Constants.Spindexer.TICKS_PER_REV / (2 * PI)).toInt()
    private var calibrationTicks = 0

    private val pid = PIDController(
        Constants.Spindexer.KP,
        Constants.Spindexer.KI,
        Constants.Spindexer.KD,
    )

    private val chrono = Chrono(autoUpdate = false)

    // Timers for settling detection
    private val settleTimer = ElapsedTime()
    private val velocitySettleTimer = ElapsedTime()

    // State tracking
    private var lastError = 0
    private var settledCount = 0

    // --- Public State --- //
    var target: MotorPosition = MotorPosition.INTAKE_1
        set(value) {
            if (field != value) {
                FileLogger.debug("SpindexerMotion", "Target changed: $field -> $value")
                pid.reset()
                settleTimer.reset()
                velocitySettleTimer.reset()
                settledCount = 0
                field = value
            }
        }

    var manualOverride = false

    val currentTicks: Int
        get() = (-motor.currentPosition + calibrationTicks)

    val velocity: Double
        get() = motor.velocity

    val targetTicks: Int
        get() = (target.radians * ticksPerRadian).toInt()

    val errorTicks: Int
        get() = wrapTicks(targetTicks - currentTicks)

    /**
     * IMPROVED: More robust "reached target" detection
     * Requires BOTH position AND velocity to be settled
     */
    val reachedTarget: Boolean
        get() {
            val positionSettled = abs(errorTicks) < Constants.Spindexer.SHOOTING_TOLERANCE_TICKS
            val velocitySettled = abs(velocity) < Constants.Spindexer.VELOCITY_TOLERANCE_TPS
            val timeSettled = velocitySettleTimer.milliseconds() > Constants.Spindexer.VELOCITY_SETTLE_TIME_MS

            return positionSettled && velocitySettled && timeSettled
        }

    /**
     * Looser tolerance for artifact detection
     */
    val withinDetectionTolerance: Boolean
        get() = abs(errorTicks) < Constants.Spindexer.DETECTION_TOLERANCE_TICKS

    val closestPosition: MotorPosition
        get() {
            var closest = MotorPosition.INTAKE_1
            var smallest = Double.MAX_VALUE

            for (pos in MotorPosition.entries) {
                val ticks = (pos.radians * ticksPerRadian).toInt()
                val error = wrapTicks(ticks - currentTicks).toDouble()
                if (abs(error) < abs(smallest)) {
                    smallest = error
                    closest = pos
                }
            }
            return closest
        }

    // --- Initialization --- //
    fun init() {
        motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

        // TUNED: Integral clamp scaled to new KI
        // Max integral contribution: 0.0001 * 2000 = 0.2 power
        pid.integralClamp = 2000.0

        settleTimer.reset()
        velocitySettleTimer.reset()
    }

    fun calibrateEncoder(calibrationTicks: Int = 0) {
        motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
        this.calibrationTicks = calibrationTicks
        pid.reset()
        FileLogger.info("SpindexerMotion", "Encoder calibrated with offset: $calibrationTicks")
    }

    // --- Update Loop --- //
    fun update() {
        if (manualOverride) return

        chrono.update()

        // Track velocity settling
        if (abs(velocity) < Constants.Spindexer.VELOCITY_TOLERANCE_TPS) {
            // Velocity is low, let timer run
        } else {
            // Velocity is high, reset timer
            velocitySettleTimer.reset()
        }

        val error = errorTicks
        val absError = abs(error)

        // ============== TWO-STAGE CONTROL ==============
        var power = 0.0

        if (absError > Constants.Spindexer.MOTOR_TOLERANCE_TICKS) {
            // Stage 1: Use PID for larger errors
            power = pid.update(error.toDouble(), chrono.dt)

            // Add static friction compensation when far from target
            if (absError > 100) {
                power += Constants.Spindexer.KS_START * sign(error.toDouble())
            }

            // Clamp power to safe range
            power = power.coerceIn(-0.7, 0.7)

        } else {
            // Stage 2: Very close - stop and let magnets/friction settle
            power = 0.0
            pid.reset()  // Clear integral windup

            // Check if we're actually settled
            if (absError < Constants.Spindexer.MOTOR_TOLERANCE_TICKS / 2) {
                settledCount++
            } else {
                settledCount = 0
            }
        }

        // Apply power (negative because of motor direction)
        motor.power = -power

        // Diagnostic logging (comment out after tuning)
        if (absError > 50 && chrono.peek() > 100) {  // Log every 100ms when moving
            FileLogger.debug("SpindexerMotion",
                "Target: $target | Error: $error ticks | Power: ${String.format("%.3f", power)} | Vel: ${velocity.toInt()}")
            chrono.reset()
        }

        lastError = error
    }

    // --- Manual Control --- //
    fun moveManual(power: Double) {
        if (!manualOverride) {
            FileLogger.debug("SpindexerMotion", "Entering manual mode")
            manualOverride = true
        }
        motor.power = power.coerceIn(-1.0, 1.0)
    }

    fun stopManual() {
        if (manualOverride) {
            FileLogger.debug("SpindexerMotion", "Exiting manual mode")
            manualOverride = false
            pid.reset()
        }
    }

    // --- Helpers --- //
    /**
     * Wraps encoder error to shortest path around circle
     * CRITICAL: This must work correctly for rotating mechanism!
     */
    private fun wrapTicks(
        error: Int,
        ticksPerRev: Int = Constants.Spindexer.TICKS_PER_REV,
    ): Int {
        var e = error % ticksPerRev
        if (e > ticksPerRev / 2) e -= ticksPerRev
        if (e < -ticksPerRev / 2) e += ticksPerRev
        return e
    }
}
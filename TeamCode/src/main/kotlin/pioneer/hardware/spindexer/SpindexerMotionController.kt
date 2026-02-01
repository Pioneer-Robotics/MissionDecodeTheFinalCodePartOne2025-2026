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

    // --- Positions --- //
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
    private var lastPower = 0.0

    // ✅ UPDATED: PID with corrected gains
    private val pid = PIDController(
        Constants.Spindexer.KP,
        Constants.Spindexer.KI,
        Constants.Spindexer.KD,
    )

    private val chrono = Chrono(false)

    // ✅ NEW: Deceleration tracking for gentle motion
    private var isDecelerating = false

    // --- Public State --- //
    var target: MotorPosition = MotorPosition.INTAKE_1
        set(value) {
            if (field != value) {
                pid.reset()
                isDecelerating = false  // ✅ NEW: Reset decel flag
                field = value
                FileLogger.debug("SpindexerMotion", "Target changed to ${value.name}")
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

    val velocityTimer = ElapsedTime()

    // ✅ IMPROVED: Tighter tolerance + velocity check
    val reachedTarget: Boolean
        get() = abs(errorTicks) < Constants.Spindexer.SHOOTING_TOLERANCE_TICKS &&
                abs(velocity) < 100.0 &&  // ✅ NEW: Velocity check
                velocityTimer.milliseconds() > 250  // ✅ REDUCED: 250ms from 300ms

    // ✅ IMPROVED: Added velocity settling check
    val withinDetectionTolerance: Boolean
        get() = abs(errorTicks) < Constants.Spindexer.DETECTION_TOLERANCE_TICKS &&
                abs(velocity) < 100.0 &&  // ✅ NEW: Not moving fast
                velocityTimer.milliseconds() > 150  // ✅ NEW: Settled for 150ms

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
        pid.integralClamp = 1_000.0
    }

    fun calibrateEncoder(calibrationTicks: Int = 0) {
        motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

        // Logical offset from encoder zero to real zero
        this.calibrationTicks = calibrationTicks

        pid.reset()
    }

    // ========================================
    // ✅ IMPROVED: Motion profile with gentle deceleration
    // ========================================
    fun update() {
        if (manualOverride) return
        if (abs(velocity) >= Constants.Spindexer.VELOCITY_TOLERANCE_TPS)
            velocityTimer.reset()

        chrono.update()

        var power = pid.update(errorTicks.toDouble(), chrono.dt)

        val ks = Constants.Spindexer.KS_START

        // ✅ NEW: Three-zone control for smooth motion
        when {
            // Zone 1: Acceleration (far from target)
            abs(errorTicks) > Constants.Spindexer.DECEL_THRESHOLD_TICKS -> {
                power += ks * sign(errorTicks.toDouble())
                isDecelerating = false
            }

            // Zone 2: GENTLE DECELERATION (approaching target)
            // This is CRITICAL for ball retention in gravity-fed system!
            abs(errorTicks) > Constants.Spindexer.MOTOR_TOLERANCE_TICKS -> {
                // Use ONLY PID, no static friction
                // This creates natural, gentle deceleration
                isDecelerating = true

                // ✅ NEW: Reduce power during approach (critical for ball retention!)
                power *= 0.7  // 30% reduction prevents balls from flying out

                FileLogger.debug("SpindexerMotion",
                    "Gentle decel: error=${errorTicks}, power=%.3f".format(power))
            }

            // Zone 3: Stop (at target)
            else -> {
                power = 0.0
                pid.reset()
                if (abs(errorTicks) < 10) {  // Very close
                    FileLogger.debug("SpindexerMotion", "Reached target!")
                }
            }
        }

        // ✅ IMPROVED: Power limits based on zone
        val maxPower = if (isDecelerating) 0.4 else 0.6  // Gentler during decel
        motor.power = -power.coerceIn(-maxPower, maxPower)
    }

    // --- Manual Control --- //
    fun moveManual(power: Double) {
        manualOverride = true
        motor.power = power.coerceIn(-1.0, 1.0)
    }

    fun stopManual() {
        manualOverride = false
        pid.reset()
    }

    // --- Helpers --- //
    private fun rampPower(
        desired: Double,
        dt: Double,
    ): Double {
        val maxDelta = Constants.Spindexer.MAX_POWER_RATE * dt / 1000.0

        val delta = desired - lastPower
        val clipped = delta.coerceIn(-maxDelta, maxDelta)

        lastPower += clipped
        return lastPower
    }

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
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

    // --- Motor Positions (UNCHANGED) --- //
    enum class MotorPosition(val radians: Double) {
        OUTTAKE_1(0 * PI / 3),
        INTAKE_1(3 * PI / 3),

        OUTTAKE_2(2 * PI / 3),
        INTAKE_2(5 * PI / 3),

        OUTTAKE_3(4 * PI / 3),
        INTAKE_3(1 * PI / 3),
    }

    // --- NEW: Direction Enum --- //
    enum class SpindexerDirection {
        INTAKE,    // Balls stay in spindexer
        SHOOTING   // Passive ramp engages, balls shoot
    }

    // --- Positions --- //
    private val intakePositions =
        listOf(
            MotorPosition.INTAKE_1,
            MotorPosition.INTAKE_2,
            MotorPosition.INTAKE_3
        )

    private val outtakePositions =
        listOf(
            MotorPosition.OUTTAKE_1,
            MotorPosition.OUTTAKE_2,
            MotorPosition.OUTTAKE_3
        )

    // --- Configuration (UNCHANGED) --- //

    private val ticksPerRadian =
        (Constants.Spindexer.TICKS_PER_REV / (2 * PI)).toInt()
    private var calibrationTicks = 0
    private var lastPower = 0.0

    private val pid = PIDController(
        Constants.Spindexer.KP,
        Constants.Spindexer.KI,
        Constants.Spindexer.KD,
    )

    private val chrono = Chrono(false)

    // --- NEW: Continuous Rotation State --- //
    private var continuousRotationActive = false
    private var continuousRotationPower = 0.0

    // --- Public State (UNCHANGED) --- //

    var target: MotorPosition = MotorPosition.INTAKE_1
        set(value) {
            if (field != value) {
                pid.reset()
                field = value
                // NEW: Stop continuous rotation when setting target
                stopContinuousRotation()
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

    val reachedTarget: Boolean
        get() =
            abs(errorTicks) < Constants.Spindexer.SHOOTING_TOLERANCE_TICKS &&
                    velocityTimer.milliseconds() > 150 // CHANGED: was 300

    val withinDetectionTolerance: Boolean
        get() =
            abs(errorTicks) < Constants.Spindexer.DETECTION_TOLERANCE_TICKS

    val withinVelocityTolerance: Boolean
        get() =
            abs(motor.velocity) < Constants.Spindexer.VELOCITY_TOLERANCE_TPS

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

    // --- Initialization (UNCHANGED) --- //

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

    // --- Update Loop --- //

    fun update() {
        // Manual override takes priority
        if (manualOverride) return

        // Continuous rotation for rapid fire
        if (continuousRotationActive) {
            motor.power = continuousRotationPower
            return
        }

        // Standard position control (UNCHANGED from original)
        updatePositionControl()
    }

    private fun updatePositionControl() {
        if (!withinVelocityTolerance)
            velocityTimer.reset()

        chrono.update()

        var power = pid.update(errorTicks.toDouble(), chrono.dt)

        val ks = Constants.Spindexer.KS_START

        if (abs(power) > 0.01) {
            power += ks * sign(power)
        }

        if (abs(errorTicks) < Constants.Spindexer.MOTOR_TOLERANCE_TICKS) {
            power = 0.0
        }

        motor.power = power.coerceIn(-0.6, 0.6)
    }

    // ========== NEW: Continuous Rotation for Rapid Fire ========== //

    /**
     * Start continuous rotation in specified direction
     * Used for multi-shot rapid fire - just spin continuously
     *
     * @param direction INTAKE or SHOOTING
     * @param power Motor power (0.0 to 1.0)
     */
    fun startContinuousRotation(
        direction: SpindexerDirection,
        power: Double
    ) {
        require(power in 0.0..1.0) { "Power must be between 0.0 and 1.0" }

        continuousRotationActive = true

        // Determine motor direction based on Constants
        val motorPower = when (direction) {
            SpindexerDirection.INTAKE -> {
                if (Constants.Spindexer.INTAKE_IS_POSITIVE) power else -power
            }
            SpindexerDirection.SHOOTING -> {
                if (Constants.Spindexer.INTAKE_IS_POSITIVE) -power else power
            }
        }

        continuousRotationPower = motorPower
        motor.power = motorPower

        FileLogger.info("SpindexerMotion",
            "Continuous rotation started: $direction @ ${(power * 100).toInt()}%")
    }

    /**
     * Stop continuous rotation
     */
    fun stopContinuousRotation() {
        if (!continuousRotationActive) return

        continuousRotationActive = false
        continuousRotationPower = 0.0
        motor.power = 0.0

        FileLogger.info("SpindexerMotion", "Continuous rotation stopped")
    }

    // --- Manual Control (UNCHANGED) --- //

    fun moveManual(power: Double) {
        manualOverride = true
        continuousRotationActive = false  // NEW: Stop continuous rotation
        motor.power = power.coerceIn(-1.0, 1.0)
    }

    fun stopManual() {
        manualOverride = false
        pid.reset()
    }

    // --- Helpers (UNCHANGED) --- //
    private fun rampPower(
        desired: Double,
        dt: Double,
    ): Double {
        val maxDelta =
            Constants.Spindexer.MAX_POWER_RATE * dt / 1000.0

        val delta = desired - lastPower
        val clipped = delta.coerceIn(-maxDelta, maxDelta)

        lastPower += clipped
        return lastPower
    }

    private fun wrapTicks(
        error: Int,
        ticksPerRev: Double = Constants.Spindexer.TICKS_PER_REV,
    ): Int {
        var e = error % ticksPerRev
        if (e > ticksPerRev / 2) e -= ticksPerRev
        if (e < -ticksPerRev / 2) e += ticksPerRev
        return e.toInt()
    }
}
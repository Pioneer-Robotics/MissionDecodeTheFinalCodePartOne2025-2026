package pioneer.hardware.spindexer

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.util.ElapsedTime
import pioneer.Constants
import pioneer.helpers.Chrono
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
        INTAKE_3(1 * PI / 3);

        /**
         * Helper method to get next entry + angle offset
         * @param numShifts Shifts motorPosition by PI/3 * numShifts
         * @return Shifted motorPosition
         */
        fun offset(numShifts: Int): MotorPosition {
            val stepAngle = PI / 3
            val fullCircle = 2 * PI

            val targetAngle = (radians + numShifts * stepAngle).mod(fullCircle)
            return entries.minBy { abs(it.radians - targetAngle) }
        }
    }

    // --- Positions --- //
    val intakePositions =
        listOf(
            MotorPosition.INTAKE_1,
            MotorPosition.INTAKE_2,
            MotorPosition.INTAKE_3
        )

    val outtakePositions =
        listOf(
            MotorPosition.OUTTAKE_1,
            MotorPosition.OUTTAKE_2,
            MotorPosition.OUTTAKE_3
        )

    // --- Configuration --- //
    private val ticksPerRadian = Constants.Spindexer.TICKS_PER_REV / (2 * PI)
    private var calibrationTicks = 0

    private val pid = PIDController(
        Constants.Spindexer.KP,
        Constants.Spindexer.KI,
        Constants.Spindexer.KD,
    )

    private val chrono = Chrono(false)

    // --- Public State --- //
    var target: MotorPosition = MotorPosition.INTAKE_1
        set(value) {
            if (field != value) {
                pid.reset()
                field = value
            }
        }

    var manualOverride = false

    private var shooting = false
    private var shootStartTicks = 0
    private var shootDeltaTicks = 0
    private var shootPower = 0.0

    val currentTicks: Int
        get() = (-motor.currentPosition + calibrationTicks)

    val velocity: Double
        get() = motor.velocity

    val targetTicks: Int
        get() = (target.radians * ticksPerRadian).toInt()

    val errorTicks: Int
        get() = wrapTicks(targetTicks - currentTicks)

    val velocityTimer = ElapsedTime()

    val isShooting: Boolean
        get() = shooting

    val reachedTarget: Boolean
        get() =
            abs(errorTicks) < Constants.Spindexer.SHOOTING_TOLERANCE_TICKS &&
                    velocityTimer.milliseconds() > 300

    val withinDetectionTolerance: Boolean
        get() =
            abs(errorTicks) < Constants.Spindexer.DETECTION_TOLERANCE_TICKS

    val withinVelocityTolerance: Boolean
        get() =
            abs(motor.velocity) < Constants.Spindexer.VELOCITY_TOLERANCE_TPS

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

    // --- Update Loop --- //

    fun update() {
        if (shooting) {
            val traveledTicks = abs(currentTicks - shootStartTicks)
            if (traveledTicks >= shootDeltaTicks) {
                stopShooting()
            } else {
                motor.power = shootPower
            }
            return
        }
        if (manualOverride) return
        if (!withinVelocityTolerance)
            velocityTimer.reset()

        chrono.update()

        // Correct error to only move in one direction (unless really small)
        val correctedError: Int = run {
            val full = Constants.Spindexer.TICKS_PER_REV.toInt()
            val e = errorTicks % full
            if (e < -Constants.Spindexer.ALLOWED_REVERSE_TICKS) e + full else e
        }

        var power = pid.update(correctedError.toDouble(), chrono.dt)
        val ks = Constants.Spindexer.KS_START

        if (abs(power) > 0.01) {
            power += ks * sign(power)
        }

        if (abs(errorTicks) < Constants.Spindexer.MOTOR_TOLERANCE_TICKS) {
            power = 0.0
        }

        motor.power = power.coerceIn(-0.6, 0.6)
    }

    // --- Manual Control --- //
    fun moveManual(power: Double) {
        if (shooting) stopShooting()
        manualOverride = true
        motor.power = power.coerceIn(-1.0, 1.0)
    }

    fun stopManual() {
        manualOverride = false
        pid.reset()
    }

    fun startShooting(deltaTicks: Int, power: Double) {
        val clampedTicks = abs(deltaTicks)
        if (clampedTicks == 0) return
        manualOverride = false
        shooting = true
        shootStartTicks = currentTicks
        shootDeltaTicks = clampedTicks
        shootPower = power.coerceIn(-1.0, 1.0)
        pid.reset()
    }

    fun stopShooting() {
        shooting = false
        motor.power = 0.0
        pid.reset()
    }

    // --- Helpers --- //
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

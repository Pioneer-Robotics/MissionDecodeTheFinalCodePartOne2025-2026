package pioneer.hardware.spindexer

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import pioneer.Constants
import pioneer.helpers.Chrono
import pioneer.helpers.PIDController
import kotlin.math.abs
import kotlin.math.sign

class SpindexerMotionController(
    private val motor: DcMotorEx,
) {
    // --- Motion logic --- //
    var positionIndex = 0
        private set
    var direction = 1
        private set
    var manualOverride = false

    // --- Logic for calibrating encoder --- //
    private var calibrationTicks = 0
    val currentTicks: Int
        get() = (-motor.currentPosition + calibrationTicks)
    val targetTicks: Int
        get() {
            // Target is defined so that positionIndex 0 is in the intake position,
            // and indices increase as spindexer moves in the direction of outtake.
            val outtakeDir = if (Constants.Spindexer.OUTTAKE_IS_POSITIVE) 1 else -1
            return Math.floorMod(
                (positionIndex * Constants.Spindexer.TICKS_PER_REV / 3 * outtakeDir).toInt(),
                Constants.Spindexer.TICKS_PER_REV.toInt()
            )
        }

    val errorTicks: Int
        get() = wrapTicks(targetTicks - currentTicks, direction)

    // --- PID Controller --- //
    private val pid = PIDController(
        Constants.Spindexer.KP,
        Constants.Spindexer.KI,
        Constants.Spindexer.KD,
    )
    val chrono = Chrono()

    init {
        motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        motor.mode = DcMotor.RunMode.RUN_USING_ENCODER
        pid.integralClamp = 1_000.0
    }

    // --- Other Public State --- //
    val reachedTarget: Boolean
        get() =
            abs(errorTicks) < Constants.Spindexer.SHOOTING_TOLERANCE_TICKS

    fun update() {
        if (manualOverride) return

        // Run PID
        var power = pid.update(errorTicks.toDouble(), chrono.dt)

        // Apply feedforward to help get past static friction
        val ks = Constants.Spindexer.KS_START
        if (abs(power) > 0.01) {
            power += ks * sign(power)
        }

        // Don't try to correct if we're close enough to the target
        if (abs(errorTicks) < Constants.Spindexer.MOTOR_TOLERANCE_TICKS) {
            power = 0.0
        }

        motor.power = power.coerceIn(-0.5, 0.5)
    }

    fun calibrateEncoder(ticks: Int) {
        calibrationTicks = ticks
        motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        motor.mode = DcMotor.RunMode.RUN_USING_ENCODER
    }

    fun moveManual(power: Double) {
        manualOverride = true
        motor.power = power
    }

    fun setTarget(positionIndex: Int, direction: Int = 0) {
        manualOverride = false
        this.positionIndex = Math.floorMod(positionIndex, 3)
        this.direction = direction
    }

    // --- Helper functions --- //
    private fun wrapTicks(
        error: Int,
        direction: Int,
        ticksPerRev: Double = Constants.Spindexer.TICKS_PER_REV,
    ): Int {

        val ticks = ticksPerRev.toInt()

        // Get error in both directions
        val forward = Math.floorMod(error, ticks)
        val reverse = if (forward == 0) 0 else forward - ticks
        val allowedReverse = Constants.Spindexer.ALLOWED_REVERSE_TICKS

        return when (direction) {
            1 -> { // Prefer forward
                if (abs(reverse) <= allowedReverse) reverse
                else forward
            }
            -1 -> { // Prefer reverse
                if (abs(forward) <= allowedReverse) forward
                else reverse
            }
            // Direction 0 means no preference, so just take the smaller error
            else -> if (abs(forward) <= abs(reverse)) forward else reverse
        }
    }
}
package pioneer.hardware.spindexer

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.util.ElapsedTime
import pioneer.Constants
import pioneer.helpers.Chrono
import pioneer.helpers.MathUtils
import pioneer.helpers.PIDController
import pioneer.helpers.Toggle
import kotlin.math.abs
import kotlin.math.sign

class SpindexerMotionController(
    private val motor: DcMotorEx,
) {
    // --- Configuration --- //
    private var calibrationTicks = 0

    private val pid = PIDController(
        Constants.Spindexer.KP,
        Constants.Spindexer.KI,
        Constants.Spindexer.KD,
    )

    private val chrono = Chrono(false)

    // --- Public State --- //
    var positionIndex = 0
        set(value) {
            if (field != value) {
                pid.reset()
                field = value
            }
        }

    var manualOverride = false

    private var shooting = false
    private var prevShooting = false
//    private var shootingToggle = Toggle(false)
    private var shootStartTicks = 0
    private var shootDeltaTicks = 0
    private var shootPower = 0.0

    val currentTicks: Int
        get() = (-motor.currentPosition + calibrationTicks)

    val velocity: Double
        get() = motor.velocity

    val targetTicks: Int
        get() = (positionIndex * Constants.Spindexer.TICKS_PER_REV / 3).toInt()

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

    val justStoppedShooting: Boolean
        get() =
            checkJustStoppedShooting()

    // --- Initialization --- //

    fun init() {
        motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        motor.mode = DcMotor.RunMode.RUN_USING_ENCODER
        pid.integralClamp = 1_000.0
    }

    fun calibrateEncoder(calibrationTicks: Int = 0) {
        motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        motor.mode = DcMotor.RunMode.RUN_USING_ENCODER

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
                motor.velocity = -shootPower * Constants.Spindexer.MAX_VELOCITY
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
            val allowedReverse = Constants.Spindexer.ALLOWED_REVERSE_TICKS
            // Wrap to enforce movement direction
//            val range = Pair(-allowedReverse, full - allowedReverse)
            // For reversed spindexer
             val range = Pair(-(full + allowedReverse), allowedReverse)
            MathUtils.wrap(errorTicks, range)
        }

//        FileLogger.debug("Spindexer Motor Control", "Corrected Error: $correctedError")

        var power = -pid.update(correctedError.toDouble(), chrono.dt)
        val ks = Constants.Spindexer.KS_START

        if (abs(power) > 0.01) {
            power += ks * sign(power)
        }

        if (abs(errorTicks) < Constants.Spindexer.MOTOR_TOLERANCE_TICKS) {
            power = 0.0
        }

        motor.power = power.coerceIn(-0.75, 0.75)
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
//        shootingToggle.state = true
        shootStartTicks = currentTicks
        shootDeltaTicks = clampedTicks
        shootPower = power.coerceIn(-1.0, 1.0)
        pid.reset()

    }

    fun stopShooting() {
        shooting = false
//        shootingToggle.state = false
        motor.power = 0.0
        pid.reset()
    }

    fun checkJustStoppedShooting(): Boolean{
//        if (shootingToggle.justChanged && !shootingToggle.state) {
//            return true
//        } else {
//            return false
//        }
        if (prevShooting && !shooting) {
            prevShooting = shooting
            return true
        } else {
            prevShooting = shooting
            return false
        }

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

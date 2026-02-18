package pioneer.hardware

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import pioneer.Constants
import pioneer.helpers.FileLogger
import kotlin.time.Duration.Companion.seconds

class Intake(
    private val hardwareMap: HardwareMap,
    private val motorName: String = Constants.HardwareNames.INTAKE_MOTOR,
) : HardwareComponent {
    private lateinit var intake: DcMotorEx

    var power
        get() = intake.power
        set(value) {
            intake.power = value
        }

    val current get() = intake.getCurrent(CurrentUnit.MILLIAMPS)
    val currentTimer = ElapsedTime()

    var defaultPower: Double = 0.9
    private val pauseTimer = ElapsedTime()
    private val reverseTime = 0.35
    private val pauseTime = 0.0
    private var continuePower = -defaultPower
    private var paused = false

    override fun init() {
        intake =
            hardwareMap.get(DcMotorEx::class.java, motorName).apply {
                mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
                zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
                direction = DcMotorSimple.Direction.FORWARD
            }
    }

    override fun update() {
        if (current < 6000) currentTimer.reset()
        if (currentTimer.seconds() > 0.5) pause()

        if (paused) {
            if (pauseTimer.seconds() < reverseTime) {
                power = 0.5
            } else if (pauseTimer.seconds() < reverseTime + pauseTime) {
                power = 0.0
            } else {
                power = continuePower
                paused = false
            }
        }
    }

    /**
     * Reverse the intake briefly, then wait some time for the spindexer
     */
    fun pause() {
        if (!paused) {
            continuePower = power
            paused = true
            pauseTimer.reset()
        }
    }

    fun forward() {
        if (paused) return
        power = -defaultPower
    }

    fun reverse() {
        if (paused) return
        power = defaultPower
    }

    fun stop() {
        if (paused) return
        power = 0.0
    }
}

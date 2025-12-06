package pioneer.hardware

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import pioneer.Constants

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

    var defaultPower: Double = 1.0

    override fun init() {
        intake =
            hardwareMap.get(DcMotorEx::class.java, motorName).apply {
                mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
                zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
                direction = DcMotorSimple.Direction.FORWARD
            }
    }

    fun forward() {
        power = -defaultPower
    }

    fun reverse() {
        power = defaultPower
    }

    fun stop() {
        power = 0.0
    }
}

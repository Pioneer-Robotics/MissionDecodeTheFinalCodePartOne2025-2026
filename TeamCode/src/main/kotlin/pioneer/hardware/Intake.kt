package pioneer.hardware

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import pioneer.Constants

class Intake(private val hardwareMap: HardwareMap,
             private val motorName: String = Constants.HardwareNames.INTAKE_MOTOR
) : HardwareComponent {

    private lateinit var intake: DcMotorEx

    var power
        get() = intake.power
        set(value) {
            intake.power = value
        }

    override fun init() {
        intake = hardwareMap.get(DcMotorEx::class.java, motorName).apply {
            mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
            zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
            direction = DcMotorSimple.Direction.FORWARD
        }
    }

    fun forward() {
        power = 1.0
    }
    fun reverse() {
        power = -1.0
    }
    fun stop() {
        power = 0.0
    }
}
package pioneer.hardware

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import pioneer.constants.HardwareNames

class Flywheel(
    private val hardwareMap: HardwareMap,
    private val motorName: String = HardwareNames.FLYWHEEL,
) : HardwareComponent {
    override val name = "Flywheel"

    private lateinit var flywheel: DcMotorEx

    var velocity
        get() = flywheel.velocity
        set(value) {
            flywheel.velocity = value
        }

    override fun init() {
        flywheel = hardwareMap.get(DcMotorEx::class.java, motorName)
        flywheel.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        flywheel.mode = DcMotor.RunMode.RUN_USING_ENCODER
        flywheel.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.FLOAT
        flywheel.direction = DcMotorSimple.Direction.FORWARD
    }
}

package pioneer.hardware

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import pioneer.constants.HardwareNames

class Flywheel(
    hardwareMap: HardwareMap,
    name: String = HardwareNames.FLYWHEEL,
) {
    private val flywheel: DcMotorEx = hardwareMap.get(DcMotorEx::class.java, name)

    val velocity
        get() = flywheel.getVelocity()

    init {
        flywheel.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        flywheel.mode = DcMotor.RunMode.RUN_USING_ENCODER
        flywheel.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.FLOAT
        flywheel.direction = DcMotorSimple.Direction.FORWARD
    }

    fun setSpeed(velocity: Double) {
        flywheel.setVelocity(velocity)
    }
}

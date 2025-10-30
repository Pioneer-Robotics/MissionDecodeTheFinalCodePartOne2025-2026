package pioneer.hardware.impl

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import pioneer.constants.HardwareNames
import pioneer.hardware.interfaces.Flywheel
import kotlin.jvm.java

class FlywheelImpl (hardwareMap: HardwareMap, name: String = "flywheel") : Flywheel {
    val flywheel = hardwareMap.get(DcMotorEx::class.java, name)

    init {
        flywheel.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        flywheel.mode = DcMotor.RunMode.RUN_USING_ENCODER
        flywheel.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.FLOAT
        flywheel.direction = DcMotorSimple.Direction.FORWARD
    }

    override fun setSpeed(velocity: Double) {
        flywheel.velocity = velocity
    }
}

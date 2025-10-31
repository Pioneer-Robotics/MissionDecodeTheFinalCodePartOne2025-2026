package pioneer.hardware.impl

import pioneer.hardware.base.FlywheelBase
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap

class FlywheelImpl(hardwareMap: HardwareMap, name: String = "flywheel") : FlywheelBase() {
    private val flywheel: DcMotorEx = hardwareMap.get(DcMotorEx::class.java, name)

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

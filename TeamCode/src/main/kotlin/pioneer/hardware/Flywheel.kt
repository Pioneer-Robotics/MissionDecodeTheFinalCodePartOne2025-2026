package pioneer.hardware

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.PIDCoefficients
import com.qualcomm.robotcore.hardware.PIDFCoefficients
import pioneer.Constants
import pioneer.helpers.FileLogger

class Flywheel(
    private val hardwareMap: HardwareMap,
    private val motorName: String = Constants.HardwareNames.FLYWHEEL,
) : HardwareComponent {

    private lateinit var flywheel: DcMotorEx

    var velocity
        get() = flywheel.velocity
        set(value) {
            flywheel.velocity = value
        }

    override fun init() {
        flywheel = hardwareMap.get(DcMotorEx::class.java, motorName).apply {
            mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
            mode = DcMotor.RunMode.RUN_USING_ENCODER
            zeroPowerBehavior = DcMotor.ZeroPowerBehavior.FLOAT
            direction = DcMotorSimple.Direction.FORWARD
        }
        FileLogger.info(name, flywheel.getPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER).toString())
        flywheel.setPIDFCoefficients(
            DcMotor.RunMode.RUN_USING_ENCODER,
            PIDFCoefficients(
                50.0,
                3.0,
                0.0,
                0.0,
            )
        )
    }
}

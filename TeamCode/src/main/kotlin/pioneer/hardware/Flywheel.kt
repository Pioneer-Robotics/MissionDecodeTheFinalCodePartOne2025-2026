package pioneer.hardware

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import pioneer.constants.HardwareNames
import pioneer.constants.ServoPositions

class Flywheel(
    private val hardwareMap: HardwareMap,
    private val motorName: String = HardwareNames.FLYWHEEL,
    private val launchServoName: String = HardwareNames.LAUNCH_SERVO,
) : HardwareComponent {
    override val name = "Flywheel"

    private lateinit var flywheel: DcMotorEx
    private lateinit var launchServo: Servo

    var velocity
        get() = flywheel.power
        set(value) {
            flywheel.power = value
        }

    override fun init() {
        flywheel = hardwareMap.get(DcMotorEx::class.java, motorName)
        flywheel.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        flywheel.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
        flywheel.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.FLOAT
        flywheel.direction = DcMotorSimple.Direction.REVERSE
        launchServo = hardwareMap.get(Servo::class.java, launchServoName)
    }

    fun triggerLaunch() {
        launchServo.position = ServoPositions.LAUNCHER_TRIGGERED
        // Return to rest position after a short delay
        Thread {
            Thread.sleep(500) // 500 ms delay
            launchServo.position = ServoPositions.LAUNCHER_REST
        }
    }
}

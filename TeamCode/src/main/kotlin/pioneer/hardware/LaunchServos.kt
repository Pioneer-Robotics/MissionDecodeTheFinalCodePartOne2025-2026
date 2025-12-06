package pioneer.hardware

import com.qualcomm.robotcore.hardware.CRServo
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.ElapsedTime
import pioneer.Constants

class LaunchServos(
    private val hardwareMap: HardwareMap,
    private val leftName: String = Constants.HardwareNames.LAUNCH_SERVO_L,
    private val rightName: String = Constants.HardwareNames.LAUNCH_SERVO_R,
) : HardwareComponent {
    private lateinit var servo1: CRServo
    private lateinit var servo2: CRServo

    var launch = false
        private set
    var retract = false
        private set
    var timer = ElapsedTime()

    override fun init() {
        servo1 = hardwareMap.get(CRServo::class.java, leftName)
        servo2 = hardwareMap.get(CRServo::class.java, rightName)
    }

    fun triggerLaunch() {
        launch = true
        retract = false
        timer.reset()
    }

    fun triggerRetract() {
        launch = false
        retract = true
        timer.reset()
    }

    override fun update() {
        if (timer.milliseconds() < 250) {
            if (launch) {
                setPower(1.0)
            } else if (retract) {
                setPower(-1.0)
            } else {
                setPower(0.0)
            }
        } else {
            launch = false
            retract = false
            setPower(0.0)
        }
    }

    private fun setPower(power: Double) {
        servo1.power = -power
        servo2.power = power
    }
}

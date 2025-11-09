package pioneer.hardware

import com.qualcomm.robotcore.hardware.CRServo
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.ElapsedTime
import pioneer.constants.HardwareNames

class LaunchServos(
    hardwareMap: HardwareMap,
    leftName: String = HardwareNames.LAUNCH_SERVO_L,
    rightName: String = HardwareNames.LAUNCH_SERVO_R,
) {
    val servo1 = hardwareMap.get(CRServo::class.java, leftName)
    val servo2 = hardwareMap.get(CRServo::class.java, rightName)

    var launch = false
    var retract = false
    var timer = ElapsedTime()

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

    fun update() {
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

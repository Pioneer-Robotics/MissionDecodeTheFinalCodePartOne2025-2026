package pioneer.hardware.impl

import com.qualcomm.robotcore.hardware.CRServo
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.ElapsedTime
import pioneer.constants.HardwareNames
import pioneer.hardware.interfaces.LaunchServos
import kotlin.jvm.java

class LaunchServosImpl (hardwareMap: HardwareMap, leftName: String = "launchServoL", rightName: String = "launchServoR") : LaunchServos {
    val servo1 = hardwareMap.get(CRServo::class.java, leftName)
    val servo2 = hardwareMap.get(CRServo::class.java, rightName)

    var launch = false
    var retract = false
    var timer = ElapsedTime()

    override fun triggerLaunch() {
        launch = true
        retract = false
        timer.reset()
    }

    override fun triggerRetract() {
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
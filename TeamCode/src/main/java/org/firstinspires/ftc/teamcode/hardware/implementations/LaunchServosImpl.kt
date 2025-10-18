package org.firstinspires.ftc.teamcode.hardware.implementations

import com.qualcomm.robotcore.hardware.CRServo
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.teamcode.HardwareNames
import org.firstinspires.ftc.teamcode.hardware.interfaces.LaunchServos

class LaunchServosImpl (hardwareMap: HardwareMap) : LaunchServos {
    val servo1 = hardwareMap.get(CRServo::class.java, HardwareNames.LAUNCH_SERVO_L)
    val servo2 = hardwareMap.get(CRServo::class.java, HardwareNames.LAUNCH_SERVO_R)
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
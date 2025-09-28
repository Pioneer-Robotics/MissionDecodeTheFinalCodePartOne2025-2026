package org.firstinspires.ftc.teamcode.hardware

import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import org.firstinspires.ftc.teamcode.HardwareNames

class LaunchServos (hardwareMap: HardwareMap ) {
    val servo1 = hardwareMap.get(Servo::class.java, HardwareNames.LAUNCH_SERVO_L)

    fun open()  { servo1.position = 1.0 }
    fun close() { servo1.position = 0.0 }
}
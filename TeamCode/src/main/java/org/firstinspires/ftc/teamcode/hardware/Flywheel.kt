package org.firstinspires.ftc.teamcode.hardware

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.teamcode.HardwareNames

class Flywheel (hardwareMap: HardwareMap) {
    val flywheel = hardwareMap.get(DcMotorEx::class.java, HardwareNames.FLYWHEEL)

    init {
        flywheel.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        flywheel.mode = DcMotor.RunMode.RUN_USING_ENCODER
        flywheel.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.FLOAT
        flywheel.direction = DcMotorSimple.Direction.FORWARD
    }

    fun setSpeed(velocity: Double) {
        flywheel.power = velocity
    }
}
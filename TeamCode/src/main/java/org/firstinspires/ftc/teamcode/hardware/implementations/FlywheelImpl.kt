package org.firstinspires.ftc.teamcode.hardware.implementations

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.teamcode.HardwareNames
import org.firstinspires.ftc.teamcode.hardware.Flywheel

class FlywheelImpl (hardwareMap: HardwareMap) : Flywheel {
    val flywheel = hardwareMap.get(DcMotorEx::class.java, HardwareNames.FLYWHEEL)

    init {
        flywheel.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        flywheel.mode = DcMotor.RunMode.RUN_USING_ENCODER
        flywheel.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.FLOAT
        flywheel.direction = DcMotorSimple.Direction.FORWARD
    }

    override fun setSpeed(velocity: Double) {
        flywheel.power = velocity
    }
}
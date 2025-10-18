package org.firstinspires.ftc.teamcode.hardware.mocks

import com.qualcomm.robotcore.hardware.DcMotor
import org.firstinspires.ftc.teamcode.hardware.interfaces.MecanumBase
import org.firstinspires.ftc.teamcode.helpers.FileLogger
import org.firstinspires.ftc.teamcode.localization.Pose

class MecanumBaseMock : MecanumBase {
    override fun setZeroPowerBehavior(behavior: DcMotor.ZeroPowerBehavior) {
        FileLogger.warn("MecanumBase", "MecanumBase not initialized")
    }

    override fun setDrivePower(x: Double, y: Double, rotation: Double, power: Double, adjustForStrafe: Boolean) {
        FileLogger.warn("MecanumBase", "MecanumBase not initialized")
    }

    override fun setDriveVA(vel: Pose, accel: Pose) {
        FileLogger.warn("MecanumBase", "MecanumBase not initialized")
    }

    override fun stop() {
        FileLogger.warn("MecanumBase", "MecanumBase not initialized")
    }
}
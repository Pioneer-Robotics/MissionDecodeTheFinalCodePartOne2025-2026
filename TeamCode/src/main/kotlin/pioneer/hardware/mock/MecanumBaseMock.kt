package pioneer.hardware.mock

import com.qualcomm.robotcore.hardware.DcMotor
import pioneer.hardware.interfaces.MecanumBase
import pioneer.helpers.FileLogger
import pioneer.helpers.Pose

class MecanumBaseMock : MecanumBase {
    override fun setZeroPowerBehavior(behavior: DcMotor.ZeroPowerBehavior) {
        FileLogger.warn("NOT INITIALIZED", "setZeroPowerBehavior called on MecanumBaseMock")
    }

    override fun setDrivePower(
        x: Double,
        y: Double,
        rotation: Double,
        power: Double
    ) {
        FileLogger.warn("NOT INITIALIZED", "setDrivePower called on MecanumBaseMock")
    }

    override fun setDriveVA(pose: Pose) {
        FileLogger.warn("NOT INITIALIZED", "setDriveVA called on MecanumBaseMock")
    }

    override fun stop() {
        FileLogger.warn("NOT INITIALIZED", "stop called on MecanumBaseMock")
    }
}

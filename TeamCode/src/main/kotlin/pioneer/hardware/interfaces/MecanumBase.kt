package pioneer.hardware.interfaces

import com.qualcomm.robotcore.hardware.DcMotor
import pioneer.Constants
import pioneer.helpers.Pose

interface MecanumBase {
    fun setZeroPowerBehavior(behavior: DcMotor.ZeroPowerBehavior)

    fun setDrivePower(
        x: Double,
        y: Double,
        rotation: Double,
        power: Double = Constants.Drive.DEFAULT_DRIVE_POWER,
    )

    fun setDriveVA(
        velocity: Pose,
        acceleration: Pose,
    )

    fun stop()
}
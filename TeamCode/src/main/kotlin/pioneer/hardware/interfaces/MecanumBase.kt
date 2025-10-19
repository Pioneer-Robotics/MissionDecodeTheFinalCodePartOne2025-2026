package pioneer.hardware.interfaces

import com.qualcomm.robotcore.hardware.DcMotor
import pioneer.Constants
import pioneer.helpers.Pose

interface MecanumBase {
    fun setZeroPowerBehavior(behavior: DcMotor.ZeroPowerBehavior)

    fun setDrivePower(
        pose: Pose,
        power: Double = Constants.Drive.DEFAULT_POWER,
        max_motor_vel_tps: Double = Constants.Drive.MAX_MOTOR_VELOCITY_TPS,
    )

    fun setDriveVA(pose: Pose)

    fun stop()
}
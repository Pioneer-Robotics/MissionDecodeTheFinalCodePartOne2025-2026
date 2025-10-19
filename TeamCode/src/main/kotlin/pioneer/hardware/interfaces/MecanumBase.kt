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
        power: Double = Constants.Drive.DEFAULT_POWER,
        max_motor_vel_tps: Double = Constants.Drive.MOTOR_MAX_VELOCITY_TPS,
    )

    fun setDriveVA(
        pose: Pose,
        max_motor_vel_tps: Double = Constants.Drive.MOTOR_MAX_VELOCITY_TPS,
    )

    fun stop()
}
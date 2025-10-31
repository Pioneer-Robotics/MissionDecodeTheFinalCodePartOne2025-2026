package pioneer.hardware.interfaces

import com.qualcomm.robotcore.hardware.DcMotor
import pioneer.helpers.Pose

interface MecanumBase {
    fun setZeroPowerBehavior(behavior: DcMotor.ZeroPowerBehavior)

    fun setDrivePower(
        pose: Pose,
        power: Double,
        maxMotorVelocityTps: Double
    )

    fun setDriveVelocity(
        pose: Pose,
        power: Double,
        maxMotorVelocityTps: Double
    )

    fun setDriveVA(pose: Pose)

    fun stop()
}
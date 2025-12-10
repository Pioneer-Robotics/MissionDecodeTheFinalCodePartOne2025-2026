package pioneer.hardware

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.IMU as QualcommIMU
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot


class IMU(
    private val hardwareMap: HardwareMap,
    private val imuName: String = "imu",
    private val unit: AngleUnit = AngleUnit.RADIANS
) : HardwareComponent {

    private lateinit var imu: QualcommIMU

    override fun init() {
        imu = hardwareMap.get(QualcommIMU::class.java, imuName).apply {
            initialize(
                QualcommIMU.Parameters(
                    RevHubOrientationOnRobot(
                        RevHubOrientationOnRobot.LogoFacingDirection.LEFT,
                        RevHubOrientationOnRobot.UsbFacingDirection.UP
                    )
                )
            )
            resetYaw()
        }
    }

    fun resetYaw() {
        imu.resetYaw()
    }

    val yaw get() = imu.getRobotYawPitchRollAngles().yaw
    val pitch get() = imu.getRobotYawPitchRollAngles().pitch
    val roll get() = imu.getRobotYawPitchRollAngles().roll

    val vyaw get() = imu.getRobotAngularVelocity(unit).zRotationRate
    val vpitch get() = imu.getRobotAngularVelocity(unit).xRotationRate
    val vroll get() = imu.getRobotAngularVelocity(unit).yRotationRate

    override fun toString(): String {
        return "Yaw: $yaw, Pitch: $pitch, Roll: $roll, Yaw Velocity: $vyaw, Pitch Velocity: $vpitch, Roll Velocity: $vroll"
    }

}
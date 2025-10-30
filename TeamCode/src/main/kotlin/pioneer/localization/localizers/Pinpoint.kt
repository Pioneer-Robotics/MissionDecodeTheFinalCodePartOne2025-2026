package pioneer.localization.localizers

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D
import pioneer.Constants
import pioneer.localization.Localizer
import pioneer.helpers.Pose

/**
 * GoBILDA Pinpoint localizer with coordinate conversion.
 * Pinpoint: X+ forward, Y+ left → Robot: X+ right, Y+ forward
 */
class Pinpoint(
    hardwareMap: HardwareMap,
    startPose: Pose = Pose(),
) : Localizer {
    override var pose: Pose = startPose
    override var prevPose: Pose = startPose.copy()

    override var encoderXTicks: Int = 0
    override var encoderYTicks: Int = 0

    private val pinpoint = hardwareMap.get(GoBildaPinpointDriver::class.java, Constants.HardwareNames.PINPOINT)

    init {
        pinpoint.setOffsets(Constants.Pinpoint.X_POD_OFFSET_MM, Constants.Pinpoint.Y_POD_OFFSET_MM, DistanceUnit.MM)
        pinpoint.setEncoderResolution(Constants.Pinpoint.ENCODER_RESOLUTION)
        pinpoint.setEncoderDirections(Constants.Pinpoint.X_ENCODER_DIRECTION, Constants.Pinpoint.Y_ENCODER_DIRECTION)
        pinpoint.recalibrateIMU()
        // Coordinate conversion: robot_y → pinpoint_x, -robot_x → pinpoint_y, -robot_θ → pinpoint_θ
        pinpoint.setPosition(Pose2D(DistanceUnit.CM, startPose.y, -startPose.x, AngleUnit.RADIANS, -startPose.theta))
        pinpoint.update()
    }

    override fun update(dt: Double) {
        pinpoint.update()

        // Coordinate conversion: robot_x = -pinpoint_y, robot_y = pinpoint_x
        val x = -pinpoint.getPosY(DistanceUnit.CM)
        val y = pinpoint.getPosX(DistanceUnit.CM)
        val vx = -pinpoint.getVelY(DistanceUnit.CM)
        val vy = pinpoint.getVelX(DistanceUnit.CM)

        // Numerical differentiation for acceleration
        val ax = (vx - prevPose.vx) / dt
        val ay = (vy - prevPose.vy) / dt

        // Angular motion with coordinate conversion
        val theta = pinpoint.getHeading(AngleUnit.RADIANS)
        val omega = (theta - prevPose.theta) / dt
        val alpha = (omega - prevPose.alpha) / dt

        prevPose = pose
        pose = Pose(x, y, vx, vy, ax, ay, theta, omega, alpha)

        encoderXTicks = pinpoint.encoderX
        encoderYTicks = pinpoint.encoderY
    }

    override fun reset(pose: Pose) {
        this.pose = pose
        // Coordinate conversion back to Pinpoint system
        pinpoint.setPosition(Pose2D(DistanceUnit.CM, pose.y, -pose.x, AngleUnit.RADIANS, -pose.theta))
    }
}

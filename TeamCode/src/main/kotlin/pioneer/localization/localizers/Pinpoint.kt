package pioneer.localization.localizers

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D
import org.firstinspires.ftc.robotcore.external.navigation.UnnormalizedAngleUnit

import pioneer.Constants.Pinpoint as PinpointConstants
import pioneer.localization.Localizer
import pioneer.localization.Pose

class Pinpoint (hardwareMap: HardwareMap, startPose: Pose = Pose()) : Localizer {
    // Current pose, velocity, and acceleration
    override var pose: Pose = startPose
    override var velocity: Pose = Pose()
    /** Acceleration is not directly available from Pinpoint. */
    override var acceleration: Pose = Pose()

    // Pinpoint driver
    private val pinpoint = hardwareMap.get(GoBildaPinpointDriver::class.java, HardwareNames.PINPOINT)

    private var prevVelocity = Pose()

    init {
        pinpoint.setOffsets(PinpointConstants.X_POD_OFFSET, PinpointConstants.Y_POD_OFFSET, DistanceUnit.MM)
        pinpoint.setEncoderResolution(PinpointConstants.ENCODER_RESOLUTION)
        pinpoint.setEncoderDirections(PinpointConstants.X_ENCODER_DIRECTION, PinpointConstants.Y_ENCODER_DIRECTION)
        pinpoint.recalibrateIMU()
        pinpoint.setPosition(Pose2D(DistanceUnit.CM, startPose.y, -startPose.x, AngleUnit.RADIANS, -startPose.heading))
        pinpoint.update()
    }

    override fun update(deltaTime: Double) {
        pinpoint.update()
        // Update pose and velocity based on the pinpoint data
        // Pinpoint uses X+ as forward, Y+ as left
        // We need to convert this to our coordinate system where X+ is right, Y+ is forward
        // Heading is changed from range [0, 2π) to [-π, π)
        pose = Pose(
            -pinpoint.getPosY(DistanceUnit.CM),
            pinpoint.getPosX(DistanceUnit.CM),
            -pinpoint.getHeading(AngleUnit.RADIANS)
        )
        velocity = Pose(
            -pinpoint.getVelY(DistanceUnit.CM),
            pinpoint.getVelX(DistanceUnit.CM),
            -pinpoint.getHeadingVelocity(UnnormalizedAngleUnit.RADIANS)
        )
        acceleration = Pose(
            (velocity.x - prevVelocity.x) / deltaTime,
            (velocity.y - prevVelocity.y) / deltaTime,
            (velocity.heading - prevVelocity.heading) / deltaTime
        )
        prevVelocity = velocity
    }

    override fun reset(pose: Pose) {
        this.pose = pose
        pinpoint.setPosition(Pose2D(DistanceUnit.CM, pose.x, pose.y, AngleUnit.RADIANS, pose.heading))
    }
}

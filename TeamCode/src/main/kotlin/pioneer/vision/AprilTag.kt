package pioneer.vision

import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase
import org.firstinspires.ftc.vision.apriltag.AprilTagLibrary
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.Position
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles

/**
 * AprilTag processor with camera offset configuration.
 *
 * @property x The X position offset of the camera (default: 0.0)
 * @property y The Y position offset of the camera (default: 0.0)
 * @property z The Z position offset of the camera (default: 0.0)
 * @property yaw The yaw angle offset of the camera (default: 0.0)
 * @property pitch The pitch angle offset of the camera (default: 0.0)
 * @property roll The roll angle offset of the camera (default: 0.0)
 * @property distanceUnit The unit for distance measurements (default: CM)
 * @property angleUnit The unit for angle measurements (default: RADIANS)
 * @property draw Whether to draw debug visualizations on the camera stream (default: false)
 */
class AprilTag(
    private val x: Double = 0.0,
    private val y: Double = 0.0,
    private val z: Double = 0.0,
    private val yaw: Double = 0.0,
    private val pitch: Double = 0.0,
    private val roll: Double = 0.0,
    private val distanceUnit: DistanceUnit = DistanceUnit.CM,
    private val angleUnit: AngleUnit = AngleUnit.RADIANS,
    private val draw: Boolean = false,
): Processor {
    private val position = Position(distanceUnit, x, y, z, 0)
    private val orientation = YawPitchRollAngles(angleUnit, yaw, pitch, roll, 0)
    private val library: AprilTagLibrary =
        AprilTagLibrary
            .Builder()
            .addTags(AprilTagGameDatabase.getCurrentGameTagLibrary())
            .build()

    override val processor: AprilTagProcessor =
        AprilTagProcessor
            .Builder()
            .setTagLibrary(library)
            .setCameraPose(position, orientation)
            // .setLensIntrinsics(CameraConstants.fx, CameraConstants.fy, CameraConstants.cx, CameraConstants.cy)
            .setOutputUnits(distanceUnit, angleUnit)
            .setDrawTagID(draw)
            .setDrawTagOutline(draw)
            .setDrawAxes(draw)
            .setDrawCubeProjection(draw)
            .build()
}
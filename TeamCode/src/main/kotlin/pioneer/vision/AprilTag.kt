package pioneer.vision

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.Position
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles
import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase
import org.firstinspires.ftc.vision.apriltag.AprilTagLibrary
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import pioneer.Constants

/**
 * AprilTag processor with camera offset configuration.
 *
 * @property xyz The position offset of the camera as a list [x, y, z]
 * @property rpy The orientation offset of the camera as a list [roll, pitch, yaw]
 * @property distanceUnit The unit for distance measurements
 * @property angleUnit The unit for angle measurements
 * @property draw Whether to draw debug visualizations on the camera stream
 */
class AprilTag(
    private val xyz: List<Double> = Constants.Camera.XYZ_OFFSET,
    private val rpy: List<Double> = Constants.Camera.RPY_OFFSET,
    private val distanceUnit: DistanceUnit = Constants.Camera.XYZ_UNITS,
    private val angleUnit: AngleUnit = Constants.Camera.RPY_UNITS,
    private val draw: Boolean = false,
) : Processor {
    init {
        require(xyz.size == 3) { "xyz must have exactly 3 elements: [x, y, z]" }
        require(rpy.size == 3) { "rpy must have exactly 3 elements: [roll, pitch, yaw]" }
    }

    private val position = Position(distanceUnit, xyz[0], xyz[1], xyz[2], 0)
    private val orientation = YawPitchRollAngles(angleUnit, rpy[2], rpy[1], rpy[0], 0)
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
            // .setLensIntrinsics(CameraConstants.fx, CameraConstants.fy, CameraConstants.cx, CameraConstants.cy) // Intrinsics are set in XML
            .setOutputUnits(distanceUnit, angleUnit)
            .setDrawTagID(draw)
            .setDrawTagOutline(draw)
            .setDrawAxes(draw)
            .setDrawCubeProjection(draw)
            .build()
}

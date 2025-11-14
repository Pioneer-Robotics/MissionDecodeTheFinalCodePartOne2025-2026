package pioneer.vision

import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase
import org.firstinspires.ftc.vision.apriltag.AprilTagLibrary
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.Position
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles

class AprilTag(
    private val position: Position = Position(DistanceUnit.CM, 0.0, 0.0, 0.0, 0),
    private val orientation: YawPitchRollAngles = YawPitchRollAngles(AngleUnit.RADIANS, 0.0, 0.0, 0.0, 0),
    private val distanceUnit: DistanceUnit = DistanceUnit.CM,
    private val angleUnit: AngleUnit = AngleUnit.RADIANS,
    private val draw: Boolean = false,
): Processor {
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
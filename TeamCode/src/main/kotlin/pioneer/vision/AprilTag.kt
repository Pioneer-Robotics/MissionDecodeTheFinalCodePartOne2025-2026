package pioneer.vision

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.Position
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraIntrinsics
import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase
import org.firstinspires.ftc.vision.apriltag.AprilTagLibrary
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import pioneer.constants.Camera
import kotlin.jvm.internal.Intrinsics


class AprilTag(
    position: Position = Position(DistanceUnit.CM, 0.0, 0.0, 0.0, 0),
    orientation: YawPitchRollAngles = YawPitchRollAngles(AngleUnit.RADIANS, 0.0, 0.0, 0.0, 0),
    distanceUnit: DistanceUnit = DistanceUnit.CM,
    angleUnit: AngleUnit = AngleUnit.RADIANS,
    draw: Boolean = false,
) : Processor {
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
            .setLensIntrinsics(Camera.fx, Camera.fy, Camera.cx, Camera.cy)
            .setOutputUnits(distanceUnit, angleUnit)
            .setDrawTagID(draw)
            .setDrawTagOutline(draw)
            .setDrawAxes(draw)
            .setDrawCubeProjection(draw)
            .build()


}

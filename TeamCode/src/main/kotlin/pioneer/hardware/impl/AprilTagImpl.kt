package pioneer.hardware.impl

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.Position
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles
import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase
import org.firstinspires.ftc.vision.apriltag.AprilTagLibrary
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import pioneer.hardware.interfaces.AprilTag

class AprilTagImpl(
    position: Position = Position(DistanceUnit.INCH, 0.0, 0.0, 0.0),
    orientation: YawPitchRollAngles = YawPitchRollAngles(AngleUnit.DEGREES, 0.0, 0.0, 0.0),
) : AprilTag {

    private val library: AprilTagLibrary =
        AprilTagLibrary
            .Builder()
            .addTags(AprilTagGameDatabase.getCurrentGameTagLibrary())
            .build()

    val processor: AprilTagProcessor =
        AprilTagProcessor
            .Builder()
            .setTaglibrary(library)
            .setCameraPose(position, orientation)
            .build()
}

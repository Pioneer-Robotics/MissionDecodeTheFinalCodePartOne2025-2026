package pioneer.hardware.impl

import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.Position
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles
import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase
import org.firstinspires.ftc.vision.apriltag.AprilTagLibrary
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection
import pioneer.hardware.interfaces.AprilTag
import kotlin.jvm.java

class AprilTagImpl(
    position: Position = Position(DistanceUnit.INCH, 0.0, 0.0, 0.0),
    orientation: YawPitchRollAngles = YawPitchRollAngles(AngleUnit.DEGREES, 0.0, 0.0, 0.0),
) : AprilTag {
    override private val processor: AprilTagProcessor

    private val library: AprilTagLibrary =
        AprilTagLibrary
            .Builder()
            .addTags(AprilTagGameDatabase.getCurrentGameTagLibrary())
            .build()

    val builder: AprilTagProcessor.Builder =
        AprilTagProcessor
            .Builder()
            .setTaglibrary(library)
            .setCameraPose(position, orientation)
            .build()

    fun tags(): List<AprilTagDetection> {
        return processor.getDetections()
    }

    fun numTags(): Int {
        return tags().size()
    }   
}

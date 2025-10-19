package pioneer.hardware.impl

import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName
import org.firstinspires.ftc.vision.VisionPortal
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import pioneer.Constants
import pioneer.hardware.interfaces.AprilTag
import kotlin.jvm.java

class AprilTagImpl (hardwareMap: HardwareMap) : AprilTag {
    override val aprilTag: AprilTagProcessor = AprilTagProcessor.Builder().build()
    private val visionPortal: VisionPortal

    init {
        val builder: VisionPortal.Builder = VisionPortal.Builder()

        builder.setCamera(hardwareMap.get(WebcamName::class.java, Constants.HardwareNames.WEBCAM))

        builder.addProcessor(aprilTag)
        visionPortal = builder.build()
    }
}

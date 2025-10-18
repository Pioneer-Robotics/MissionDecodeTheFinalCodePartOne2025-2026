package org.firstinspires.ftc.teamcode.hardware.implementations

import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName
import org.firstinspires.ftc.teamcode.HardwareNames
import org.firstinspires.ftc.teamcode.hardware.interfaces.AprilTag
import org.firstinspires.ftc.vision.VisionPortal
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor

class AprilTagImpl (hardwareMap: HardwareMap) : AprilTag {
    override val aprilTag: AprilTagProcessor = AprilTagProcessor.Builder().build()
    private val visionPortal: VisionPortal

    init {
        val builder: VisionPortal.Builder = VisionPortal.Builder()

        builder.setCamera(hardwareMap.get(WebcamName::class.java, HardwareNames.WEBCAM))

        builder.addProcessor(aprilTag)
        visionPortal = builder.build()
    }
}
package org.firstinspires.ftc.teamcode.hardware.mocks

import org.firstinspires.ftc.teamcode.hardware.interfaces.AprilTag
import org.firstinspires.ftc.teamcode.helpers.FileLogger
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor

class AprilTagMock : AprilTag {
    override val aprilTag: AprilTagProcessor
        get() {
            FileLogger.warn("April Tag", "April Tag not initialized")
            return AprilTagProcessor.Builder().build()
        }
}
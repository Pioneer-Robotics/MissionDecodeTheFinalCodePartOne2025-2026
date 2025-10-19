package pioneer.hardware.mock

import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import pioneer.hardware.interfaces.AprilTag
import pioneer.helpers.FileLogger

class AprilTagMock : AprilTag {
    override val processor: AprilTagProcessor
        get() {
            FileLogger.warn("NOT INITIALIZED", "processor getter called on AprilTagMock")
            return AprilTagProcessor.Builder().build()
        }
}

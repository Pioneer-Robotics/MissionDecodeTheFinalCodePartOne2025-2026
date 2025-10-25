package pioneer.hardware.mock

import org.firstinspires.ftc.vision.VisionPortal
import pioneer.hardware.interfaces.Camera
import pioneer.helpers.FileLogger

class CameraMock : Camera {
    override val portal: VisionPortal
        get() {
            FileLogger.warn("NOT INITIALIZED", "portal getter called on CameraMock")
            return VisionPortal.Builder().build()
        }
}

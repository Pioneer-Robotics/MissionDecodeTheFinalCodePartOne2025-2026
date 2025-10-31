package pioneer.hardware.base

import org.firstinspires.ftc.vision.VisionPortal

open class CameraBase {
    open val portal: VisionPortal
        get() = VisionPortal.Builder().build()
}

package pioneer.hardware.impl

import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName
import org.firstinspires.ftc.vision.VisionPortal
import org.firstinspires.ftc.vision.VisionProcessor
import kotlin.jvm.java

class CameraImpl(
    hardwareMap: HardwareMap,
    name: String,
    processors: List<VisionProcessor> = emptyList(),
) : Camera {
    override private val portal: VisionPortal = VisionPortal

    val builder: VisionPortal.Builder =
        VisionPortal
            .Builder()
            .setCamera(hardwareMap.get(WebcamName::class.java, name))
            .apply {
                if (processors.isNotEmpty()) {
                    addProcessors(processors)
                }
            }.build()

    val state: VisionPortal.State
        get() = portal.getState()
}

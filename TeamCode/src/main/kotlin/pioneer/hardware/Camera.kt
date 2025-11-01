package pioneer.hardware

import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName
import org.firstinspires.ftc.vision.VisionPortal
import org.firstinspires.ftc.vision.VisionProcessor
import kotlin.jvm.java

class Camera(
    hardwareMap: HardwareMap,
    name: String = "Webcam 1",
    processors: Array<VisionProcessor> = emptyArray(),
) {
    val portal: VisionPortal =
        VisionPortal
            .Builder()
            .setCamera(hardwareMap.get(WebcamName::class.java, name))
            .apply {
                if (processors.isNotEmpty()) {
                    addProcessors(*processors)
                }
            }.build()
}

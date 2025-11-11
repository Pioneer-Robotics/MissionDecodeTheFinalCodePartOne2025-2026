package pioneer.hardware

import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.Position
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles
import org.firstinspires.ftc.vision.VisionPortal
import org.firstinspires.ftc.vision.VisionProcessor
import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase
import org.firstinspires.ftc.vision.apriltag.AprilTagLibrary
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import pioneer.constants.HardwareNames
import kotlin.jvm.java

class Camera(
    private val hardwareMap: HardwareMap,
    private val cameraName: String = HardwareNames.WEBCAM,
    private val processors: Array<VisionProcessor> = emptyArray(),
) : HardwareComponent {
    override val name = "Camera"

    private lateinit var portal: VisionPortal

    override fun init() {
        portal =
            VisionPortal
                .Builder()
                .setCamera(hardwareMap.get(WebcamName::class.java, cameraName))
                .apply {
                    if (processors.isNotEmpty()) {
                        addProcessors(*processors)
                    }
                }.build()
    }

    companion object {
        fun createAprilTagProcessor(
            position: Position = Position(DistanceUnit.CM, 0.0, 0.0, 0.0, 0),
            orientation: YawPitchRollAngles = YawPitchRollAngles(AngleUnit.RADIANS, 0.0, 0.0, 0.0, 0),
        ): VisionProcessor {
            val library: AprilTagLibrary =
                AprilTagLibrary
                    .Builder()
                    .addTags(AprilTagGameDatabase.getCurrentGameTagLibrary())
                    .build()

            val processor: AprilTagProcessor =
                AprilTagProcessor
                    .Builder()
                    .setTagLibrary(library)
                    .setCameraPose(position, orientation)
                    .build()

            return processor
        }
    }
}

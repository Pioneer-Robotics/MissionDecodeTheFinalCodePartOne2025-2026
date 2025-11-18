package pioneer.hardware

import android.util.Size
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
import pioneer.constants.Camera as CameraConstants
import kotlin.jvm.java

class Camera(
    private val hardwareMap: HardwareMap,
    private val cameraName: String = HardwareNames.WEBCAM,
    val processors: Array<VisionProcessor> = emptyArray(),
) : HardwareComponent {

    private lateinit var portal: VisionPortal

    override fun init() {
       portal =
        VisionPortal
            .Builder()
            .setCamera(hardwareMap.get(WebcamName::class.java, cameraName))
            .setCameraResolution(Size(640, 480))
            .enableLiveView(true)
            .apply {
                if (processors.isNotEmpty()) {
                    addProcessors(*processors)
                }
            }.build()
    }

    // Helper function to get a specific processor by type
    private inline fun <reified T : VisionProcessor> getProcessor(): T? = processors.filterIsInstance<T>().firstOrNull()

    fun close() {
        portal.close()
    }

    companion object {
        fun createAprilTagProcessor(
            position: Position = Position(DistanceUnit.CM, 0.0, 0.0, 0.0, 0),
            orientation: YawPitchRollAngles = YawPitchRollAngles(AngleUnit.RADIANS, 0.0, 0.0, 0.0, 0),
            distanceUnit: DistanceUnit = DistanceUnit.CM,
            angleUnit: AngleUnit = AngleUnit.RADIANS,
            draw: Boolean = false,
        ): AprilTagProcessor {
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
                    .setLensIntrinsics(CameraConstants.fx, CameraConstants.fy, CameraConstants.cx, CameraConstants.cy)
                    .setOutputUnits(distanceUnit, angleUnit)
                    .setDrawTagID(draw)
                    .setDrawTagOutline(draw)
                    .setDrawAxes(draw)
                    .setDrawCubeProjection(draw)
                    .build()

            return processor
        }
    }
}

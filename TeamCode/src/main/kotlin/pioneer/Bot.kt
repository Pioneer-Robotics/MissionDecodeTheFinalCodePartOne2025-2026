package pioneer

import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import pioneer.vision.AprilTag
import pioneer.hardware.BatteryMonitor
import pioneer.hardware.Camera
import pioneer.hardware.Flywheel
import pioneer.hardware.LaunchServos
import pioneer.hardware.MecanumBase
import pioneer.hardware.MockHardware
import pioneer.localization.Localizer
import pioneer.localization.localizers.LocalizerMock
import pioneer.localization.localizers.Pinpoint
import pioneer.pathing.follower.Follower
import pioneer.constants.Camera as CameraConstants

enum class BotType(
    val supportsLocalizer: Boolean,
) {
    BASIC_MECANUM_BOT(false),
    GOBILDA_STARTER_BOT(true),
}

class Bot(
    val botType: BotType,
    hardwareMap: HardwareMap,
) {
    // Basic hardware components
    var mecanumBase: MecanumBase = MecanumBase(MockHardware())
    var localizer: Localizer = LocalizerMock()
    var batteryMonitor: BatteryMonitor = BatteryMonitor(MockHardware())

    // GoBilda starter bot specific components
    var flywheel: Flywheel = Flywheel(MockHardware())
    var launchServos: LaunchServos = LaunchServos(MockHardware())

    // Other hardware components
    var aprilTagProcessor: AprilTagProcessor = AprilTag().processor
    var camera: Camera? = null // Don't create default Camera - prevents multiple VisionPortals

    // Path follower
    var follower = Follower(this)

    init {
        when (botType) {
            BotType.BASIC_MECANUM_BOT -> {
                // Initialize hardware components for Basic Mecanum Bot
                // mecanumBase = MecanumBase(hardwareMap)
                // batteryMonitor = BatteryMonitor(hardwareMap)
            }

            BotType.GOBILDA_STARTER_BOT -> {
                // Initialize hardware components for GoBilda Starter Bot
                // mecanumBase = MecanumBase(hardwareMap)  // Uncomment if you have mecanum motors
                // localizer = Pinpoint(hardwareMap)  // Uncomment if you have Pinpoint sensor
                // batteryMonitor = BatteryMonitor(hardwareMap)  // Uncomment if needed
                // flywheel = Flywheel(hardwareMap)  // Uncomment if you have flywheel motor
                // launchServos = LaunchServos(hardwareMap)  // Uncomment if you have launch servos

                aprilTagProcessor =
                    AprilTag(
                        CameraConstants.POSITION_CM,
                        CameraConstants.ORIENTATION_RAD,
                        draw = true,
                    ).processor
                camera =
                    Camera(
                        hardwareMap,
                        processors = arrayOf(aprilTagProcessor),
                    )
            }
        }
    }
}

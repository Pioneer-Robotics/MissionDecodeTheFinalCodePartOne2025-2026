package pioneer

import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import com.qualcomm.robotcore.hardware.HardwareMap
import pioneer.hardware.impl.BatteryMonitorImpl
import pioneer.hardware.impl.MecanumBaseImpl
import pioneer.hardware.interfaces.BatteryMonitor
import pioneer.hardware.interfaces.MecanumBase
import pioneer.hardware.mock.BatteryMonitorMock
import pioneer.hardware.mock.MecanumBaseMock
import pioneer.helpers.DeltaTimeTracker
import pioneer.localization.Localizer
import pioneer.localization.localizers.LocalizerMock
import pioneer.localization.localizers.Pinpoint
import pioneer.pathing.follower.Follower

enum class BotType {
    BASIC_MECANUM_BOT,
    GOBILDA_STARTER_BOT,
}

class Bot(botType: BotType, hardwareMap: HardwareMap) {
    // Delta time tracker
    var dtTracker = DeltaTimeTracker()

    // Basic hardware components
    var mecanumBase: MecanumBase = MecanumBaseMock()
    var localizer: Localizer = LocalizerMock()
    var batteryMonitor: BatteryMonitor = BatteryMonitorMock()

    // Path follower
    var follower = Follower(this)

    init {
        when (botType) {
            BotType.BASIC_MECANUM_BOT -> {
                // Initialize hardware components for Basic Mecanum Bot
                mecanumBase = MecanumBaseImpl(hardwareMap)
                localizer = Pinpoint(hardwareMap)
                batteryMonitor = BatteryMonitorImpl(hardwareMap)
            }

            BotType.GOBILDA_STARTER_BOT -> {
                // Initialize hardware components for GoBilda Starter Bot
                mecanumBase = MecanumBaseImpl(hardwareMap)
                localizer = Pinpoint(hardwareMap)
                batteryMonitor = BatteryMonitorImpl(hardwareMap)
            }
        }
    }
}

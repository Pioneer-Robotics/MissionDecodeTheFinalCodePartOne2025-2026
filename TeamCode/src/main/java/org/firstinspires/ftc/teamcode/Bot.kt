package org.firstinspires.ftc.teamcode

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.teamcode.hardware.Flywheel
import org.firstinspires.ftc.teamcode.hardware.implementations.AprilTagImpl
import org.firstinspires.ftc.teamcode.hardware.implementations.FlywheelImpl
import org.firstinspires.ftc.teamcode.hardware.implementations.LaunchServosImpl
import org.firstinspires.ftc.teamcode.hardware.implementations.MecanumBaseImpl
import org.firstinspires.ftc.teamcode.hardware.implementations.VoltageHandlerImpl
import org.firstinspires.ftc.teamcode.hardware.interfaces.AprilTag
import org.firstinspires.ftc.teamcode.hardware.interfaces.LaunchServos
import org.firstinspires.ftc.teamcode.hardware.interfaces.MecanumBase
import org.firstinspires.ftc.teamcode.hardware.interfaces.VoltageHandler
import org.firstinspires.ftc.teamcode.hardware.mocks.AprilTagMock
import org.firstinspires.ftc.teamcode.hardware.mocks.FlywheelMock
import org.firstinspires.ftc.teamcode.hardware.mocks.LaunchServosMock
import org.firstinspires.ftc.teamcode.hardware.mocks.MecanumBaseMock
import org.firstinspires.ftc.teamcode.hardware.mocks.VoltageHandlerMock
import org.firstinspires.ftc.teamcode.helpers.DashboardPlotter
import org.firstinspires.ftc.teamcode.helpers.FileLogger
import org.firstinspires.ftc.teamcode.localization.Localizer
import org.firstinspires.ftc.teamcode.localization.localizers.LocalizerMock
import org.firstinspires.ftc.teamcode.localization.localizers.Pinpoint
import org.firstinspires.ftc.teamcode.pathing.follower.Follower

class Bot(botFlavor: BotFlavor, hardwareMap: HardwareMap) {
    enum class BotFlavor {
        GOBILDA_STARTER_BOT,
    }

    // Timer
    private val timer: ElapsedTime = ElapsedTime()
    private var prevTime: Double = timer.milliseconds()
    var dt: Double = 0.0 // Delta time in milliseconds
        private set

    // Telemetry
    var telemetryPacket: TelemetryPacket = TelemetryPacket(false)

    // Basic bot
    var mecanumBase: MecanumBase = MecanumBaseMock()
    var voltageHandler: VoltageHandler = VoltageHandlerMock()
    var localizer: Localizer = LocalizerMock()

    // General hardware
    var aprilTagProcessor: AprilTag = AprilTagMock()

    // GoBilda starter bot
    var launchServos: LaunchServos = LaunchServosMock()
    var flywheel: Flywheel = FlywheelMock()

    // Follower
    var follower = Follower(this)

    init {
        when (botFlavor) {
            BotFlavor.GOBILDA_STARTER_BOT -> {
                mecanumBase = MecanumBaseImpl(hardwareMap)
                voltageHandler = VoltageHandlerImpl(hardwareMap)
                localizer = Pinpoint(hardwareMap)

                aprilTagProcessor = AprilTagImpl(hardwareMap)
                launchServos = LaunchServosImpl(hardwareMap)
                flywheel = FlywheelImpl(hardwareMap)
            }
        }
    }

    fun update() {
        // Update delta time
        dt = timer.milliseconds() - prevTime
        prevTime = timer.milliseconds()
    }

    fun sendTelemetryPacket() {
        FtcDashboard.getInstance().sendTelemetryPacket(telemetryPacket)
        telemetryPacket = TelemetryPacket() // Reset the packet for the next loop
    }

    fun stop() {
        mecanumBase.stop()
        FileLogger.flush() // Save any pending logs
        DashboardPlotter.clearPreviousPositions()
    }
}

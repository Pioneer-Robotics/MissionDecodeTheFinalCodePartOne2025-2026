package pioneer.opmodes

import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import pioneer.Bot
import pioneer.BotType
import pioneer.helpers.DeltaTimeTracker
import pioneer.helpers.FileLogger

// Base OpMode class to be extended by all user-defined OpModes
abstract class BaseOpMode(
    private val botType: BotType = BotType.BASIC_MECANUM_BOT
) : OpMode() {

    // Bot instance
    protected lateinit var bot: Bot
        private set // Prevent external modification

    // Telemetry packet for dashboard
    protected var telemetryPacket = TelemetryPacket()

    // Dashboard instance
    private val dashboard = com.acmerobotics.dashboard.FtcDashboard.getInstance()

    // Tracker and getter for dt
    private val dtTracker = DeltaTimeTracker()
    protected val dt: Double
        get() = dtTracker.dt

    final override fun init() {
        bot = Bot(botType, hardwareMap)
        onInit() // Call user-defined init method
        updateTelemetry()
    }

    final override fun loop() {
        // Update bot systems
        dtTracker.update()
        if (bot.botType.supportsLocalizer) {
            bot.localizer.update(dt)
        }

        // Call user-defined loop logic
        onLoop()

        // Update path follower
        if (bot.botType.supportsLocalizer) {
            bot.follower.update(dt)
        }

        // Automatically handle telemetry updates
        updateTelemetry()
    }

    final override fun stop() {
        bot.mecanumBase.stop() // Ensure motors are stopped
        FileLogger.flush() // Flush any logged data
        onStop() // Call user-defined stop method
    }

    private fun updateTelemetry() {
        telemetry.update()
        dashboard.sendTelemetryPacket(telemetryPacket)
        telemetryPacket = TelemetryPacket() // Reset packet for next loop
    }

    // These functions are meant to be overridden in subclasses
    protected open fun onInit() {}
    protected open fun onLoop() {}
    protected open fun onStop() {}
}

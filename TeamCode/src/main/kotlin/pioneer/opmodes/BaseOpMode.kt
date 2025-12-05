package pioneer.opmodes

import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import pioneer.Bot
import pioneer.hardware.MecanumBase
import pioneer.helpers.Chrono
import pioneer.helpers.FileLogger
import pioneer.localization.localizers.Pinpoint

// Base OpMode class to be extended by all user-defined OpModes
abstract class BaseOpMode : OpMode() {
    // Bot instance to be defined in subclasses
    protected lateinit var bot: Bot

    // Telemetry packet for dashboard
    protected var telemetryPacket = TelemetryPacket()

    // Dashboard instance
    private val dashboard =
        com.acmerobotics.dashboard.FtcDashboard
            .getInstance()

    // Tracker and getter for dt
    protected val chrono = Chrono()
    protected val dt: Double
        get() = chrono.dt

    val elapsedTime: Double
        get() = getRuntime()

    final override fun init() {
        onInit() // Call user-defined init method
        bot.initAll() // Initialize bot hardware
        if (!::bot.isInitialized) {
            throw IllegalStateException("Bot not initialized. Please set 'bot' in onInit().")
        }
        updateTelemetry()
    }

    final override fun loop() {
        // Update bot systems
        bot.updateAll(dt)

        // Call user-defined loop logic
        onLoop()

        // Update path follower
        if (bot.has<Pinpoint>() && bot.has<MecanumBase>()) {
            bot.follower.update(dt)
            telemetry.addLine("Updated follower")
        }

        // Automatically handle telemetry updates
        updateTelemetry()
    }

    final override fun stop() {
        bot.mecanumBase?.stop() // Ensure motors are stopped
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

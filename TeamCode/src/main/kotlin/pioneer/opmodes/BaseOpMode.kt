package pioneer.opmodes

import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.Bot
import pioneer.hardware.MecanumBase
import pioneer.localization.localizers.Pinpoint
import pioneer.helpers.Chrono
import pioneer.helpers.FileLogger
import pioneer.general.Period
import pioneer.general.AllianceColor
import pioneer.helpers.OpModeDataTransfer

// Base OpMode class to be extended by all user-defined OpModes
abstract class BaseOpMode(
    private val period: Period = Period.NONE,
    private val allianceColor: AllianceColor = AllianceColor.NONE
) : OpMode() {
    // Bot instance
    protected lateinit var bot: Bot
    
    // Telemetry packet for dashboard
    protected var telemetryPacket = TelemetryPacket()

    // Dashboard instance
    private val dashboard =
        FtcDashboard.getInstance()

    // Tracker and getter for dt
    protected val chrono = Chrono()
    protected val dt: Double
        get() = chrono.dt

    val elapsedTime: Double
        get() = getRuntime()

    final override fun init() {
        // Auto-load bot in TELEOP period
        if (period == Period.TELEOP) {
            OpModeDataTransfer.loadOrNull()?.let { omdt ->
                // Reinitialize bot from saved object
                omdt.bot?.let { savedBot ->
                    bot = savedBot
                    bot.initAll() // Re-initialize hardware
                }
            }
        }
        
        // If bot wasn't loaded from OMDT, call user init
        if (!::bot.isInitialized) {
            onInit() // Call user-defined init method
            if (!::bot.isInitialized) {
                throw IllegalStateException("Bot not initialized. Please set 'bot' in onInit().")
            }
            bot.initAll() // Initialize bot hardware
        } else {
            // Bot was loaded, but still call onInit for any additional setup
            onInit()
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
        
        // Auto-save data in AUTO period, clear in TELEOP
        when (period) {
            Period.AUTO -> {
                val omdt = OpModeDataTransfer.OMDT(
                    bot = bot,
                )
                OpModeDataTransfer.save(omdt)
            }
            Period.TELEOP -> {
                OpModeDataTransfer.clear()
            }
            else -> { /* No data transfer for NONE */ }
        }
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

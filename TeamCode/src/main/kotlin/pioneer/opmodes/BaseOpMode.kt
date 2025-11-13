package pioneer.opmodes

import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.Bot
import pioneer.hardware.MecanumBase
import pioneer.helpers.Chrono
import pioneer.helpers.FileLogger
import pioneer.general.Period
import pioneer.general.AllianceColor
import pioneer.helpers.OpModeDataTransfer
import pioneer.helpers.OpModeDataTransfer.OMDT

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

    final override fun init() {
        onInit() // Call user-defined init method
        bot.initAll() // Initialize bot hardware
        if (!::bot.isInitialized) {
            throw IllegalStateException("Bot not initialized. Please set 'bot' in onInit().")
        }
        updateTelemetry()

        // TODO: Finish loading OpModeDataTransfer
        if (period == Period.TELEOP) {
            OpModeDataTransfer.loadOrNull()?.let { omdt ->
                omdt.pose?.let { pose ->
                    if (bot.botType.supportsLocalizer) {
                    bot.localizer.pose = pose
                    }
                }
                omdt.data["alliance"]?.let { alliance ->
                    if (alliance is AllianceColor) {
                    // Handle alliance color if needed
                    }
                }
            }
        }
    }

    final override fun loop() {
        // Update bot systems
        bot.pinpoint?.update(dt)

        // Call user-defined loop logic
        onLoop()

        // Update path follower
        if (bot.has<Pinpoint>() && bot.has<MecanumBase>()) {
            bot.follower.update(dt)
        }

        // Automatically handle telemetry updates
        updateTelemetry()
    }

    final override fun stop() {
        bot.mecanumBase?.stop() // Ensure motors are stopped
        FileLogger.flush() // Flush any logged data
        onStop() // Call user-defined stop method
        
        if (period == Period.AUTO) {
            // Save end pose to OpModeDataTransfer
            val omdt = OMDT().apply { 
                data["alliance"] = allianceColor
                if (bot.botType.supportsLocalizer) {
                    data["pose"] = bot.localizer.pose
                }
            }
            OpModeDataTransfer.save(omdt)
        }
        else if (period == Period.TELEOP) {
            OpModeDataTransfer.clear()
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

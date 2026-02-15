package pioneer.opmodes

import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.util.ElapsedTime
import pioneer.Bot
import pioneer.Constants
import pioneer.hardware.MecanumBase
import pioneer.helpers.FileLogger
import pioneer.helpers.Pose
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

    val run_timer = ElapsedTime()

    val elapsedTime: Double
        get() = run_timer.seconds()

    val allHubs: List<LynxModule> by lazy {
        hardwareMap.getAll(LynxModule::class.java)
    }

    final override fun init() {
        for (hub in allHubs) {
            hub.bulkCachingMode = LynxModule.BulkCachingMode.MANUAL
        }
        onInit() // Call user-defined init method
        bot.initAll() // Initialize bot hardware
        if (!::bot.isInitialized) {
            throw IllegalStateException("Bot not initialized. Please set 'bot' in onInit().")
        }
        updateTelemetry()

        // Transfer data
        bot.allianceColor = Constants.TransferData.allianceColor
        bot.pinpoint?.reset(Constants.TransferData.pose)
        bot.turret?.resetMotorPosition(Constants.TransferData.turretMotorTicks)
        bot.spindexer?.resetMotorPosition(Constants.TransferData.spindexerMotorTicks)
    }

    final override fun start() {
        onStart()
        run_timer.reset()
    }

    final override fun loop() {
        for (hub in allHubs) {
            hub.clearBulkCache()
        }
        // Update bot systems
        bot.updateAll()

        // Call user-defined loop logic
        onLoop()

        // Update path follower
        if (bot.has<Pinpoint>() && bot.has<MecanumBase>()) {
            bot.follower.update()
        }

        // Automatically handle telemetry updates
        updateTelemetry()
    }

    final override fun stop() {
        // Transfer data
        Constants.TransferData.allianceColor = bot.allianceColor
        Constants.TransferData.pose = bot.pinpoint?.pose ?: Pose()
        Constants.TransferData.turretMotorTicks = bot.turret?.currentTicks ?: 0
        Constants.TransferData.spindexerMotorTicks = bot.spindexer?.currentMotorTicks ?: 0

        bot.led?.clear()
        bot.mecanumBase?.stop() // Ensure motors are stopped
        FileLogger.flush() // Flush any logged data
        onStop() // Call user-defined stop method
    }

    enum class Verbose {
        DEBUG,
        INFO,
        FATAL
    }

    fun addTelemetryData(caption: String, value: Any ?= null, verbose: Verbose) {
        if (verbose.ordinal >= Constants.Misc.VERBOSE_LEVEL.ordinal) {
            telemetry.addData(caption, value)
        }
    }

    private fun updateTelemetry() {
        telemetry.update()
        dashboard.sendTelemetryPacket(telemetryPacket)
        telemetryPacket = TelemetryPacket() // Reset packet for next loop
    }

    // These functions are meant to be overridden in subclasses
    protected open fun onInit() {}

    protected open fun onStart() {}

    protected open fun onLoop() {}

    protected open fun onStop() {}
}

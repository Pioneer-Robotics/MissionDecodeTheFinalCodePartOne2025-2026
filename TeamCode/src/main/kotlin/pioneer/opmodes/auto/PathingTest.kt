package pioneer.opmodes.auto

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import pioneer.Bot
import pioneer.BotType
import pioneer.helpers.DashboardPlotter
import pioneer.helpers.FileLogger
import pioneer.pathing.paths.HermitePath
import pioneer.helpers.Pose
import kotlin.math.hypot

@Autonomous(name = "Pathing Test", group = "Testing")
class PathingTest : OpMode() {
    private lateinit var bot: Bot

    enum class State {
        INIT,
        RUNNING,
        DONE
    }

    private var state: State = State.INIT

    var telemetryPacket = TelemetryPacket()

    override fun init() {
        bot = Bot(BotType.GOBILDA_STARTER_BOT, hardwareMap)

        telemetryPacket.put("Target Velocity", 0.0)
        telemetryPacket.put("Current Velocity", 0.0)
        FtcDashboard.getInstance().sendTelemetryPacket(telemetryPacket)
    }

    override fun loop() {
        bot.dtTracker.update()
        bot.localizer.update(bot.dtTracker.dt)
        when (state) {
            State.INIT -> {
                bot.follower.path = HermitePath.Builder()
                    .addPoint(Pose(0.0, 0.0), Pose(50.0, 0.0))
                    .addPoint(Pose(50.0, 100.0), Pose(50.0, 0.0))
                    .build()
                bot.follower.start()
                state = State.RUNNING
            }
            State.RUNNING -> {
                if (bot.follower.done) state = State.DONE
                // Telemetry updates
                telemetryPacket.put("Target Velocity", bot.follower.targetState!!.v)
                telemetryPacket.put("Current Velocity", hypot(bot.localizer.pose.vx, bot.localizer.pose.vy))
                // Field view
                DashboardPlotter.plotGrid(telemetryPacket)
                DashboardPlotter.plotBotPosition(telemetryPacket, bot.localizer.pose)
                DashboardPlotter.plotPath(telemetryPacket, bot.follower.path!!)
                DashboardPlotter.plotPoint(telemetryPacket, bot.follower.path!!.getPoint(bot.follower.targetState!!.x / bot.follower.path!!.getLength()))
                FtcDashboard.getInstance().sendTelemetryPacket(telemetryPacket)
                telemetryPacket = TelemetryPacket() // Reset packet for next loop
            }
            State.DONE -> {
                requestOpModeStop()
            }
        }
    }

    override fun stop() {
        bot.mecanumBase.stop()
        FileLogger.flush()
    }
}

package org.firstinspires.ftc.teamcode.opmodes.auto

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import org.firstinspires.ftc.teamcode.Bot
import org.firstinspires.ftc.teamcode.helpers.DashboardPlotter
import org.firstinspires.ftc.teamcode.pathing.paths.HermitePath
import org.firstinspires.ftc.teamcode.localization.Pose

@Autonomous(name = "Pathing Test", group = "Testing")
class PathingTest : OpMode() {
    enum class State {
        INIT,
        RUNNING,
        DONE
    }

    private var state: State = State.INIT
    private lateinit var bot: Bot

    override fun init() {
        bot = Bot(Bot.BotFlavor.GOBILDA_STARTER_BOT, hardwareMap)
        bot.telemetryPacket.put("Target Velocity", 0.0)
        bot.telemetryPacket.put("Current Velocity", 0.0)
        bot.sendTelemetryPacket()
    }

    override fun loop() {
        bot.update()
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
                bot.telemetryPacket.put("Target Velocity", bot.follower.targetState!!.v)
                bot.telemetryPacket.put("Current Velocity", bot.localizer.velocity.getLength())
                // Field view
                DashboardPlotter.plotGrid(bot.telemetryPacket)
                DashboardPlotter.plotBotPosition(bot.telemetryPacket, bot.localizer.pose)
                DashboardPlotter.plotPath(bot.telemetryPacket, bot.follower.path!!)
                DashboardPlotter.plotPoint(bot.telemetryPacket, bot.follower.path!!.getPoint(bot.follower.targetState!!.x / bot.follower.path!!.getLength()))
                bot.sendTelemetryPacket()
            }
            State.DONE -> {
                requestOpModeStop()
            }
        }
    }

    override fun stop() {
        bot.stop()
    }
}
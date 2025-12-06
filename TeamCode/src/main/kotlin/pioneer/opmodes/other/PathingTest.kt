package pioneer.opmodes.auto

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import pioneer.Bot
import pioneer.BotType
import pioneer.helpers.DashboardPlotter
import pioneer.helpers.Pose
import pioneer.opmodes.BaseOpMode
import pioneer.pathing.paths.HermitePath
import kotlin.math.hypot

@Disabled
@Autonomous(name = "Pathing Test", group = "Testing")
class PathingTest : BaseOpMode() {
    enum class State {
        INIT,
        RUNNING,
        DONE,
    }

    private var state: State = State.INIT

    override fun onInit() {
        bot = Bot.fromType(BotType.MECANUM_BOT, hardwareMap)
        telemetryPacket.put("Target Velocity", 0.0)
        telemetryPacket.put("Current Velocity", 0.0)
        DashboardPlotter.scale = 2.5
    }

    override fun onLoop() {
        when (state) {
            State.INIT -> {
                bot.pinpoint!!.reset(Pose(10.0, 10.0, theta = 0.1))
                Thread.sleep(500)
                bot.follower.path =
                    HermitePath
                        .Builder()
                        .addPoint(Pose(0.0, 0.0, theta = 0.0), Pose(100.0, 0.0))
                        .addPoint(Pose(50.0, 100.0, theta = 0.0), Pose(100.0, 0.0))
                        .build()
                bot.follower.start()
                state = State.RUNNING
            }
            State.RUNNING -> {
                if (bot.follower.done) state = State.DONE
                // Telemetry updates
                telemetryPacket.put("Target Velocity", bot.follower.targetState!!.v)
                telemetryPacket.put("Current Velocity", hypot(bot.pinpoint!!.pose.vx, bot.pinpoint!!.pose.vy))
                // Field view
                DashboardPlotter.plotGrid(telemetryPacket)
                DashboardPlotter.plotBotPosition(telemetryPacket, bot.pinpoint!!.pose)
                DashboardPlotter.plotPath(telemetryPacket, bot.follower.path!!)
                DashboardPlotter.plotPoint(
                    telemetryPacket,
                    bot.follower.path!!.getPoint(bot.follower.targetState!!.x / bot.follower.path!!.getLength()),
                )
            }
            State.DONE -> {
                requestOpModeStop()
            }
        }
    }
}

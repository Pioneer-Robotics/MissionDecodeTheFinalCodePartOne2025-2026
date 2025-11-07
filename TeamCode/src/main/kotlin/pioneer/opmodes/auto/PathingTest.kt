package pioneer.opmodes.auto

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import pioneer.BotType
import pioneer.helpers.DashboardPlotter
import pioneer.helpers.Pose
import pioneer.opmodes.BaseOpMode
import pioneer.pathing.paths.HermitePath
import kotlin.math.hypot

@Autonomous(name = "Pathing Test", group = "Testing")
class PathingTest : BaseOpMode(BotType.MECANUM_BOT) {
    enum class State {
        INIT,
        RUNNING,
        DONE,
    }

    private var state: State = State.INIT

    override fun onInit() {
        telemetryPacket.put("Target Velocity", 0.0)
        telemetryPacket.put("Current Velocity", 0.0)
        DashboardPlotter.scale = 2.5
    }

    override fun onLoop() {
        when (state) {
            State.INIT -> {
                bot.follower.path = HermitePath.Builder()
                    .addPoint(Pose(0.0, 0.0), Pose(100.0, 0.0))
                    .addPoint(Pose(50.0, 100.0), Pose(100.0, 0.0))
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

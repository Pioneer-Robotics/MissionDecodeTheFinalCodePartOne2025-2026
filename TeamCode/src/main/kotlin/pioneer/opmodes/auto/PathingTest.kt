package pioneer.opmodes.auto

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import pioneer.Bot
import pioneer.helpers.DashboardPlotter
import pioneer.pathing.paths.HermitePath
import pioneer.localization.Pose

@Autonomous(name = "Pathing Test", group = "Testing")
class PathingTest : OpMode() {
    enum class State {
        INIT,
        RUNNING,
        DONE
    }

    private var state: State = State.INIT

    override fun init() {
        Bot.initialize(hardwareMap, telemetry)

        Bot.telemetryPacket.put("Target Velocity", 0.0)
        Bot.telemetryPacket.put("Current Velocity", 0.0)
        Bot.sendTelemetryPacket()
    }

    override fun loop() {
        Bot.update()
        when (state) {
            State.INIT -> {
                Bot.follower.path = HermitePath.Builder()
                    .addPoint(Pose(0.0, 0.0), Pose(50.0, 0.0))
                    .addPoint(Pose(50.0, 100.0), Pose(50.0, 0.0))
                    .build()
                Bot.follower.start()
                state = State.RUNNING
            }
            State.RUNNING -> {
                if (Bot.follower.done) state = State.DONE
                // Telemetry updates
                Bot.telemetryPacket.put("Target Velocity", Bot.follower.targetState!!.v)
                Bot.telemetryPacket.put("Current Velocity", Bot.localizer.velocity.getLength())
                // Field view
                DashboardPlotter.plotGrid(Bot.telemetryPacket)
                DashboardPlotter.plotBotPosition(Bot.telemetryPacket, Bot.localizer.pose)
                DashboardPlotter.plotPath(Bot.telemetryPacket, Bot.follower.path!!)
                DashboardPlotter.plotPoint(Bot.telemetryPacket, Bot.follower.path!!.getPoint(Bot.follower.targetState!!.x / Bot.follower.path!!.getLength()))
                Bot.sendTelemetryPacket()
            }
            State.DONE -> {
                requestOpModeStop()
            }
        }
    }

    override fun stop() {
        Bot.stop()
    }
}
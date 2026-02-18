package pioneer.opmodes.other

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import pioneer.Bot
import pioneer.BotType
import pioneer.decode.Points
import pioneer.general.AllianceColor
import pioneer.helpers.DashboardPlotter
import pioneer.helpers.Pose
import pioneer.opmodes.BaseOpMode
import pioneer.pathing.motionprofile.constraints.VelocityConstraint
import pioneer.pathing.paths.HermitePath
import pioneer.pathing.paths.LinearPath
import kotlin.math.PI
import kotlin.math.hypot

//@Disabled
@Autonomous(name = "Pathing Test", group = "Testing")
class PathingTest : BaseOpMode() {
    enum class State {
        INIT,
        RUNNING,
        DONE,
    }

    private var state: State = State.INIT
    private var P = Points(AllianceColor.RED)

    override fun onInit() {
        bot = Bot.fromType(BotType.COMP_BOT, hardwareMap)
        telemetryPacket.put("Target Velocity", 0.0)
        telemetryPacket.put("Current Velocity", 0.0)
        DashboardPlotter.scale = 2.5
    }

    override fun onStart() {
        bot.pinpoint?.reset(P.START_FAR)
        bot.intake?.forward()
        bot.follower.reset()
    }

    override fun onLoop() {
        when (state) {
            State.INIT -> {
                bot.follower.followPath(
                    HermitePath.Builder()
                        .addPoint(bot.pinpoint!!.pose, Pose(0.0, 100.0))
                        .addPoint(Pose(100.0,-110.0, theta = -6.0 * PI / 7.0))
                        .addPoint(Pose(148.0, -150.0, theta = - 6.0 * PI / 7.0), Pose(-50.0,-300.0))
                        .build()
                        .apply {
                            velocityConstraint = VelocityConstraint {
                                s -> if (s > this.getLength() - 40.0) 15.0 else Double.MAX_VALUE
                            }
                        }
                )
                state = State.RUNNING
            }
            State.RUNNING -> {
                if (bot.follower.done) state = State.DONE
                // Telemetry updates
                telemetryPacket.put("Target Velocity", bot.follower.targetState!!.v)
                telemetryPacket.put("Current Velocity", hypot(bot.pinpoint!!.pose.vx, bot.pinpoint!!.pose.vy))
                telemetryPacket.put("Pose", bot.pinpoint!!.pose)
                // Field view
                DashboardPlotter.plotGrid(telemetryPacket)
                DashboardPlotter.plotBotPosition(telemetryPacket, bot.pinpoint!!.pose)
                DashboardPlotter.plotPath(telemetryPacket, bot.follower.currentPath!!)
                DashboardPlotter.plotPoint(
                    telemetryPacket,
                    bot.follower.currentPath!!.getPoint(bot.follower.targetState!!.x / bot.follower.currentPath!!.getLength()),
                )
            }
            State.DONE -> {
                requestOpModeStop()
            }
        }
    }
}

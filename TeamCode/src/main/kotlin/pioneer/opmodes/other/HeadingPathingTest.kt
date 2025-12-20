package pioneer.opmodes.other

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import pioneer.Bot
import pioneer.hardware.MecanumBase
import pioneer.helpers.Pose
import pioneer.localization.localizers.Pinpoint
import pioneer.opmodes.BaseOpMode
import pioneer.pathing.paths.LinearPath
import kotlin.math.PI

//@Disabled
@Autonomous(name = "Heading Pathing Test", group = "Testing")
class HeadingPathingTest : BaseOpMode() {
    enum class State {
        INIT,
        FORWARD,
        BACKWARD,
        STOP,
    }

    var state = State.INIT

    override fun onInit() {
        bot =
            Bot
                .Builder()
                .add(MecanumBase(hardwareMap))
                .add(Pinpoint(hardwareMap))
                .build()
    }

    override fun start() {
        bot.pinpoint!!.reset(Pose(50.0, 0.0, theta=PI/2))
    }

    override fun onLoop() {
        when (state) {
            State.INIT -> state_init()
            State.FORWARD -> state_forward()
            State.BACKWARD -> state_backward()
            State.STOP -> state_stop()
        }
        telemetry.addData("State", state)
    }

    private fun state_init() {
        bot.follower.followPath(
            LinearPath(
                bot.pinpoint!!.pose,
                Pose(x = 0.0, y = 100.0, theta = 3.14),
            )
        )
        state = State.FORWARD
    }

    private fun state_forward() {
        if (bot.follower.done) {
            bot.follower.followPath(
                LinearPath(
                    bot.pinpoint!!.pose,
                    Pose(x = 0.0, y = 0.0, theta = 0.0),
                )
            )
            state = State.BACKWARD
        }
    }

    private fun state_backward() {
        if (bot.follower.done) {
            state = State.STOP
        }
    }

    private fun state_stop() {
        requestOpModeStop()
    }
}

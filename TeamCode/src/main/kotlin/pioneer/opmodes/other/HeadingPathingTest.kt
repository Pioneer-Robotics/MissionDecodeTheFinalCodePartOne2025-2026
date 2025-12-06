package pioneer.opmodes.auto

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import pioneer.Bot
import pioneer.hardware.MecanumBase
import pioneer.helpers.Pose
import pioneer.localization.localizers.Pinpoint
import pioneer.opmodes.BaseOpMode
import pioneer.pathing.paths.LinearPath

@Disabled
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
        bot.pinpoint!!.reset(Pose(0.0, 0.0))
        bot.follower.path =
            LinearPath(
                Pose(x = 0.0, y = 0.0, theta = 0.0),
                Pose(x = 0.0, y = 100.0, theta = 3.14),
            )
        bot.follower.start()
        state = State.FORWARD
    }

    private fun state_forward() {
        if (bot.follower.done) {
            bot.follower.path =
                LinearPath(
                    bot.pinpoint!!.pose,
                    Pose(x = 0.0, y = 0.0, theta = 0.0),
                )
            bot.follower.start()
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

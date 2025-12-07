package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import pioneer.Bot
import pioneer.hardware.MecanumBase
import pioneer.helpers.Pose
import pioneer.localization.localizers.Pinpoint
import pioneer.opmodes.BaseOpMode
import pioneer.pathing.paths.LinearPath

@Disabled
@Autonomous(name = "Theta PID Tuner", group = "Calibration")
class ThetaPidTuner : BaseOpMode() {
    enum class State {
        INIT,
        CLOCK,
        COUNTER,
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
            State.CLOCK -> state_clock()
            State.COUNTER -> state_counter()
            State.STOP -> state_stop()
        }
        telemetry.addData("State", state)
    }

    private fun state_init() {
        bot.follower.path = LinearPath(Pose(), Pose(theta = -3.14))
        bot.follower.start()
        state = State.CLOCK
    }

    private fun state_clock() {
        if (bot.follower.done) {
            bot.follower.path = LinearPath(bot.pinpoint!!.pose, Pose(theta = 0.0))
            bot.follower.start()
            state = State.COUNTER
        }
    }

    private fun state_counter() {
        if (bot.follower.done) {
            bot.follower.path = LinearPath(bot.pinpoint!!.pose, Pose(theta = -3.14))
            bot.follower.start()
            state = State.COUNTER
        }
    }

    private fun state_stop() {
        requestOpModeStop()
    }
}

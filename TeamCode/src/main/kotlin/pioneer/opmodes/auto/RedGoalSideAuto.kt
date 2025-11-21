package pioneer.opmodes.auto

import pioneer.Bot
import pioneer.BotType
import pioneer.hardware.MecanumBase
import pioneer.helpers.Pose
import pioneer.localization.localizers.Pinpoint
import pioneer.opmodes.BaseOpMode
import pioneer.pathing.paths.LinearPath
import pioneer.decode.Points
import pioneer.general.AllianceColor

class RedGoalSideAuto : BaseOpMode() {


    val P = Points(AllianceColor.RED)
    /* ----------------
       -    ENUMS     -
       ---------------- */

    // Main state for auto
    enum class State {
        INIT,
        GOTO_SHOOT,
        SHOOT,
        GOTO_COLLECT,
        COLLECT,
        STOP
    }

    // State for which line of artifacts to collect
    enum class CollectState {
        GOAL,
        MID,
        AUDIENCE,
    }

    var state = State.INIT
    var collectState = CollectState.GOAL

    /* ------------------
       - MAIN FUNCTIONS -
       ------------------ */

    override fun onInit() {
        // TODO: Change when we get new bot
        // TODO: Move pinpoint reset into a state function
        bot = Bot.builder()
            .add(MecanumBase(hardwareMap))
            .add(Pinpoint(hardwareMap))
            .build()
        bot.pinpoint!!.reset(Pose(130.0,137.0))
    }

    override fun onLoop() {
        when (state) {
            State.INIT->state_init()
            State.GOTO_SHOOT -> state_goto_shoot()
            State.SHOOT -> state_shoot()
            State.GOTO_COLLECT -> state_goto_collect()
            State.COLLECT -> state_collect()
            State.STOP -> state_stop()
        }

        telemetry.addData("State", state)
        telemetry.addData("Collect State", collectState)
        telemetry.addData("Pose", bot.pinpoint!!.pose.toString())
    }

    override fun onStop() {}


    /* -------------------
       - STATE FUNCTIONS -
       ------------------- */

    private fun state_init() {
        bot.follower.path = LinearPath(bot.pinpoint!!.pose, Pose(60.0, 60.0))
        bot.follower.start()
        state = State.GOTO_SHOOT
    }

    private fun state_goto_shoot() {
        if (bot.follower.done) {
            state = State.SHOOT
        }
    }

    private fun state_shoot() { 
        // TODO: Shoot
        if (true) {
            when (collectState) {
                CollectState.GOAL -> bot.follower.path = LinearPath(bot.pinpoint!!.pose, Pose(70.0, 30.0))
                CollectState.MID -> bot.follower.path = LinearPath(bot.pinpoint!!.pose, Pose(70.0, -30.0))
                CollectState.AUDIENCE -> bot.follower.path = LinearPath(bot.pinpoint!!.pose, Pose(70.0, -90.0))
            }
            bot.follower.start()
            state = State.GOTO_COLLECT
        }
    }

    private fun state_goto_collect() {
        if (bot.follower.done) {
            // TODO: add speed scalar to motion profile
            when (collectState) {
                CollectState.GOAL -> bot.follower.path = LinearPath(bot.pinpoint!!.pose, Pose(130.0, 30.0))
                CollectState.MID -> bot.follower.path = LinearPath(bot.pinpoint!!.pose, Pose(130.0, -30.0))
                CollectState.AUDIENCE -> bot.follower.path = LinearPath(bot.pinpoint!!.pose, Pose(130.0, -90.0))
            }
            bot.follower.start()
            state = State.COLLECT
        }
    }

    private fun state_collect() {
        // TODO: collect
        if (bot.follower.done) {
            state = State.INIT
            when (collectState) {
                CollectState.GOAL -> collectState = CollectState.MID
                CollectState.MID -> collectState = CollectState.AUDIENCE
                CollectState.AUDIENCE -> state = State.STOP
            }
        }
    }

    private fun state_stop() {
        requestOpModeStop()
    }
}

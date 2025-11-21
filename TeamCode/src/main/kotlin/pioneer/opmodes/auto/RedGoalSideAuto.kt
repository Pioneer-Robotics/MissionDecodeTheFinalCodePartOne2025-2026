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

    var state = State.GOTO_SHOOT
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
        bot.pinpoint!!.reset(P.START_GOAL)
    }

    override fun onLoop() {
        when (state) {
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

    private fun state_goto_shoot() {
        if (bot.follower.path == null) { // Starting path
            bot.follower.path = LinearPath(bot.pinpoint!!.pose, P.SHOOT_GOAL_CLOSE)
            bot.follower.start()
        }
        if (bot.follower.done) { // Ending path
            state = State.SHOOT
            bot.follower.path = null
        }
    }

    private fun state_shoot() { 
        // TODO: Shoot
        if (true) {
            state = State.GOTO_COLLECT
        }
    }

    private fun state_goto_collect() {
        if (bot.follower.path == null) { // Starting path
            when (collectState) {
                CollectState.GOAL -> bot.follower.path = LinearPath(bot.pinpoint!!.pose, P.PREP_COLLECT_GOAL)
                CollectState.MID -> bot.follower.path = LinearPath(bot.pinpoint!!.pose, P.PREP_COLLECT_MID)
                CollectState.AUDIENCE -> bot.follower.path = LinearPath(bot.pinpoint!!.pose, P.PREP_COLLECT_AUDIENCE)
            }
            bot.follower.start()
        }

        if (bot.follower.done) { // Ending path
            // TODO: add speed scalar to motion profile
            bot.follower.path = null
            state = State.COLLECT
        }
    }

    private fun state_collect() {
        // TODO: collect
        if (bot.follower.path == null) { // Starting path
            when (collectState) {
                CollectState.GOAL -> bot.follower.path = LinearPath(bot.pinpoint!!.pose, P.COLLECT_GOAL)
                CollectState.MID -> bot.follower.path = LinearPath(bot.pinpoint!!.pose, P.COLLECT_MID)
                CollectState.AUDIENCE -> bot.follower.path = LinearPath(bot.pinpoint!!.pose, P.COLLECT_AUDIENCE)
            }
            bot.follower.start()
        }

        if (bot.follower.done) { // Ending path
            state = State.GOTO_SHOOT
            bot.follower.path = null
            when (collectState) {
                CollectState.GOAL -> collectState = CollectState.MID
                CollectState.MID -> collectState = CollectState.AUDIENCE
                // TODO: Move so that audience artifacts are shot
                CollectState.AUDIENCE -> state = State.STOP
            }
        }
    }

    private fun state_stop() {
        requestOpModeStop()
    }
}

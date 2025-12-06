package pioneer.opmodes.auto

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import pioneer.Bot
import pioneer.BotType
import pioneer.decode.Artifact
import pioneer.decode.Points
import pioneer.opmodes.BaseOpMode
import pioneer.pathing.paths.LinearPath

@Autonomous(name = "Goal Side Auto", group = "Autonomous")
class RedGoalSideAuto : BaseOpMode() {
    val P = Points(bot.allianceColor)
    /* ----------------
       -    ENUMS     -
       ---------------- */

    // Allows you to do some or all of the auto
    enum class AutoOptions {
        PRELOAD_ONLY,
        FIRST_ROW,
        SECOND_ROW,
        ALL,
    }

    // Main state for auto
    enum class State {
        INIT,
        GOTO_SHOOT,
        SHOOT,
        GOTO_COLLECT,
        COLLECT,
        STOP,
    }

    // State for which line of artifacts to collect
    enum class CollectState {
        GOAL,
        MID,
        AUDIENCE,
        DONE,
    }

    enum class LaunchState {
        READY,
        MOVING_TO_POSITION,
        LAUNCHING,
    }

    var autoType = AutoOptions.PRELOAD_ONLY
    var state = State.INIT
    var collectState = CollectState.GOAL
    var launchState = LaunchState.READY

    /* ------------------
       - MAIN FUNCTIONS -
       ------------------ */

    override fun onInit() {
        bot = Bot.fromType(BotType.COMP_BOT, hardwareMap)
    }

    override fun onLoop() {
        when (state) {
            State.INIT -> {
                bot.follower.path = null
                bot.pinpoint!!.reset(P.START_GOAL.copy(theta = 0.1))
                bot.spindexer?.setArtifacts(Artifact.PURPLE, Artifact.PURPLE, Artifact.PURPLE)
                state = State.GOTO_SHOOT
            }
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
        bot.flywheel?.velocity = 800.0
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
        handle_shoot_all()
        if (bot.spindexer?.isEmpty == true) {
            state = State.GOTO_COLLECT

            // Breakpoint for the different auto options
            when (autoType) {
                AutoOptions.PRELOAD_ONLY -> {
                    if (collectState == CollectState.GOAL) {
                        state = State.STOP
                    }
                }
                AutoOptions.FIRST_ROW -> {
                    if (collectState == CollectState.MID) {
                        state = State.STOP
                    }
                }
                AutoOptions.SECOND_ROW -> {
                    if (collectState == CollectState.AUDIENCE) {
                        state = State.STOP
                    }
                }
                AutoOptions.ALL -> {}
            }
        }
    }

    private fun handle_shoot_all() {
        when (launchState) {
            LaunchState.READY -> {
                bot.spindexer?.moveToNextOuttake()
                launchState = LaunchState.MOVING_TO_POSITION
            }

            LaunchState.MOVING_TO_POSITION -> {
                if (bot.spindexer?.reachedTarget == true) {
                    bot.launcher?.triggerLaunch()
                }
                launchState = LaunchState.LAUNCHING
            }

            LaunchState.LAUNCHING -> {
                if (bot.launcher?.isReset == true) {
                    launchState = LaunchState.READY
                }
            }
        }
    }

    private fun state_goto_collect() {
        if (bot.follower.path == null) { // Starting path
            when (collectState) {
                CollectState.GOAL -> bot.follower.path = LinearPath(bot.pinpoint!!.pose, P.PREP_COLLECT_GOAL)
                CollectState.MID -> bot.follower.path = LinearPath(bot.pinpoint!!.pose, P.PREP_COLLECT_MID)
                CollectState.AUDIENCE -> bot.follower.path = LinearPath(bot.pinpoint!!.pose, P.PREP_COLLECT_AUDIENCE)
                CollectState.DONE -> state = State.STOP
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
        bot.intake?.forward()
        if (bot.follower.path == null) { // Starting path
            when (collectState) {
                CollectState.GOAL -> bot.follower.path = LinearPath(bot.pinpoint!!.pose, P.COLLECT_GOAL)
                CollectState.MID -> bot.follower.path = LinearPath(bot.pinpoint!!.pose, P.COLLECT_MID)
                CollectState.AUDIENCE -> bot.follower.path = LinearPath(bot.pinpoint!!.pose, P.COLLECT_AUDIENCE)
                CollectState.DONE -> {}
            }
            bot.follower.start()
        }

        if (bot.follower.done) { // Ending path
            state = State.GOTO_SHOOT
            bot.intake?.stop()
            bot.follower.path = null
            when (collectState) {
                CollectState.GOAL -> collectState = CollectState.MID
                CollectState.MID -> collectState = CollectState.AUDIENCE
                CollectState.AUDIENCE -> collectState = CollectState.DONE
                CollectState.DONE -> {}
            }
        }
    }

    private fun state_stop() {
        requestOpModeStop()
    }
}

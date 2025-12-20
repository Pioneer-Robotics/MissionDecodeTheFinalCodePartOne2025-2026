package pioneer.opmodes.auto

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import pioneer.Bot
import pioneer.BotType
import pioneer.decode.Artifact
import pioneer.decode.GoalTag
import pioneer.decode.Points
import pioneer.general.AllianceColor
import pioneer.opmodes.BaseOpMode
import pioneer.pathing.paths.LinearPath

@Autonomous(name = "Goal Side Auto", group = "Autonomous")
class GoalSideAuto : BaseOpMode() {
    enum class AutoOptions {
        PRELOAD_ONLY,
        FIRST_ROW,
        SECOND_ROW,
        ALL,
    }

    // Main state for auto
    enum class State {
        GOTO_SHOOT,
        SHOOT,
        GOTO_COLLECT,
        COLLECT,
        STOP,
    }

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

    private lateinit var P: Points
    private var autoType = AutoOptions.FIRST_ROW
    private var state = State.GOTO_SHOOT
    private var collectState = CollectState.GOAL
    private var launchState = LaunchState.READY

    override fun onInit() {
        bot =
            Bot.fromType(BotType.COMP_BOT, hardwareMap)
        P = Points(bot.allianceColor)
    }

    override fun start() {
        bot.apply{
            pinpoint?.reset(P.START_GOAL)
            spindexer?.setArtifacts(Artifact.GREEN, Artifact.PURPLE, Artifact.PURPLE)
            follower.reset()
        }
    }

    override fun onLoop() {
        when (state) {
            State.GOTO_SHOOT -> state_goto_shoot()
            State.SHOOT -> state_shoot()
            State.GOTO_COLLECT -> state_goto_collect()
            State.COLLECT -> state_collect()
            State.STOP -> state_stop()
        }

        val target = if (bot.allianceColor == AllianceColor.RED) GoalTag.RED.shootingPose else GoalTag.BLUE.shootingPose
        bot.turret?.autoTrack(bot.pinpoint!!.pose, target)

        telemetry.addData("State", state)
        telemetry.addData("Collect State", collectState)
        telemetry.addData("Pose", bot.pinpoint!!.pose.toString())
    }

    private fun state_goto_shoot() {
        bot.flywheel?.velocity = 780.0
        if (!bot.follower.isFollowing) { // Starting path
            bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.SHOOT_GOAL_CLOSE))
        }
        if (bot.follower.done) { // Ending path
            state = State.SHOOT
        }
    }

    private fun state_shoot() {
        handle_shoot_all()
        if (bot.spindexer?.isEmpty == true) {
            bot.spindexer!!.moveManual(0.0)
            state = State.GOTO_COLLECT

            // Breakpoint for the different auto options
            when (autoType) {
                AutoOptions.PRELOAD_ONLY -> {
                    if (collectState == CollectState.GOAL) {
                        bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.LEAVE_POSITION))
                        state = State.STOP
                    }
                }
                AutoOptions.FIRST_ROW -> {
                    if (collectState == CollectState.MID) {
                        bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.LEAVE_POSITION))
                        state = State.STOP
                    }
                }
                AutoOptions.SECOND_ROW -> {
                    if (collectState == CollectState.AUDIENCE) {
                        bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.LEAVE_POSITION))
                        state = State.STOP
                    }
                }
                AutoOptions.ALL -> {}
            }
        }
    }

    private fun handle_shoot_all() {
        // FIXME: UNTESTED IN AUTO
        if (bot.flywheel!!.velocity > 790.0) {
            val slowSpeed = 0.075 + 0.005 * bot.spindexer!!.numStoredArtifacts
            val speed = 0.125 + 0.005 * bot.spindexer!!.numStoredArtifacts
            bot.spindexer?.moveManual(if (bot.spindexer!!.reachedOuttakePosition) slowSpeed else speed)
            val closestMotorPosition = bot.spindexer!!.closestMotorState.ordinal / 2
            val hasArtifact = bot.spindexer!!.artifacts[closestMotorPosition] != null
            if (bot.spindexer!!.reachedOuttakePosition && hasArtifact) {
                bot.launcher!!.triggerLaunch()
                bot.spindexer!!.popCurrentArtifact()
            }
        }
    }

    private fun state_goto_collect() {
        if (!bot.follower.isFollowing) { // Starting path
            when (collectState) {
                CollectState.GOAL -> bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.PREP_COLLECT_GOAL))
                CollectState.MID -> bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.PREP_COLLECT_MID))
                CollectState.AUDIENCE -> bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.PREP_COLLECT_AUDIENCE))
                CollectState.DONE -> state = State.STOP
            }
        }

        if (bot.follower.done) { // Ending path
            // TODO: add speed scalar to motion profile
            state = State.COLLECT
        }
    }

    private fun state_collect() {
        bot.intake?.forward()
        bot.flywheel?.velocity = 0.0
        if (!bot.follower.isFollowing) { // Starting path
            when (collectState) {
                CollectState.GOAL -> bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.COLLECT_GOAL), 5.0)
                CollectState.MID -> bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.COLLECT_MID), 5.0)
                CollectState.AUDIENCE -> bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.COLLECT_AUDIENCE), 5.0)
                CollectState.DONE -> {}
            }
        }

        if (bot.follower.done) { // Ending path
            state = State.GOTO_SHOOT
            bot.intake?.stop()
            bot.flywheel?.velocity = 780.0
            when (collectState) {
                CollectState.GOAL -> collectState = CollectState.MID
                CollectState.MID -> collectState = CollectState.AUDIENCE
                CollectState.AUDIENCE -> collectState = CollectState.DONE
                CollectState.DONE -> {}
            }
        }
    }

    private fun state_stop() {
        if (bot.follower.done) {
            requestOpModeStop()
        }
    }
}

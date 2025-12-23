package pioneer.opmodes.auto

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import pioneer.Bot
import pioneer.BotType
import pioneer.decode.Artifact
import pioneer.decode.GoalTag
import pioneer.decode.Motif
import pioneer.decode.Obelisk
import pioneer.decode.Points
import pioneer.general.AllianceColor
import pioneer.helpers.Toggle
import pioneer.helpers.next
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

    // Not used in current implementation
    enum class LaunchState {
        READY,
        MOVING_TO_POSITION,
        LAUNCHING,
    }

    private val allianceToggle = Toggle(false)
    private lateinit var P: Points
    private var autoType = AutoOptions.ALL
    private var state = State.GOTO_SHOOT
    private var collectState = CollectState.GOAL
    private var launchState = LaunchState.READY
    private var targetVelocity = 0.0
    // Motif logic variables
    private var motifOrder: Motif = Motif(21)
    private var lookForTag = true

    override fun onInit() {
        bot =
            Bot.fromType(BotType.COMP_BOT, hardwareMap)
    }

    override fun init_loop() {
        allianceToggle.toggle(gamepad1.touchpad)
        if (allianceToggle.justChanged) {
            bot.allianceColor = bot.allianceColor.next()
        }

        telemetry.apply {
            addData("Alliance Color", bot.allianceColor)
            update()
        }
    }

    override fun start() {
        P = Points(bot.allianceColor)
        bot.apply{
            pinpoint?.reset(P.START_GOAL)
            spindexer?.setArtifacts(Artifact.GREEN, Artifact.PURPLE, Artifact.PURPLE)
            spindexer?.moveToNextOuttake(motifOrder.currentArtifact)
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

        val targetGoal = if (bot.allianceColor == AllianceColor.RED) GoalTag.RED else GoalTag.BLUE
        targetVelocity = bot.flywheel!!.estimateVelocity(targetGoal.shootingPose, bot.pinpoint!!.pose, targetGoal.shootingHeight)
        bot.turret?.autoTrack(bot.pinpoint!!.pose, targetGoal.shootingPose)

        telemetry.addData("Next Artifact", motifOrder.currentArtifact)
        telemetry.addData("Detected Motif", motifOrder.toString())
        telemetry.addData("Looking for Motif", lookForTag)
        telemetry.addData("State", state)
        telemetry.addData("Collect State", collectState)
        telemetry.addData("Pose", bot.pinpoint!!.pose.toString())
    }

    private fun state_goto_shoot() {
        bot.flywheel?.velocity = targetVelocity
        if (!bot.follower.isFollowing) { // Starting path
            bot.spindexer?.moveToNextOuttake(motifOrder.currentArtifact)
            val endPose = if (lookForTag) P.SHOOT_GOAL_CLOSE.copy(theta=0.0) else P.SHOOT_GOAL_CLOSE
            bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, endPose))
        }
        if (lookForTag) {
            bot.camera?.getProcessor<AprilTagProcessor>()?.detections?.let { detections ->
                Obelisk.detectMotif(detections, bot.allianceColor)?.let { detectedMotif ->
                    motifOrder = detectedMotif
                    lookForTag = false // Detected the motif
                    bot.spindexer?.moveToNextOuttake(motifOrder.currentArtifact)
                }
            }
        }
        if (bot.follower.done) { // Ending path
            state = State.SHOOT
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
        when (launchState) {
            LaunchState.READY -> {
                launchState = LaunchState.MOVING_TO_POSITION
                bot.spindexer?.moveToNextOuttake(motifOrder.currentArtifact)
            }

            LaunchState.MOVING_TO_POSITION -> {
                if (bot.spindexer?.reachedTarget == true) {
                    bot.launcher?.triggerLaunch()
                    bot.spindexer?.popArtifact(bot.spindexer!!.closestMotorState.ordinal / 2)
                    motifOrder.getNextArtifact() // Cycle to next artifact
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
        if (!bot.follower.isFollowing) { // Starting path
            bot.spindexer!!.moveToNextOpenIntake()
            when (collectState) {
                CollectState.GOAL -> bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.PREP_COLLECT_GOAL))
                CollectState.MID -> bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.PREP_COLLECT_MID))
                CollectState.AUDIENCE -> bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.PREP_COLLECT_AUDIENCE))
                CollectState.DONE -> state = State.STOP
            }
        }

        if (bot.follower.done) { // Ending path
            state = State.COLLECT
        }
    }

    private fun state_collect() {
        bot.intake?.forward()
        bot.flywheel?.velocity = 0.0
        if (!bot.follower.isFollowing) { // Starting path
            when (collectState) {
                CollectState.GOAL -> bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.COLLECT_GOAL), 4.0)
                CollectState.MID -> bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.COLLECT_MID), 4.0)
                CollectState.AUDIENCE -> bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.COLLECT_AUDIENCE), 4.0)
                CollectState.DONE -> {}
            }
        }

        if (bot.follower.done) { // Ending path
            state = State.GOTO_SHOOT
            bot.intake?.stop()
            bot.flywheel?.velocity = targetVelocity
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

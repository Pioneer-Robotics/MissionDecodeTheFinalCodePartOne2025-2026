package pioneer.opmodes.auto

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import pioneer.Bot
import pioneer.BotType
import pioneer.Constants
import pioneer.decode.Artifact
import pioneer.decode.GoalTag
import pioneer.decode.Motif
import pioneer.decode.Obelisk
import pioneer.decode.Points
import pioneer.general.AllianceColor
import pioneer.hardware.prism.Color
import pioneer.helpers.Toggle
import pioneer.helpers.next
import pioneer.opmodes.BaseOpMode
import pioneer.pathing.paths.LinearPath
import java.util.Timer
import java.util.TimerTask

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

    private val allianceToggle = Toggle(false)
    private val cameraTimer = ElapsedTime()
    private lateinit var P: Points
    private lateinit var targetGoal: GoalTag
    private var autoType = AutoOptions.ALL
    private var state = State.GOTO_SHOOT
    private var collectState = CollectState.GOAL
    private var launchState = LaunchState.READY
    private var targetVelocity = 815.0
    // Motif logic variables
    private var motifOrder: Motif = Motif(21)
    private var lookForTag: Boolean? = null

    override fun onInit() {
        Constants.TransferData.reset()
        bot = Bot.fromType(BotType.COMP_BOT, hardwareMap)
    }

    override fun init_loop() {
        allianceToggle.toggle(gamepad1.touchpad)
        if (allianceToggle.justChanged) {
            bot.allianceColor = bot.allianceColor.next()
            bot.led?.setColor(
                when(bot.allianceColor) {
                    AllianceColor.RED -> Color.RED
                    AllianceColor.BLUE -> Color.BLUE
                    AllianceColor.NEUTRAL -> Color.PURPLE
                }
            )
        }

        bot.camera?.getProcessor<AprilTagProcessor>()?.detections?.let { detections ->
            Obelisk.detectMotif(detections, bot.allianceColor)?.let { detectedMotif ->
                motifOrder = detectedMotif.nextMotif()!!
                lookForTag = false // Detected the motif
            }
        }

        telemetry.apply {
            addData("Alliance Color", bot.allianceColor)
            addData("Detected Motif", motifOrder.toString())
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
        targetGoal = if (bot.allianceColor == AllianceColor.RED) GoalTag.RED else GoalTag.BLUE
        cameraTimer.reset()
    }

    override fun onLoop() {
        when (state) {
            State.GOTO_SHOOT -> state_goto_shoot()
            State.SHOOT -> state_shoot()
            State.GOTO_COLLECT -> state_goto_collect()
            State.COLLECT -> state_collect()
            State.STOP -> state_stop()
        }

//        targetVelocity = bot.flywheel!!.estimateVelocity(bot.pinpoint!!.pose, targetGoal.shootingPose, targetGoal.shootingHeight)
        bot.turret?.autoTrack(bot.pinpoint!!.pose, targetGoal.shootingPose)

        telemetry.addData("Pose", bot.pinpoint!!.pose.toString())
        telemetry.addData("Follower Done", bot.follower.done)
        telemetry.addData("Next Artifact", motifOrder.currentArtifact)
        telemetry.addData("Detected Motif", motifOrder.toString())
        telemetry.addData("Artifacts", bot.spindexer?.artifacts.contentDeepToString())
        telemetry.addData("Looking for Motif", lookForTag)
        telemetry.addData("State", state)
        telemetry.addData("Collect State", collectState)
        telemetry.addData("Launch State", launchState)
    }

    private fun state_goto_shoot() {
        bot.flywheel?.velocity = targetVelocity
        if (!bot.follower.isFollowing) { // Starting path
            bot.spindexer?.moveToNextOuttake(motifOrder.currentArtifact)
            val endPose = if (lookForTag == null) P.SHOOT_GOAL_CLOSE.copy(theta=0.0) else P.SHOOT_GOAL_CLOSE
//            FileLogger.debug("GoalSideAuto", "Set Target Pose: $endPose")
            bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, endPose))
        }
        if (cameraTimer.seconds() > 2.5 && lookForTag == null) lookForTag = true
        if (lookForTag == true) {
            bot.camera?.getProcessor<AprilTagProcessor>()?.detections?.let { detections ->
                Obelisk.detectMotif(detections, bot.allianceColor)?.let { detectedMotif ->
                    motifOrder = detectedMotif
                    lookForTag = false // Detected the motif
                    bot.spindexer?.moveToNextOuttake(motifOrder.currentArtifact)
                }
            }
        }
        if (bot.follower.done) { // Ending path
            bot.follower.reset()
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
                        bot.follower.reset()
                        bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.LEAVE_POSITION))
                        state = State.STOP
                    }
                }
                AutoOptions.FIRST_ROW -> {
                    if (collectState == CollectState.MID) {
                        bot.follower.reset()
                        bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.LEAVE_POSITION))
                        state = State.STOP
                    }
                }
                AutoOptions.SECOND_ROW -> {
                    if (collectState == CollectState.AUDIENCE) {
                        bot.follower.reset()
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
                bot.spindexer?.moveToNextOuttake(motifOrder.currentArtifact)
                launchState = LaunchState.MOVING_TO_POSITION
            }

            LaunchState.MOVING_TO_POSITION -> {
                if (bot.spindexer?.reachedTarget == true) {
                    bot.launcher?.triggerLaunch()
                    launchState = LaunchState.LAUNCHING
                }
            }

            LaunchState.LAUNCHING -> {
                if (bot.launcher?.isReset == true) {
                    bot.spindexer?.popCurrentArtifact()
                    motifOrder.getNextArtifact() // Cycle to next artifact
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
            bot.follower.reset()
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

        if (bot.follower.done || bot.spindexer?.isFull == true) { // Ending path
            bot.follower.reset()
            state = State.GOTO_SHOOT
            Timer().schedule(object : TimerTask() {
                override fun run() {
                    bot.intake?.stop()
                }
            }, 1000)
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
            bot.follower.reset()
            requestOpModeStop()
        }
    }
}

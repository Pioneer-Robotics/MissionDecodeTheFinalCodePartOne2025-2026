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
import pioneer.helpers.Pose
import pioneer.helpers.Toggle
import pioneer.helpers.next
import pioneer.opmodes.BaseOpMode
import pioneer.pathing.paths.HermitePath
import pioneer.pathing.paths.LinearPath
import java.util.Timer
import java.util.TimerTask

@Autonomous(name = "Audience Side Auto Middle", group = "Autonomous")
class AudienceSideAutoMiddle : BaseOpMode() {
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
        LEAVE,
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
    private lateinit var P: Points
    private lateinit var targetGoal: GoalTag
    private var startLeave = true
    private var autoType = AutoOptions.ALL
    private var state = State.GOTO_SHOOT
    private var collectState = CollectState.MID
    private var launchState = LaunchState.READY
    private var targetVelocity = 980.0
    // Motif logic variables
    private var motifOrder: Motif = Motif(21)
    private var lookForTag = true
    private val tagTimer = ElapsedTime()
    private val tagTimeout = 3.0
    private var firstShot = true

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
                motifOrder = detectedMotif
            }
        }

        telemetry.apply {
            addData("Alliance Color", bot.allianceColor)
            addData("Detected Motif", motifOrder.toString())
            update()
        }
    }

    override fun onStart() {
        P = Points(bot.allianceColor)
        bot.apply{
            pinpoint?.reset(P.START_FAR)
            spindexer?.setArtifacts(Artifact.GREEN, Artifact.PURPLE, Artifact.PURPLE)
            spindexer?.moveToNextOuttake(motifOrder.currentArtifact)
            follower.reset()
        }
        targetGoal = if (bot.allianceColor == AllianceColor.RED) GoalTag.RED else GoalTag.BLUE
        tagTimer.reset()
    }

    override fun onLoop() {
        when (state) {
            State.GOTO_SHOOT -> state_goto_shoot()
            State.SHOOT -> state_shoot()
            State.GOTO_COLLECT -> state_goto_collect()
            State.COLLECT -> state_collect()
            State.LEAVE -> state_leave()
            State.STOP -> state_stop()
        }

        checkForTimeUp()

//        targetVelocity = bot.flywheel!!.estimateVelocity(bot.pinpoint!!.pose, targetGoal.shootingPose, targetGoal.shootingHeight)
        handleTurret()

        telemetry.addData("Pose", bot.pinpoint!!.pose.toString())
        telemetry.addData("Follower Done", bot.follower.done)
        telemetry.addData("Flywheel Speed", bot.flywheel?.velocity)
        telemetry.addData("Next Artifact", motifOrder.currentArtifact)
        telemetry.addData("Detected Motif", motifOrder.toString())
        telemetry.addData("Artifacts", bot.spindexer?.artifacts.contentDeepToString())
        telemetry.addData("State", state)
        telemetry.addData("Collect State", collectState)
        telemetry.addData("Launch State", launchState)
    }

    private fun handleTurret() {
        if (lookForTag && tagTimer.seconds() < tagTimeout) {
            bot.turret?.autoTrack(bot.pinpoint!!.pose, Pose(0.0, 200.0))
            bot.camera?.getProcessor<AprilTagProcessor>()?.detections?.let { detections ->
                Obelisk.detectMotif(detections, bot.allianceColor)?.let { detectedMotif ->
                    motifOrder = detectedMotif
                    lookForTag = false // Detected the motif
                }
            }
        } else {
            bot.turret?.autoTrack(bot.pinpoint!!.pose, targetGoal.shootingPose)
        }
    }

    private fun checkForTimeUp() {
        if ((30.0 - elapsedTime) < 1.5) {
            state = State.LEAVE
        }
    }

    private fun state_goto_shoot() {
        bot.flywheel?.velocity = targetVelocity
        if (!bot.follower.isFollowing) { // Starting path
            bot.spindexer?.moveToNextOuttake(motifOrder.currentArtifact)
            if (firstShot) {
                bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.SHOOT_GOAL_FAR), 150.0)
                firstShot = false
            } else {
                bot.follower.followPath(
                    HermitePath(
                        bot.pinpoint!!.pose,
                        P.SHOOT_GOAL_FAR,
                        P.GOTO_SHOOT_VELOCITY,
                        Pose(0.0,0.0)
                    ),
                    150.0
                )
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
                if (bot.spindexer?.reachedTarget == true && flywheelAtSpeed()) {
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

    private fun flywheelAtSpeed(): Boolean {
        return (bot.flywheel?.velocity ?: 0.0) > (targetVelocity - 10) &&
                (bot.flywheel?.velocity ?: 0.0) < (targetVelocity + 10)
    }

    private fun state_goto_collect() {
        if (!bot.follower.isFollowing) { // Starting path
            bot.spindexer!!.moveToNextOpenIntake()
            when (collectState) {
                CollectState.GOAL -> bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.PREP_COLLECT_GOAL), 175.0)
                CollectState.MID -> bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.PREP_COLLECT_MID), 175.0)
                CollectState.AUDIENCE -> bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.PREP_COLLECT_AUDIENCE), 175.0)
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
        if (!bot.follower.isFollowing) { // Starting path
            when (collectState) {
                CollectState.GOAL -> bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.COLLECT_GOAL), 10.0)
                CollectState.MID -> bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.COLLECT_MID), 10.0)
                CollectState.AUDIENCE -> bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.COLLECT_AUDIENCE), 10.0)
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
            collectState = CollectState.DONE
        }
    }

    private fun state_leave() {
        if (startLeave) {
            bot.flywheel?.velocity = 0.0
            bot.intake?.stop()
            bot.follower.followPath(LinearPath(bot.pinpoint?.pose ?: Pose(), P.LEAVE_POSITION))
            startLeave = false
        }
        if (bot.follower.done) {
            bot.follower.reset()
        }
    }

    private fun state_stop() {
        if (bot.follower.done) {
            bot.follower.reset()
            requestOpModeStop()
        }
    }
}

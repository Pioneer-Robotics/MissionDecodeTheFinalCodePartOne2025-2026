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
import pioneer.helpers.OdometryLogger
import pioneer.helpers.Pose
import pioneer.helpers.Toggle
import pioneer.helpers.next
import pioneer.opmodes.BaseOpMode
import pioneer.pathing.paths.HermitePath
import pioneer.pathing.paths.LinearPath
import java.util.Timer
import java.util.TimerTask

@Autonomous(name = "Goal Side Auto (Logging)", group = "Autonomous")
class GoalSideAuto_WithLogging : BaseOpMode() {
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
    private var startLeave = true
    
    // DRIFT LOGGING - Add this
    private lateinit var odometryLogger: OdometryLogger

    override fun onInit() {
        Constants.TransferData.reset()
        bot = Bot.fromType(BotType.COMP_BOT, hardwareMap)
        
        // DRIFT LOGGING - Initialize the logger
        odometryLogger = OdometryLogger(bot, "GoalSideAuto")
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
                motifOrder = if(bot.allianceColor == AllianceColor.RED)
                    detectedMotif.prevMotif()!! else detectedMotif.nextMotif()!!
                lookForTag = false // Detected the motif
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
            pinpoint?.reset(P.START_GOAL)
            spindexer?.setArtifacts(Artifact.GREEN, Artifact.PURPLE, Artifact.PURPLE)
            spindexer?.moveToNextOuttake(motifOrder.currentArtifact)
            follower.reset()
        }
        targetGoal = if (bot.allianceColor == AllianceColor.RED) GoalTag.RED else GoalTag.BLUE
        cameraTimer.reset()
        
        // DRIFT LOGGING - Start logging when autonomous starts
        odometryLogger.start()
        odometryLogger.logCheckpoint("AUTO_START", P.START_GOAL)
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

        bot.turret?.autoTrack(bot.pinpoint!!.pose, targetGoal.shootingPose)
        
        // DRIFT LOGGING - Log every loop with current state
        odometryLogger.log(
            state = state.name,
            subState = "${collectState.name}_${launchState.name}",
            targetPose = null // Follower will provide target automatically
        )

        telemetry.addData("Pose", bot.pinpoint!!.pose.toString())
        telemetry.addData("Follower Done", bot.follower.done)
        telemetry.addData("Next Artifact", motifOrder.currentArtifact)
        telemetry.addData("Detected Motif", motifOrder.toString())
        telemetry.addData("Artifacts", bot.spindexer?.artifacts.contentDeepToString())
        telemetry.addData("Looking for Motif", lookForTag)
        telemetry.addData("State", state)
        telemetry.addData("Collect State", collectState)
        telemetry.addData("Launch State", launchState)
        
        // DRIFT LOGGING - Add stats to telemetry
        telemetry.addData("Logging", odometryLogger.getStats())
    }
    
    override fun onStop() {
        // DRIFT LOGGING - Stop logging and save file
        odometryLogger.stop()
        super.onStop() // This calls FileLogger.flush()
    }

    private fun checkForTimeUp() {
        if ((30.0 - elapsedTime) < 1.5) {
            state = State.LEAVE
        }
    }

    private fun state_goto_shoot() {
        bot.flywheel?.velocity = targetVelocity
        if (!bot.follower.isFollowing) { // Starting path
            // DRIFT LOGGING - Log checkpoint at start of path
            odometryLogger.logCheckpoint("START_PATH_TO_SHOOT")
            
            bot.spindexer?.moveToNextOuttake(motifOrder.currentArtifact)
            val endPose = if (lookForTag == null) P.SHOOT_GOAL_CLOSE.copy(theta=0.0) else P.SHOOT_GOAL_CLOSE
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
            // DRIFT LOGGING - Log checkpoint at end of path
            odometryLogger.logCheckpoint("END_PATH_TO_SHOOT", P.SHOOT_GOAL_CLOSE)
            
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

    private fun state_goto_collect() {
        if (!bot.follower.isFollowing) { // Starting path
            // DRIFT LOGGING - Log checkpoint at start of collection path
            odometryLogger.logCheckpoint("START_PATH_TO_COLLECT_${collectState.name}")
            
            bot.spindexer!!.moveToNextOpenIntake()
            when (collectState) {
                CollectState.GOAL -> bot.follower.followPath(
                    HermitePath(bot.pinpoint!!.pose, P.PREP_COLLECT_GOAL,
                        P.PREP_COLLECT_START_VELOCITY, P.PREP_COLLECT_END_VELOCITY)
                )
                CollectState.MID -> bot.follower.followPath(
                    HermitePath(bot.pinpoint!!.pose, P.PREP_COLLECT_MID,
                        P.PREP_COLLECT_START_VELOCITY, P.PREP_COLLECT_END_VELOCITY)
                )
                CollectState.AUDIENCE -> bot.follower.followPath(
                    HermitePath(bot.pinpoint!!.pose, P.PREP_COLLECT_AUDIENCE,
                        P.PREP_COLLECT_START_VELOCITY, P.PREP_COLLECT_END_VELOCITY)
                )
                CollectState.DONE -> state = State.STOP
            }
        }

        if (bot.follower.done) { // Ending path
            // DRIFT LOGGING - Log checkpoint at prep collection position
            val prepPose = when (collectState) {
                CollectState.GOAL -> P.PREP_COLLECT_GOAL
                CollectState.MID -> P.PREP_COLLECT_MID
                CollectState.AUDIENCE -> P.PREP_COLLECT_AUDIENCE
                CollectState.DONE -> Pose()
            }
            odometryLogger.logCheckpoint("END_PATH_PREP_COLLECT_${collectState.name}", prepPose)
            
            bot.follower.reset()
            state = State.COLLECT
        }
    }

    private fun flywheelAtSpeed(): Boolean {
        return (bot.flywheel?.velocity ?: 0.0) > (targetVelocity - 10) &&
                (bot.flywheel?.velocity ?: 0.0) < (targetVelocity + 10)
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
            // DRIFT LOGGING - Log checkpoint at collection complete
            val collectPose = when (collectState) {
                CollectState.GOAL -> P.COLLECT_GOAL
                CollectState.MID -> P.COLLECT_MID
                CollectState.AUDIENCE -> P.COLLECT_AUDIENCE
                CollectState.DONE -> Pose()
            }
            odometryLogger.logCheckpoint("COLLECTION_COMPLETE_${collectState.name}", collectPose)
            
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

    private fun state_leave() {
        if (startLeave) {
            // DRIFT LOGGING - Log checkpoint leaving
            odometryLogger.logCheckpoint("LEAVING_TO_PARK")
            
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
            // DRIFT LOGGING - Log final checkpoint
            odometryLogger.logCheckpoint("AUTO_COMPLETE", P.LEAVE_POSITION)
            
            bot.follower.reset()
            requestOpModeStop()
        }
    }
}

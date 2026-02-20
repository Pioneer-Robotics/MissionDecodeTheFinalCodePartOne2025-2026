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
import org.firstinspires.ftc.teamcode.prism.Color
import pioneer.helpers.Pose
import pioneer.helpers.Toggle
import pioneer.helpers.next
import pioneer.opmodes.BaseOpMode
import pioneer.pathing.paths.HermitePath
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
        //        MOVING_TO_POSITION,
//        LAUNCHING,
        SORTING,
        SHOOTING
    }

    private val allianceToggle = Toggle(false)
    private lateinit var P: Points
    private lateinit var targetGoal: GoalTag
    private var autoType = AutoOptions.ALL
    private var state = State.GOTO_SHOOT
    private var collectState = CollectState.GOAL
    private var launchState = LaunchState.READY
    private var targetVelocity = 800.0
    // Motif logic variables
    private var motifOrder: Motif = Motif(21)
    private var lookForTag = true
    private var startLeave = true
    private var firstShoot = true
    private val tagTimer = ElapsedTime()
    private val tagTimeout = 3.0
    private val shootTimer = ElapsedTime()
    private val minShotTime = 1.25

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
                },
                0,
                23
            )
        }

//        bot.camera?.getProcessor<AprilTagProcessor>()?.detections?.let { detections ->
//            Obelisk.detectMotif(detections, bot.allianceColor)?.let { detectedMotif ->
//                motifOrder = if(bot.allianceColor == AllianceColor.RED)
//                    detectedMotif.prevMotif()!! else detectedMotif.nextMotif()!!
//                lookForTag = false // Detected the motif
//            }
//        }

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
            follower.reset()
        }
        targetGoal = if (bot.allianceColor == AllianceColor.RED) GoalTag.RED else GoalTag.BLUE
        tagTimer.reset()

        // Constantly run intake to keep balls in spindexer
        bot.intake?.forward()
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

//        checkForTimeUp()

        targetVelocity = bot.flywheel!!.estimateVelocity(bot.pinpoint!!.pose, targetGoal.shootingPose, targetGoal.shootingHeight)

        handleTurret()
//        handleFlywheel()

        telemetry.addData("Pose", bot.pinpoint!!.pose.toString())
        telemetry.addData("Follower Done", bot.follower.done)
        telemetry.addData("Next Artifact", motifOrder.currentArtifact)
        telemetry.addData("Detected Motif", motifOrder.toString())
        telemetry.addData("Artifacts", bot.spindexer?.artifacts.contentDeepToString())
        telemetry.addData("Looking for Motif", lookForTag)
        telemetry.addData("State", state)
        telemetry.addData("Collect State", collectState)
        telemetry.addData("Launch State", launchState)

        telemetryPacket.put("Target Flywheel Speed", bot.flywheel?.targetVelocity)
        telemetryPacket.put("Actual Flywheel Speed", bot.flywheel?.velocity)
    }

    private fun handleTurret() {
        if (lookForTag && tagTimer.seconds() < tagTimeout && tagTimer.seconds() > 1.0) {
            bot.turret?.autoTrack(bot.pinpoint!!.pose, Pose(0.0, 200.0))
            bot.camera?.getProcessor<AprilTagProcessor>()?.detections?.let { detections ->
                Obelisk.detectMotif(detections, bot.allianceColor)?.let { detectedMotif ->
                    motifOrder = detectedMotif
                    lookForTag = false // Detected the motif
                    bot.spindexer?.readyOuttake(motifOrder)
                }
            }
        } else {
            bot.turret?.autoTrack(bot.pinpoint!!.pose, targetGoal.shootingPose)
        }
    }

    private fun handleFlywheel() {
        val distance = bot.pinpoint?.pose?.distanceTo(targetGoal.pose)
        targetVelocity = bot.flywheel!!.estimateVelocity(
            distance!!,
            targetGoal.shootingHeight
        )
        bot.flywheel?.velocity = targetVelocity
    }

    private fun checkForTimeUp() {
        if ((30.0 - elapsedTime) < 1.5) {
            state = State.LEAVE
        }
    }

    private fun state_goto_shoot() {
        bot.flywheel?.velocity = targetVelocity
        if (!bot.follower.isFollowing) { // Starting path
            bot.spindexer?.readyOuttake(motifOrder)
            firstShoot = false
            val endPose = if (30.0 - elapsedTime < 10.0) P.SHOOT_CLOSE_LEAVE else P.SHOOT_CLOSE
            bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, endPose))
        }
        if (bot.follower.done) { // Ending path
            bot.follower.reset()
            shootTimer.reset()
            state = State.SHOOT
        }
    }

    private fun state_shoot() {
//        handle_shoot_all()

        if (shootTimer.seconds() > minShotTime && flywheelAtSpeed()) {
            bot.spindexer?.shootNext()
            shootTimer.reset()
        }
        // Check if the spindexer is empty and the last shot has cleared
        if (bot.spindexer?.isEmpty == true && bot.spindexer?.reachedTarget == true) {
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

//    private fun handle_shoot_all() {
//        // Shoot all stored balls
//        // Add a minimum delay and check that flywheel is at speed
//        if (shootTimer.seconds() > minShotTime && flywheelAtSpeed()) {
//            bot.spindexer?.shootNext()
//            shootTimer.reset()
//        }
//    }

    private fun state_goto_collect() {
        if (!bot.follower.isFollowing) { // Starting path
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
            bot.follower.reset()
            state = State.COLLECT
        }
    }

    private fun flywheelAtSpeed(): Boolean {
        return (bot.flywheel?.velocity ?: 0.0) > (targetVelocity - 50) &&
                (bot.flywheel?.velocity ?: 0.0) < (targetVelocity + 50)
    }

    private fun state_collect() {
//        bot.flywheel?.velocity = 0.0
        if (!bot.follower.isFollowing) { // Starting path
            when (collectState) {
                CollectState.GOAL -> bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.COLLECT_GOAL), 8.0)
                CollectState.MID -> bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.COLLECT_MID), 8.0)
                CollectState.AUDIENCE -> bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.COLLECT_AUDIENCE), 8.0)
                CollectState.DONE -> {}
            }
        }

        if (bot.follower.done || bot.spindexer?.isFull == true) { // Ending path
            bot.follower.reset()
            state = State.GOTO_SHOOT
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
            bot.flywheel?.velocity = 0.0
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
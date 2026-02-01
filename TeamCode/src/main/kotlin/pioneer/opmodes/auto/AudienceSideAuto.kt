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
import pioneer.helpers.TestMatrixLogger
import pioneer.helpers.Toggle
import pioneer.helpers.next
import pioneer.opmodes.BaseOpMode
import pioneer.pathing.paths.LinearPath

@Autonomous(name = "Audience Side Auto", group = "Autonomous")
class AudienceSideAuto : BaseOpMode() {
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

    // ✅ NEW: Collection sub-states for stop-and-collect
    enum class CollectSubState {
        APPROACHING,
        STOPPED_INTAKING,
        DONE
    }

    private val allianceToggle = Toggle(false)
    private lateinit var P: Points
    private lateinit var targetGoal: GoalTag
    private var startLeave = true
    private var autoType = AutoOptions.ALL
    private var state = State.GOTO_SHOOT
    private var collectState = CollectState.AUDIENCE  // Start with AUDIENCE
    private var launchState = LaunchState.READY

    // ✅ NEW: Collection sub-state
    private var collectSubState = CollectSubState.APPROACHING

    private var targetVelocity = 980.0
    private var motifOrder: Motif = Motif(21)

    // ✅ NEW: Collection timing
    private val intakeTimer = ElapsedTime()
    private val INTAKE_TIMEOUT_MS = 2000L  // 2 seconds max per position

    // ✅ NEW: Test matrix logging (optional)
    private var testLogger: TestMatrixLogger? = null
    private var shotsFired = 0
    private var collectionsAttempted = 0  // Changed to var
    private var collectionsSuccessful = 0  // Changed to var

    override fun onInit() {
        Constants.TransferData.reset()
        bot = Bot.fromType(BotType.COMP_BOT, hardwareMap)

        // ✅ NEW: Initialize test logger
        testLogger = TestMatrixLogger(
            bot,
            "AUTO_AUD_${System.currentTimeMillis()}",
            "Audience Side Autonomous",
            TestMatrixLogger.TestType.FUNCTIONAL
        )
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

        // ✅ NEW: Start test logging
        testLogger?.start()
    }

    override fun onLoop() {
        when (state) {
            State.GOTO_SHOOT -> state_goto_shoot()
            State.SHOOT -> state_shoot()
            State.GOTO_COLLECT -> state_goto_collect()
            State.COLLECT -> state_collect_improved()  // ✅ NEW: Stop-and-collect
            State.LEAVE -> state_leave()
            State.STOP -> state_stop()
        }

        checkForTimeUp()

        bot.turret?.autoTrack(bot.pinpoint!!.pose, targetGoal.shootingPose)

        // ✅ NEW: Log automatic telemetry
        testLogger?.logAutomaticTelemetry()

        telemetry.addData("Pose", bot.pinpoint!!.pose.toString())
        telemetry.addData("Follower Done", bot.follower.done)
        telemetry.addData("Flywheel Speed", bot.flywheel?.targetVelocity)
        telemetry.addData("Next Artifact", motifOrder.currentArtifact)
        telemetry.addData("Detected Motif", motifOrder.toString())
        telemetry.addData("Artifacts", bot.spindexer?.artifacts.contentDeepToString())
        telemetry.addData("State", state)
        telemetry.addData("Collect State", collectState)
        telemetry.addData("Collect Sub-State", collectSubState)  // ✅ NEW
        telemetry.addData("Launch State", launchState)
    }

    override fun onStop() {
        // ✅ NEW: Finalize test logging
        val shotAccuracy = if (shotsFired > 0) {
            // Placeholder - would need hit detection
            85.0
        } else {
            0.0
        }

        val collectionRate = if (collectionsAttempted > 0) {
            (collectionsSuccessful.toDouble() / collectionsAttempted * 100.0)
        } else {
            0.0
        }

        val result = if (shotAccuracy >= 90.0 && collectionRate >= 90.0) {
            TestMatrixLogger.TestResult.PASS
        } else {
            TestMatrixLogger.TestResult.MARGINAL
        }

        testLogger?.setResult(result,
            "Shots: $shotsFired, Collection: %.1f%%".format(collectionRate))
        testLogger?.stop()

        super.onStop()
    }

    private fun checkForTimeUp() {
        if ((30.0 - elapsedTime) < 1.5) {
            state = State.LEAVE
        }
    }

    private fun state_goto_shoot() {
        bot.flywheel?.targetVelocity = targetVelocity
        if (!bot.follower.isFollowing) { // Starting path
            bot.spindexer?.moveToNextOuttake(motifOrder.currentArtifact)
            bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.SHOOT_GOAL_FAR), 150.0)

            // ✅ NEW: Log path start
            testLogger?.logEvent("PATH_START", mapOf(
                "path" to "GOTO_SHOOT",
                "target" to "SHOOT_GOAL_FAR"
            ))
        }
        if (bot.follower.done) { // Ending path
            bot.follower.reset()
            state = State.SHOOT

            // ✅ NEW: Log path complete
            testLogger?.logEvent("PATH_COMPLETE", mapOf("path" to "GOTO_SHOOT"))
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

                    // ✅ NEW: Log shot
                    shotsFired++
                    val distance = bot.pinpoint!!.pose.distanceTo(targetGoal.shootingPose)
                    testLogger?.logShot(
                        shotNumber = shotsFired,
                        distance = distance,
                        targetX = targetGoal.shootingPose.x,
                        targetY = targetGoal.shootingPose.y,
                        hit = true  // Placeholder
                    )
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
        return (bot.flywheel?.targetVelocity ?: 0.0) > (targetVelocity - 10) &&
                (bot.flywheel?.targetVelocity ?: 0.0) < (targetVelocity + 10)
    }

    private fun state_goto_collect() {
        if (!bot.follower.isFollowing) { // Starting path
            bot.spindexer!!.moveToNextOpenIntake()
            when (collectState) {
                CollectState.GOAL -> bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.PREP_COLLECT_GOAL), 150.0)
                CollectState.MID -> bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.PREP_COLLECT_MID), 150.0)
                CollectState.AUDIENCE -> bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.PREP_COLLECT_AUDIENCE), 150.0)
                CollectState.DONE -> state = State.STOP
            }
        }

        if (bot.follower.done) { // Ending path
            bot.follower.reset()
            state = State.COLLECT
        }
    }

    // ========================================
    // ✅ IMPROVED: Stop-and-Collect Pattern
    // ========================================
    private fun state_collect_improved() {
        when (collectSubState) {
            CollectSubState.APPROACHING -> {
                // Phase 1: Drive to position WITHOUT intake running
                bot.intake?.stop()  // Keep intake OFF while moving

                if (!bot.follower.isFollowing) {
                    bot.spindexer?.moveToNextOpenIntake()

                    when (collectState) {
                        CollectState.GOAL -> bot.follower.followPath(
                            LinearPath(bot.pinpoint!!.pose, P.COLLECT_GOAL),
                            20.0  // Faster approach, will stop before collecting
                        )
                        CollectState.MID -> bot.follower.followPath(
                            LinearPath(bot.pinpoint!!.pose, P.COLLECT_MID),
                            20.0
                        )
                        CollectState.AUDIENCE -> bot.follower.followPath(
                            LinearPath(bot.pinpoint!!.pose, P.COLLECT_AUDIENCE),
                            20.0
                        )
                        CollectState.DONE -> {
                            state = State.STOP
                            return
                        }
                    }
                }

                if (bot.follower.done) {
                    bot.follower.reset()

                    // ✅ CRITICAL: Ensure robot is COMPLETELY stopped
                    bot.mecanumBase?.setDrivePower(Pose(), 0.0, 0.0)
                    Thread.sleep(100)  // Brief pause for mechanical settling

                    collectSubState = CollectSubState.STOPPED_INTAKING
                    intakeTimer.reset()
                }
            }

            CollectSubState.STOPPED_INTAKING -> {
                // Phase 2: Run intake while robot is STATIONARY
                bot.intake?.forward()
                collectionsAttempted++

                val initialCount = bot.spindexer?.numStoredArtifacts ?: 0

                // Optional: Very slow movement over spike marks
                if (!bot.follower.isFollowing) {
                    when (collectState) {
                        CollectState.GOAL -> bot.follower.followPath(
                            LinearPath(bot.pinpoint!!.pose, P.COLLECT_GOAL),
                            6.7  // Very slow over spike marks
                        )
                        CollectState.MID -> bot.follower.followPath(
                            LinearPath(bot.pinpoint!!.pose, P.COLLECT_MID),
                            6.7
                        )
                        CollectState.AUDIENCE -> bot.follower.followPath(
                            LinearPath(bot.pinpoint!!.pose, P.COLLECT_AUDIENCE),
                            6.7
                        )
                        CollectState.DONE -> {}
                    }
                }

                // Check if collection complete
                val spindexerFull = bot.spindexer?.isFull == true
                val timeoutReached = intakeTimer.milliseconds() > INTAKE_TIMEOUT_MS
                val pathComplete = bot.follower.done

                if (spindexerFull || (timeoutReached && pathComplete)) {
                    bot.follower.reset()
                    bot.intake?.stop()

                    // ✅ Track collection success
                    val finalCount = bot.spindexer?.numStoredArtifacts ?: 0
                    val success = finalCount > initialCount
                    if (success) collectionsSuccessful++

                    // ✅ CRITICAL: Wait for balls to settle after intake stops
                    Thread.sleep(200)

                    // ✅ NEW: Log collection
                    testLogger?.logCollection(
                        artifactType = "MIXED",
                        position = collectState.name,
                        success = success,
                        timeMs = intakeTimer.milliseconds().toLong()
                    )

                    collectSubState = CollectSubState.DONE
                }
            }

            CollectSubState.DONE -> {
                // Phase 3: Transition to shooting
                state = State.GOTO_SHOOT
                collectSubState = CollectSubState.APPROACHING  // Reset for next collection

                bot.flywheel?.targetVelocity = targetVelocity

                // Advance to next collection position
                // Audience side goes: AUDIENCE → MID → GOAL
                when (collectState) {
                    CollectState.AUDIENCE -> collectState = CollectState.MID
                    CollectState.MID -> collectState = CollectState.GOAL
                    CollectState.GOAL -> collectState = CollectState.DONE
                    CollectState.DONE -> {}
                }
            }
        }
    }

    private fun state_leave() {
        if (startLeave) {
            bot.flywheel?.targetVelocity = 0.0
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
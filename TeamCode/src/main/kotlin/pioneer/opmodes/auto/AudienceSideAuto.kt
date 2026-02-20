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
import pioneer.general.AllianceColor
import org.firstinspires.ftc.teamcode.prism.Color
import pioneer.decode.Points
import pioneer.helpers.Pose
import pioneer.helpers.next
import pioneer.opmodes.BaseOpMode
import pioneer.pathing.paths.LinearPath
import kotlin.math.hypot

@Autonomous(name = "Audience Side Auto", group = "Autonomous")
class AudienceSideAuto : BaseOpMode() {
    // Main state for auto
    enum class State {
        GOTO_SHOOT,
        SHOOT,
        COLLECT,
        LEAVE,
    }

    // Collects in the order specified by the enum
    enum class CollectState {
        AUDIENCE,
//        MID,
        HUMAN_PLAYER,
        DONE,
    }

    private lateinit var P: Points
    private lateinit var targetGoal: GoalTag

    private var state = State.GOTO_SHOOT
    private var collectState = CollectState.entries.first()
    private var shouldStartLeave = false

    // Motif logic variables
    private var motifOrder: Motif = Motif(21)
    private var lookForTag = true
    private val tagTimer = ElapsedTime()
    private val tagTimeout = 3.0
    private val shootTimer = ElapsedTime()
    private val minShotTime = 2.0
    private val collectionTimer = ElapsedTime()

    override fun onInit() {
        Constants.TransferData.reset()
        bot = Bot.fromType(BotType.COMP_BOT, hardwareMap)
    }

    override fun init_loop() {
        if (gamepad1.touchpadWasPressed()) {
            bot.allianceColor = bot.allianceColor.next()
            bot.led?.setColor(
                when(bot.allianceColor) {
                    AllianceColor.RED -> Color.RED
                    AllianceColor.BLUE -> Color.BLUE
                    AllianceColor.NEUTRAL -> Color.PURPLE
                }
            )
        }

        telemetry.addData("Alliance Color", bot.allianceColor)
        telemetry.update()
    }

    override fun onStart() {
        P = Points(bot.allianceColor)
        bot.apply{
            pinpoint?.reset(P.START_FAR)
            spindexer?.setArtifacts(Artifact.GREEN, Artifact.PURPLE, Artifact.PURPLE)
            follower.reset()
        }
        targetGoal = if (bot.allianceColor == AllianceColor.RED) GoalTag.RED else GoalTag.BLUE
        tagTimer.reset()
        shootTimer.reset()

//        bot.flywheel?.velocity = bot.flywheel?.estimateVelocity(bot.pinpoint!!.pose, targetGoal.pose, targetGoal.height)
//            ?: 1700.0
        bot.flywheel?.velocity = 1650.0

        // Constantly run intake to keep balls in spindexer
        bot.intake?.power = -1.0
    }

    override fun onLoop() {
        when (state) {
            State.GOTO_SHOOT -> stateGotoShoot()
            State.SHOOT -> stateShoot()
            State.COLLECT -> stateCollect()
            State.LEAVE -> stateLeave()
        }

        if (30.0 - elapsedTime < 2.5) {
            shouldStartLeave = true
        }

        if (shouldStartLeave) {
            state = State.LEAVE
            return
        }

        checkForTimeUp()
        handleTurret()

        telemetry.addData("Pose", bot.pinpoint!!.pose.toString())
        telemetry.addData("Follower Done", bot.follower.done)
        telemetry.addData("Flywheel Speed", bot.flywheel?.velocity)
        telemetry.addData("Target Velocity", bot.flywheel?.targetVelocity)
        telemetry.addData("Next Artifact", motifOrder.currentArtifact)
        telemetry.addData("Detected Motif", motifOrder.toString())
        telemetry.addData("Artifacts", bot.spindexer?.artifacts.contentDeepToString())
        telemetry.addData("State", state)
        telemetry.addData("Collect State", collectState)

        telemetryPacket.put("Target Flywheel Speed", bot.flywheel?.targetVelocity)
        telemetryPacket.put("Actual Flywheel Speed", bot.flywheel?.velocity)
        telemetryPacket.put("Target Turret Position", bot.turret?.targetTicks)
        telemetryPacket.put("Actual Turret Position", bot.turret?.currentTicks)
    }

    private fun handleTurret() {
        if (tagTimer.seconds() > tagTimeout) {
            lookForTag = false
        }
        if (lookForTag) {
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
        if ((30.0 - elapsedTime) < 1.0) {
            state = State.LEAVE
        }
    }

    private fun stateGotoShoot() {
        if (!bot.follower.isFollowing) { // Starting path
            bot.intake?.power = -1.0
            bot.spindexer?.readyOuttake(motifOrder)
            bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.SHOOT_FAR))
        }
        if (bot.follower.done) { // Ending path
            bot.follower.reset()
            state = State.SHOOT
        }
    }

    private fun flywheelAtSpeed(): Boolean {
        val fw = bot.flywheel ?: return false
        val target = fw.targetVelocity
        val actual = fw.velocity
        return actual in (target - 10) .. (target + 10)
    }

    private fun stateShoot() {
        // Shoot all stored balls
        // Add a minimum delay and check that flywheel is at speed
        if (shootTimer.seconds() > minShotTime && flywheelAtSpeed()) {
            bot.spindexer?.shootNext()
            shootTimer.reset()
        }
        // Check if the spindexer is empty and the last shot has cleared
        if (bot.spindexer?.isEmpty == true && bot.spindexer?.reachedTarget == true) {
            collectionTimer.reset()
            state = State.COLLECT
        }
    }

    private fun stateCollect() {
        if (!bot.follower.isFollowing) { // Starting path
            bot.spindexer?.moveToNextOpenIntake()
            bot.intake?.forward()
            bot.intake?.forward()
            when (collectState) {
                CollectState.HUMAN_PLAYER ->
                    bot.follower.followPath(P.PATH_HUMAN_PLAYER(bot.pinpoint!!.pose))
                CollectState.AUDIENCE ->
                    bot.follower.followPath(P.PATH_COLLECT_AUDIENCE(bot.pinpoint!!.pose))
//                CollectState.MID ->
//                    bot.follower.followPath(P.PATH_COLLECT_MID(bot.pinpoint!!.pose))
                CollectState.DONE -> {
                    shouldStartLeave = true
                    return
                }
            }
        }

        // If collecting from the human player, make sure we return in time to shoot
        if (collectState == CollectState.HUMAN_PLAYER && collectionTimer.seconds() > 7.5) {
            bot.follower.reset()
            state = State.GOTO_SHOOT
        }

        if (bot.follower.done || bot.spindexer?.isFull == true) { // Ending path
            bot.follower.reset()
            collectState = collectState.next()
            state = State.GOTO_SHOOT
        }
    }

    private fun stateLeave() {
        if (shouldStartLeave) {
            bot.flywheel?.velocity = 0.0
            bot.intake?.stop()
            bot.follower.followPath(LinearPath(bot.pinpoint?.pose ?: Pose(), P.LEAVE_POSITION))
            shouldStartLeave = false
        }
        if (bot.follower.done) {
            bot.follower.reset()
        }
    }
}

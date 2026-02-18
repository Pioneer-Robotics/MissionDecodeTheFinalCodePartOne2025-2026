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
import pioneer.pathing.motionprofile.constraints.VelocityConstraint
import pioneer.pathing.paths.HermitePath
import pioneer.pathing.paths.LinearPath
import kotlin.math.PI

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
        HUMAN_PLAYER,
        AUDIENCE,
        DONE,
    }

    private lateinit var P: Points
    private lateinit var targetGoal: GoalTag

    private var state = State.GOTO_SHOOT
    private var collectState = CollectState.HUMAN_PLAYER
    private var shouldStartLeave = true
    private var startedShooting = true

    // Motif logic variables
    private var motifOrder: Motif = Motif(21)
    private var lookForTag = true
    private val tagTimer = ElapsedTime()
    private val tagTimeout = 3.0
    private val shotTimer = ElapsedTime()
    private val shotTime = 3.0

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
            spindexer?.readyOuttake(motifOrder)
            follower.reset()
        }
        targetGoal = if (bot.allianceColor == AllianceColor.RED) GoalTag.RED else GoalTag.BLUE
        tagTimer.reset()

        // Constantly run intake to keep balls in spindexer
        bot.intake?.forward()
    }

    override fun onLoop() {
        when (state) {
            State.GOTO_SHOOT -> stateGotoShoot()
            State.SHOOT -> stateShoot()
            State.COLLECT -> stateCollect()
            State.LEAVE -> stateLeave()
        }

        checkForTimeUp()
        handleTurret()

        bot.flywheel?.velocity = bot.flywheel?.estimateVelocity(bot.pinpoint!!.pose, targetGoal.pose, targetGoal.height) ?: 1750.0

        telemetry.addData("Pose", bot.pinpoint!!.pose.toString())
        telemetry.addData("Follower Done", bot.follower.done)
        telemetry.addData("Flywheel Speed", bot.flywheel?.velocity)
        telemetry.addData("Next Artifact", motifOrder.currentArtifact)
        telemetry.addData("Detected Motif", motifOrder.toString())
        telemetry.addData("Artifacts", bot.spindexer?.artifacts.contentDeepToString())
        telemetry.addData("State", state)
        telemetry.addData("Collect State", collectState)

        telemetryPacket.put("Target Flywheel Speed", bot.flywheel?.targetVelocity)
        telemetryPacket.put("Actual Flywheel Speed", bot.flywheel?.velocity)
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
            bot.spindexer?.readyOuttake(motifOrder)
            bot.follower.followPath(LinearPath(bot.pinpoint!!.pose, P.SHOOT_FAR))
        }
        if (bot.follower.done) { // Ending path
            bot.follower.reset()
            shotTimer.reset()
            state = State.SHOOT
        }
    }

    private fun flywheelAtSpeed(): Boolean {
        return (bot.flywheel?.velocity ?: 0.0) > (bot.flywheel!!.targetVelocity - 20) &&
                (bot.flywheel?.velocity ?: 0.0) < (bot.flywheel!!.targetVelocity + 20)
    }

    private fun stateShoot() {
        if (!flywheelAtSpeed()) return
        bot.spindexer?.shootAll()
        if (shotTimer.seconds() > shotTime) {
            state = State.COLLECT
        }
    }
    private fun stateCollect() {
        if (!bot.follower.isFollowing) { // Starting path
            when (collectState) {
                CollectState.HUMAN_PLAYER -> collectHumanPlayer()
                CollectState.AUDIENCE -> collectAudience()
                CollectState.DONE -> shouldStartLeave = true
            }
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

    private fun collectHumanPlayer() {
        bot.follower.followPath(
            HermitePath.Builder()
                .addPoint(bot.pinpoint!!.pose, Pose(0.0, 100.0))
                .addPoint(Pose(100.0,-110.0, theta = -6.0 * PI / 7.0))
                .addPoint(Pose(148.0, -150.0, theta = - 6.0 * PI / 7.0), Pose(-50.0,-300.0))
                .build()
                .apply {
                    velocityConstraint = VelocityConstraint {
                            s -> if (s > this.getLength() - 40.0) 15.0 else Double.MAX_VALUE
                    }
                }
        )
    }

    private fun collectAudience() {

    }
}

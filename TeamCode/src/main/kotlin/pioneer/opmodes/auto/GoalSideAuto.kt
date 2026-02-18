package pioneer.opmodes.auto

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import org.firstinspires.ftc.teamcode.prism.Color
import pioneer.Bot
import pioneer.BotType
import pioneer.Constants
import pioneer.decode.Artifact
import pioneer.decode.GoalTag
import pioneer.decode.Motif
import pioneer.decode.Points
import pioneer.general.AllianceColor
import pioneer.helpers.FileLogger
import pioneer.helpers.Pose
import pioneer.helpers.next
import pioneer.opmodes.BaseOpMode
import pioneer.pathing.motionprofile.constraints.VelocityConstraint
import pioneer.pathing.paths.HermitePath
import pioneer.pathing.paths.LinearPath

// TODO: Flywheel
// TODO: Camera

@Autonomous(name = "Goal Side Auto", group = "Auto")
class GoalSideAuto : BaseOpMode() {
    lateinit var P : Points
    lateinit var targetGoal: GoalTag

    enum class State {
        GOTO_SHOOT,
        SHOOT,
        COLLECT,
        LEAVE,
    }

    // Collects in the order specified by the enum
    enum class CollectState {
        GOAL,
        DONE,
    }

    var state = State.GOTO_SHOOT
    var collectState = CollectState.GOAL

    var motif = Motif(21)

    var stateJustChanged = false
    var prevState = state

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
                },
                0,
                23
            )
        }

        telemetry.addData("Alliance Color", bot.allianceColor)
        telemetry.update()
    }

    override fun onLoop() {
        when (state) {
            State.GOTO_SHOOT -> stateGoToShoot()
            State.SHOOT -> stateShoot()
            State.COLLECT -> stateCollect()
            State.LEAVE -> stateLeave()
        }

        // Handle state change logic
        stateJustChanged = state != prevState

        if (stateJustChanged) {
            FileLogger.debug("GoalSideAuto", "State changed from $prevState to $state")
        }

        prevState = state

        // Telemetry
        telemetry.addData("State", state)
        telemetry.addData("Collect State", collectState)
        telemetry.addData("Pose", bot.pinpoint!!.pose.toString())
        telemetry.addData("Artifacts", bot.spindexer?.artifacts.contentDeepToString())
    }

    override fun onStart() {
        P = Points(bot.allianceColor)
        bot.apply{
            pinpoint?.reset(P.START_GOAL)
            spindexer?.setArtifacts(Artifact.PURPLE, Artifact.GREEN, Artifact.PURPLE)
            follower.reset()
        }
        targetGoal = if (bot.allianceColor == AllianceColor.RED) GoalTag.RED else GoalTag.BLUE

        // Constantly run intake to keep balls in spindexer
        bot.intake?.forward()
    }

    private fun stateGoToShoot() {
        if (stateJustChanged) {
            bot.spindexer?.readyOuttake(motif)
            // Start path to shooting position
            bot.follower.followPath(
                LinearPath(
                    bot.pinpoint?.pose!!,
                    P.SHOOT_CLOSE,
                )
            )
        }
        if (bot.follower.done) {
            state = State.SHOOT
        }
    }

    private fun stateShoot() {
        if (stateJustChanged) {
            bot.spindexer?.shootAll()
        }
        if (bot.spindexer?.isShooting == false) {
            // Reset spindexer to intake mode for collecting new artifacts
            bot.spindexer?.moveToNextOpenIntake()
            bot.spindexer?.reset()
            state = State.COLLECT
        }
    }

    private fun stateCollect() {
        if (collectState == CollectState.DONE) {
            FileLogger.debug("GoalSideAuto", "Exit 2")
            state = State.LEAVE
            return
        }

        if (stateJustChanged) {
            // Start path to collect position
            val endPose: Pose; val startVelocity: Pose; val endVelocity: Pose
            when (collectState) {
                CollectState.GOAL -> {
                    endPose = P.COLLECT_GOAL_END
                    startVelocity = P.COLLECT_GOAL_START_VEL
                    endVelocity = P.COLLECT_GOAL_END_VEL
                }
                else -> {
                    // TODO: Add other collect positions
                    FileLogger.debug("GoalSideAuto", "Exit 1")
                    requestOpModeStop()
                    return
                }
            }
            bot.follower.followPath(
                HermitePath(
                    bot.pinpoint?.pose!!,
                    endPose,
                    startVelocity,
                    endVelocity,
                ).apply {
                    velocityConstraint = VelocityConstraint {
                        s -> if (s > this.getLength() - 75.0) 10.0 else Double.MAX_VALUE
                    }
                }
            )
        }
        if (bot.follower.done || bot.spindexer?.isFull == true) {
            FileLogger.debug("GoalSideAuto", "Follower done: ${bot.follower.done}")
            FileLogger.debug("GoalSideAuto", "Spindexer full: ${bot.spindexer?.isFull}")
            FileLogger.debug("GoalSideAuto", "Spindexer artifacts: ${bot.spindexer?.artifacts.contentDeepToString()}")
            collectState = collectState.next()
            state = State.GOTO_SHOOT
        }
    }

    private fun stateLeave() {
        requestOpModeStop() // TODO: Add leave path
    }
}
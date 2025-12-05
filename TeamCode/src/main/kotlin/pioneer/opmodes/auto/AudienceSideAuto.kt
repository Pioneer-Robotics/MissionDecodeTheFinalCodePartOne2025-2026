package pioneer.opmodes.auto

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import pioneer.Bot
import pioneer.BotType
import pioneer.decode.Artifact
import pioneer.decode.GoalTag
import pioneer.general.AllianceColor
import pioneer.helpers.Pose
import pioneer.helpers.Toggle
import pioneer.helpers.next
import pioneer.opmodes.BaseOpMode
import pioneer.pathing.paths.LinearPath

@Autonomous(name = "Audience Side Auto", group = "Autonomous")
class AudienceSideAuto : BaseOpMode() {
    enum class State {
        INIT,
        DRIVE,
        SHOOT,
    }

    enum class LaunchState {
        READY,
        MOVING_TO_POSITION,
        LAUNCHING,
    }

    var state = State.INIT
    var launchState = LaunchState.READY
    val allianceToggle = Toggle(false)

    override fun onInit() {
        bot = Bot.fromType(BotType.COMP_BOT, hardwareMap)
    }

    override fun init_loop() {
        allianceToggle.toggle(gamepad1.touchpad)
        if (allianceToggle.justChanged) {
            bot.allianceColor = bot.allianceColor.next()
        }
        telemetry.addData("Alliance Color", bot.allianceColor)
        telemetry.update()
    }

    override fun onLoop() {
        bot.turret?.autoTrack(
            bot.pinpoint?.pose ?: Pose(),
            if (bot.allianceColor == AllianceColor.BLUE) GoalTag.BLUE.pose else GoalTag.RED.pose + Pose(y = 60.0 / 2, x = 60.0), // TODO Use GoalTag shooting offset
        )
        when (state) {
            State.INIT -> {
                bot.pinpoint!!.reset(Pose(43.0, -157.0))
                bot.spindexer?.setArtifacts(Artifact.PURPLE, Artifact.PURPLE, Artifact.PURPLE)
                bot.follower.path = null
                state = State.DRIVE
            }
            State.DRIVE -> state_drive()
            State.SHOOT -> state_shoot()
        }
        telemetry.addData("Artifacts", bot.spindexer?.artifacts.contentDeepToString())
        telemetry.addData("State", state)
        telemetry.addData("Launch State", launchState)
        telemetry.addData("Flywheel Velocity", bot.flywheel?.velocity)
    }

    override fun onStop() {
        super.onStop()
    }

    fun state_drive() {
        if (bot.follower.path == null) {
            bot.flywheel?.velocity = 1000.0
            bot.follower.path = LinearPath(bot.pinpoint!!.pose, Pose(43.0, -147.0))
        }

        if (bot.follower.done) {
            state = State.SHOOT
        }
    }

    fun state_shoot() {
        bot.flywheel?.velocity?.let {
            if (it > 980.0) {
                when (launchState) {
                    LaunchState.READY -> {
                        bot.spindexer?.moveToNextOuttake()
                        launchState = LaunchState.MOVING_TO_POSITION
                    }

                    LaunchState.MOVING_TO_POSITION -> {
                        if (bot.spindexer?.reachedTarget == true) {
                            bot.launcher?.triggerLaunch()
                            bot.spindexer?.popCurrentArtifact()
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
        }
    }
}

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
import pioneer.hardware.Spindexer.MotorPosition
import pioneer.helpers.Pose
import pioneer.helpers.Toggle
import pioneer.helpers.next
import pioneer.opmodes.BaseOpMode
import pioneer.pathing.paths.LinearPath

@Autonomous(name = "Audience Side Auto", group = "Autonomous")
class AudienceSideAuto : BaseOpMode() {
    enum class State {
        INIT,
        DRIVE_1,
        SHOOT,
        DRIVE_2,
        STOP,
    }

    enum class LaunchState {
        READY,
        MOVING_TO_POSITION,
        LAUNCHING,
    }

    var state = State.INIT
    lateinit var P: Points
    var launchState = LaunchState.READY
    val allianceToggle = Toggle(false)
    var motifOrder: Motif = Motif(21)
    var motifIndex = 0

    override fun onInit() {
        bot = Bot.fromType(BotType.COMP_BOT, hardwareMap)
    }

    override fun init_loop() {
        bot.spindexer?.setArtifacts(Artifact.GREEN, Artifact.PURPLE, Artifact.PURPLE)
        bot.spindexer?.motorState = MotorPosition.OUTTAKE_1 // TODO: check this
        P = Points(bot.allianceColor)
        allianceToggle.toggle(gamepad1.touchpad)
        if (allianceToggle.justChanged) {
            bot.allianceColor = bot.allianceColor.next()
        }
        telemetry.addData("Alliance Color", bot.allianceColor)
        val processor = bot.camera?.getProcessor<AprilTagProcessor>()
        val motif = Obelisk.detectMotif(processor!!.detections, bot.allianceColor)
        if (motif != null) {
            motifOrder = motif
        }
        telemetry.addData("Detected Motif", motif?.toString())
        telemetry.addData("Motif Order", motifOrder.toString())

        telemetry.update()
    }

    override fun onLoop() {
        bot.turret?.autoTrack(
            bot.pinpoint?.pose ?: Pose(),
            if (bot.allianceColor == AllianceColor.BLUE) GoalTag.BLUE.pose + (P.shootingOffset) else GoalTag.RED.pose + P.shootingOffset, // TODO Use GoalTag shooting offset
        )
        when (state) {
            State.INIT -> {
                bot.pinpoint!!.reset(Pose(43.0, -157.0))
//                bot.spindexer?.setArtifacts(Artifact.GREEN, Artifact.PURPLE, Artifact.PURPLE)
                bot.follower.path = null
                state = State.DRIVE_1
            }
            State.DRIVE_1 -> state_drive_1()
            State.SHOOT -> state_shoot()
            State.DRIVE_2 -> state_drive_2()
            State.STOP -> state_stop()
        }
        telemetry.addData("Artifacts", bot.spindexer?.artifacts.contentDeepToString())
        telemetry.addData("State", state)
        telemetry.addData("Launch State", launchState)
        telemetry.addData("Flywheel Velocity", bot.flywheel?.velocity)
        telemetry.addData("Motif Index", motifIndex)
    }

    override fun onStop() {
        super.onStop()
    }

    fun state_drive_1() {
        if (bot.follower.path == null) {
            bot.flywheel?.velocity = 1000.0
            // FIXME: pose transpose
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
                        bot.spindexer?.moveToNextOuttake(motifOrder.getNextArtifact())
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
                            if (bot.spindexer?.isEmpty == true) {
                                state = State.DRIVE_2
                                bot.follower.path = null
                            }
                            launchState = LaunchState.READY
                        }
                    }
                }
            }
        }
    }

    fun state_drive_2() {
        if (bot.follower.path == null) {
            bot.follower.path = LinearPath(bot.pinpoint!!.pose, P.PREP_COLLECT_AUDIENCE)
            bot.spindexer?.moveToNextOpenIntake()
            // TODO: set turret and everything to 0
        }

        if (bot.follower.done) {
            bot.flywheel?.velocity = 0.0
            bot.follower.path = null
            state = State.STOP
        }
    }

    fun state_stop() {
        stop()
    }
}

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

    private var state = State.INIT
    private var launchState = LaunchState.READY
    private lateinit var P: Points
    private val allianceToggle = Toggle(false)
    private var motifOrder: Motif = Motif(21)
    private var motifIndex = 0
    private val resetTimer = ElapsedTime()
    private var shouldAutoTrack = true

    override fun onInit() {
        bot = Bot.fromType(BotType.COMP_BOT, hardwareMap)
    }

    override fun init_loop() {
        allianceToggle.toggle(gamepad1.touchpad)
        if (allianceToggle.justChanged) {
            bot.allianceColor = bot.allianceColor.next()
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

    override fun onLoop() {
        if (shouldAutoTrack) updateTurretTracking()

        when (state) {
            State.INIT -> {
                bot.spindexer?.checkingForNewArtifacts = false
                P = Points(bot.allianceColor)
                Constants.TransferData.allianceColor = bot.allianceColor
                bot.spindexer?.apply {
                    setArtifacts(Artifact.GREEN, Artifact.PURPLE, Artifact.PURPLE)
                    bot.spindexer?.moveToNextOuttake(motifOrder.currentArtifact)
                }
                bot.pinpoint?.reset(Points(bot.allianceColor).START_FAR)
                bot.follower.path = null
                state = State.DRIVE_1
            }
            State.DRIVE_1 -> state_drive_1()
            State.SHOOT -> state_shoot()
            State.DRIVE_2 -> state_drive_2()
            State.STOP -> state_stop()
        }

        updateTelemetry()
    }

    private fun state_drive_1() {
        if (bot.follower.path == null) {
            bot.flywheel?.velocity = 1000.0
            bot.follower.path = LinearPath(bot.pinpoint!!.pose, P.SHOOT_GOAL_FAR)
        }

        if (bot.follower.done) {
            state = State.SHOOT
        }
    }

    private fun state_shoot() {
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
                                resetTimer.reset()
                                bot.follower.path = null
                                shouldAutoTrack = false
                                state = State.STOP
//                                bot.spindexer?.moveToNextOpenIntake()
//                                bot.turret?.gotoAngle(0.0)
//                                state = State.DRIVE_2
                            }
                            launchState = LaunchState.READY
                        }
                    }
                }
            }
        }
    }

    private fun state_drive_2() {
        if (bot.follower.path == null) {
            bot.follower.path = LinearPath(bot.pinpoint!!.pose, P.PREP_COLLECT_AUDIENCE)
            bot.spindexer?.moveToNextOpenIntake()
        }

        if (bot.follower.done) {
            bot.flywheel?.velocity = 0.0
            bot.follower.path = null
            state = State.STOP
        }
    }

    private fun state_stop() {
        if (resetTimer.seconds() > 2.5) {
            bot.mecanumBase?.setDriveVA(Pose(vy=20.0))
            bot.flywheel?.velocity = 0.0
            bot.spindexer?.moveToNextOpenIntake()
            bot.turret?.gotoAngle(0.0)
        }
        if (resetTimer.seconds() > 5.0) {
            requestOpModeStop()
        }
//        if (bot.spindexer?.reachedTarget == true &&
//            bot.turret?.reachedTarget == true) {
//            Constants.TransferData.turretPositionTicks = bot.turret?.currentTicks ?: 0
//            Constants.TransferData.spindexerPositionTicks = bot.spindexer?.currentMotorPosition ?: 0
//            stop()
//        }
    }

    private fun updateTurretTracking() {
        bot.turret?.autoTrack(
            bot.pinpoint?.pose ?: Pose(),
            if (bot.allianceColor == AllianceColor.BLUE) GoalTag.BLUE.shootingPose else GoalTag.RED.shootingPose,
        )
    }

    private fun updateTelemetry() {
        telemetry.addData("Artifacts", bot.spindexer?.artifacts.contentDeepToString())
        telemetry.addData("State", state)
        telemetry.addData("Launch State", launchState)
        telemetry.addData("Flywheel Velocity", bot.flywheel?.velocity)
        telemetry.addData("Motif Index", motifIndex)
    }

//    override fun onStop() {
//        bot.apply {
//            flywheel?.velocity = 0.0
//            turret?.gotoAngle(0.0, 0.0)
//            spindexer?.motorState = MotorPosition.INTAKE_1
//            updateAll(dt)
//        }
//        super.onStop()
//    }
}

package pioneer.opmodes.teleop

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.robotcore.external.Const
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import pioneer.Bot
import pioneer.BotType
import pioneer.Constants
import pioneer.decode.GoalTagProcessor
import pioneer.general.AllianceColor
import pioneer.hardware.prism.Color
import pioneer.helpers.Pose
import pioneer.helpers.Toggle
import pioneer.helpers.next
import pioneer.opmodes.BaseOpMode
import pioneer.opmodes.teleop.drivers.TeleopDriver1
import pioneer.opmodes.teleop.drivers.TeleopDriver2
import kotlin.math.*

@TeleOp(name = "Teleop")
class Teleop : BaseOpMode() {
    private lateinit var driver1: TeleopDriver1
    private lateinit var driver2: TeleopDriver2
    private val allianceToggle = Toggle(false)
    private var changedAllianceColor = false

    // AprilTag drift correction settings
    private var enableAprilTagCorrection = true
    private var lastCorrectionTime = 0L
    private val CORRECTION_INTERVAL_MS = 500L
    private var visionBlendFactor = 0.3  // 30% vision, 70% odometry
    private val MIN_TAG_CONFIDENCE = 0.7
    private val MAX_CORRECTION_DISTANCE = 20.0  // cm

    override fun onInit() {
        bot = Bot.fromType(BotType.COMP_BOT, hardwareMap)

        driver2 = TeleopDriver2(gamepad2, bot)
        driver1 = TeleopDriver1(gamepad1, bot, driver2)
    }

    override fun init_loop() {
        allianceToggle.toggle(gamepad1.touchpad)
        if (allianceToggle.justChanged) {
            changedAllianceColor = true
            bot.allianceColor = bot.allianceColor.next()
            bot.led?.setColor(
                when(bot.allianceColor) {
                    AllianceColor.RED -> Color.RED
                    AllianceColor.BLUE -> Color.BLUE
                    AllianceColor.NEUTRAL -> Color.PURPLE
                }
            )
            // Update field points when alliance changes
            driver1.updateFieldPoints()
        }
        telemetry.addData("Alliance Color", bot.allianceColor)
        telemetry.addData("AprilTag Correction", if (enableAprilTagCorrection) "ENABLED" else "DISABLED")
        telemetry.update()
    }

    override fun onStart() {
        if (!changedAllianceColor) bot.allianceColor = Constants.TransferData.allianceColor
        driver2.onStart()
        driver1.updateFieldPoints()
    }

    override fun onLoop() {
        // Update gamepad inputs
        driver1.update()
        driver2.update()

        // Automatic AprilTag drift correction
        if (enableAprilTagCorrection) {
            correctOdometryWithAprilTags()
        }

        // Toggle drift correction with gamepad1.left_stick_button
        if (gamepad1.left_stick_button) {
            enableAprilTagCorrection = !enableAprilTagCorrection
            gamepad1.rumble(200)
        }

        // Add telemetry data
        addTelemetryData()
    }

    // Automatic drift correction using AprilTags
    private fun correctOdometryWithAprilTags() {
        val currentTime = System.currentTimeMillis()

        // Rate limiting
        if (currentTime - lastCorrectionTime < CORRECTION_INTERVAL_MS) {
            return
        }

        // Get AprilTag detections
        val detections = bot.camera?.getProcessor<AprilTagProcessor>()?.detections

        if (detections.isNullOrEmpty()) {
            return
        }

        // Get current odometry pose
        val currentPose = bot.pinpoint?.pose ?: return

        // Filter for high-quality detections
        val goodDetections = detections.filter { detection ->
            detection.ftcPose != null &&
                    detection.hamming <= 2 &&
                    detection.decisionMargin > MIN_TAG_CONFIDENCE &&
                    GoalTagProcessor.isValidGoalTag(detection.id)
        }

        if (goodDetections.isEmpty()) {
            return
        }

        // Use existing GoalTagProcessor to calculate robot pose from tags
        val robotPoseFromTag = GoalTagProcessor.getRobotFieldPose(goodDetections) ?: return

        // Calculate correction distance
        val correctionDistance = sqrt(
            (robotPoseFromTag.x - currentPose.x).pow(2) +
                    (robotPoseFromTag.y - currentPose.y).pow(2)
        )

        // Ignore corrections that are too large
        if (correctionDistance > MAX_CORRECTION_DISTANCE) {
            return
        }

        // Blend vision pose with odometry pose
        val blendedPose = Pose(
            x = (1 - visionBlendFactor) * currentPose.x + visionBlendFactor * robotPoseFromTag.x,
            y = (1 - visionBlendFactor) * currentPose.y + visionBlendFactor * robotPoseFromTag.y,
            theta = blendAngles(currentPose.theta, robotPoseFromTag.theta, visionBlendFactor)
        )

        // Reset odometry to blended pose
        bot.pinpoint?.reset(blendedPose)

        // Update last correction time
        lastCorrectionTime = currentTime
    }

    // Blend two angles with proper wrapping
    private fun blendAngles(angle1: Double, angle2: Double, blendFactor: Double): Double {
        val x1 = cos(angle1)
        val y1 = sin(angle1)
        val x2 = cos(angle2)
        val y2 = sin(angle2)

        val xBlended = (1 - blendFactor) * x1 + blendFactor * x2
        val yBlended = (1 - blendFactor) * y1 + blendFactor * y2

        return atan2(yBlended, xBlended)
    }

    private fun addTelemetryData() {
        addTelemetryData("Alliance Color", bot.allianceColor, Verbose.INFO)
        addTelemetryData("Drive Power", driver1.drivePower, Verbose.INFO)
        addTelemetryData("Pose", bot.pinpoint!!.pose, Verbose.DEBUG)
        addTelemetryData("Artifacts", bot.spindexer?.artifacts.contentDeepToString(), Verbose.INFO)

        addTelemetryData("Turret Mode", bot.turret?.mode, Verbose.INFO)
        addTelemetryData("Flywheel Operating Mode", Constants.Flywheel.OPERATING_MODE, Verbose.INFO)
        addTelemetryData("Use Auto Track Offset", driver2.useAutoTrackOffset, Verbose.DEBUG)
        addTelemetryData("Flywheel Speed Offset", driver2.flywheelSpeedOffset, Verbose.DEBUG)
        addTelemetryData("Flywheel Target Speed", driver2.flywheelSpeed, Verbose.DEBUG)
        addTelemetryData("Flywheel TPS", bot.flywheel?.velocity, Verbose.DEBUG)
        addTelemetryData("Turret Angle", driver2.turretAngle, Verbose.DEBUG)

        addTelemetryData("Drive Power", driver1.drivePower, Verbose.DEBUG)
        addTelemetryData("Spindexer State", bot.spindexer?.motorState, Verbose.INFO)

        addTelemetryData("Relative Tag Pose", Pose(driver1.detection?.ftcPose?.x ?: 0.0, driver1.detection?.ftcPose?.y ?: 0.0), Verbose.FATAL)
        addTelemetryData("Robot Pose Tag", driver1.robotPoseTag, Verbose.FATAL)

        addTelemetryData("Spindexer Target Ticks", bot.spindexer?.targetMotorTicks, Verbose.DEBUG)
        addTelemetryData("Spindexer Ticks", bot.spindexer?.currentMotorTicks, Verbose.DEBUG)
        telemetryPacket.put("Spindexer Target Ticks", bot.spindexer?.targetMotorTicks)
        telemetryPacket.put("Spindexer Ticks", bot.spindexer?.currentMotorTicks)
        telemetryPacket.put("Spindexer Velocity", bot.spindexer?.currentMotorVelocity)

        addTelemetryData("Field Centric", driver1.fieldCentric, Verbose.INFO)
        addTelemetryData("Velocity", "vx: %.2f, vy: %.2f".format(bot.pinpoint?.pose?.vx, bot.pinpoint?.pose?.vy), Verbose.DEBUG)
        addTelemetryData("Voltage", bot.batteryMonitor?.voltage, Verbose.INFO)
        addTelemetryData("Flywheel Motor Current", bot.flywheel?.motor?.getCurrent(CurrentUnit.MILLIAMPS), Verbose.DEBUG)
        telemetryPacket.put("Flywheel TPS", (bot.flywheel?.velocity ?: 0.0))
        telemetryPacket.put("Target Flywheel TPS", (driver2.flywheelSpeed))
        telemetryPacket.addLine("Flywheel TPS" + (bot.flywheel?.velocity ?: 0.0))

        telemetryPacket.put("Turret Target Ticks", bot.turret?.targetTicks)
        telemetryPacket.put("Turret Current Ticks", bot.turret?.currentTicks)

        // Drift correction telemetry
        addTelemetryData("AprilTag Correction", if (enableAprilTagCorrection) "ON" else "OFF", Verbose.INFO)
        val visibleGoalTags = bot.camera?.getProcessor<AprilTagProcessor>()?.detections
            ?.count { GoalTagProcessor.isValidGoalTag(it.id) } ?: 0
        addTelemetryData("Visible Goal Tags", visibleGoalTags, Verbose.INFO)

        // ✅ NEW: Flywheel status telemetry
        addTelemetryData("Flywheel State", bot.flywheel?.state?.name, Verbose.INFO)
        addTelemetryData("Thermal Status", bot.flywheel?.getThermalStatus(), Verbose.INFO)

        // ✅ NEW: Multi-shot status
        if (driver2.multiShotStatus.isNotEmpty()) {
            addTelemetryData("Multi-Shot", driver2.multiShotStatus, Verbose.INFO)
        }
    }
}
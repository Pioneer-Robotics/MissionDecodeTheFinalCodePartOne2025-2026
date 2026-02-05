package pioneer.opmodes.teleop.input

import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import pioneer.Bot
import pioneer.decode.Artifact
import pioneer.decode.GoalTag
import pioneer.general.AllianceColor
import pioneer.hardware.Turret
import pioneer.hardware.prism.Color
import pioneer.helpers.Pose
import pioneer.opmodes.teleop.commands.*
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sign

/**
 * Input mapping for Driver 2 - Shooting and scoring controls.
 *
 * CRITICAL FIX: This mapper reads all input states ONCE per frame,
 * preventing the button timing issues that were causing failures.
 */
class Driver2InputMapper(
    private val gamepad: com.qualcomm.robotcore.hardware.Gamepad
) : InputMapper {

    // ====================================================================
    // COMMANDS (reusable, stateless)
    // ====================================================================
    private val setFlywheelCommand = SetFlywheelVelocityCommand()
    private val stopFlywheelCommand = StopFlywheelCommand()
    private val setTurretAngleCommand = SetTurretAngleCommand()
    private val turretAutoTrackCommand = TurretAutoTrackCommand()
    private val moveToNextOuttakeCommand = MoveSpindexerToNextOuttakeCommand()
    private val triggerLauncherCommand = TriggerLauncherCommand()
    private val popArtifactCommand = PopArtifactCommand()
    private val setLEDCommand = SetLEDColorCommand()
    private val setGamepadLEDCommand = SetGamepadLEDCommand(gamepad)

    // ====================================================================
    // STATE (persistent between frames)
    // ====================================================================

    // Flywheel control state
    private var flywheelEnabled = false
    private var useManualFlywheelSpeed = false  // false = auto-estimate, true = manual
    private var manualFlywheelSpeed = 0.0
    private var flywheelSpeedOffset = 0.0  // Offset applied to auto-estimated speed

    // Turret control state
    private var turretMode = Turret.Mode.MANUAL
    private var manualTurretAngle = 0.0
    private var useAutoTrackOffset = false
    private var tagShootingTarget = Pose()
    private var offsetShootingTarget = Pose()

    // Shooting state
    private var shootingArtifact = false
    private val launchPressedTimer = ElapsedTime()

    // Goal target
    var targetGoal = GoalTag.RED

    override fun mapInputsToCommands(input: InputState, bot: Bot): List<Pair<Command, CommandContext>> {
        val commands = mutableListOf<Pair<Command, CommandContext>>()

        // ====================================================================
        // FLYWHEEL SPEED CONTROL
        // This is where the bug was! All dpad checks now happen in sequence
        // with the input state captured ONCE at the start of the frame.
        // ====================================================================

        // Toggle between manual and auto speed estimation (dpad left)
        if (input.isPressed(Button.DPAD_LEFT)) {
            useManualFlywheelSpeed = !useManualFlywheelSpeed
        }

        // Adjust speed based on mode
        if (useManualFlywheelSpeed) {
            // Manual mode: dpad up/down changes absolute speed
            if (input.isPressed(Button.DPAD_UP)) {
                manualFlywheelSpeed += 50.0
            }
            if (input.isPressed(Button.DPAD_DOWN)) {
                manualFlywheelSpeed -= 50.0
            }
        } else {
            // Auto-estimate mode: dpad up/down changes offset
            if (input.isPressed(Button.DPAD_UP)) {
                flywheelSpeedOffset += 10.0
            }
            if (input.isPressed(Button.DPAD_DOWN)) {
                flywheelSpeedOffset -= 10.0
            }
            // Reset offset with left stick button
            if (input.isPressed(Button.LEFT_STICK_BUTTON)) {
                flywheelSpeedOffset = 0.0
            }
        }

        // Calculate target flywheel speed
        val targetFlywheelSpeed = if (useManualFlywheelSpeed) {
            manualFlywheelSpeed
        } else {
            // Auto-estimate based on distance to goal
            val estimatedSpeed = bot.flywheel?.estimateVelocity(
                bot.pinpoint?.pose ?: Pose(),
                tagShootingTarget,
                targetGoal.shootingHeight
            ) ?: 0.0
            estimatedSpeed + flywheelSpeedOffset
        }

        // ====================================================================
        // FLYWHEEL ON/OFF TOGGLE (dpad right)
        // ====================================================================
        if (input.isPressed(Button.DPAD_RIGHT)) {
            flywheelEnabled = !flywheelEnabled
        }

        // Apply flywheel command
        if (flywheelEnabled) {
            commands.add(setFlywheelCommand to CommandContext(analogValue = targetFlywheelSpeed))
        } else {
            commands.add(stopFlywheelCommand to CommandContext.EMPTY)
        }

        // ====================================================================
        // TURRET MODE TOGGLE (cross button)
        // ====================================================================
        if (input.isPressed(Button.CROSS)) {
            turretMode = if (turretMode == Turret.Mode.MANUAL) {
                Turret.Mode.AUTO_TRACK
            } else {
                Turret.Mode.MANUAL
            }
            bot.turret?.mode = turretMode
        }

        // ====================================================================
        // TURRET CONTROL (mode-dependent)
        // ====================================================================
        if (turretMode == Turret.Mode.MANUAL) {
            // Manual turret control with right stick
            val turretAdjustment = input.getStickValue(Stick.RIGHT_X)
            if (turretAdjustment != 0.0) {
                // Cubic scaling for fine control
                val scaledAdjustment = sign(turretAdjustment) *
                        abs(turretAdjustment).pow(3) / 10.0
                manualTurretAngle -= scaledAdjustment
            }

            // Reset turret angle with right stick button
            if (input.isPressed(Button.RIGHT_STICK_BUTTON)) {
                manualTurretAngle = 0.0
            }

            commands.add(setTurretAngleCommand to CommandContext(analogValue = manualTurretAngle))

        } else {
            // Auto-tracking mode
            val tagDetections = bot.camera?.getProcessor<AprilTagProcessor>()?.detections
            val errorDegrees = tagDetections?.firstOrNull()?.ftcPose?.bearing?.times(-1.0)

            val trackContext = CommandContext(
                parameters = if (errorDegrees != null) {
                    mapOf("errorDegrees" to (errorDegrees as Any))
                } else {
                    emptyMap()
                }
            )
            commands.add(turretAutoTrackCommand to trackContext)

            // Manual offset adjustment in auto-track mode
            val offsetAdjustment = input.getStickValue(Stick.RIGHT_X)
            if (offsetAdjustment != 0.0) {
                useAutoTrackOffset = true
                val scaledAdjustment = sign(offsetAdjustment) *
                        abs(offsetAdjustment).pow(3) / 17.5
                offsetShootingTarget = offsetShootingTarget.rotate(-scaledAdjustment)
            }

            // Reset offset with right stick button
            if (input.isPressed(Button.RIGHT_STICK_BUTTON)) {
                useAutoTrackOffset = false
                offsetShootingTarget = tagShootingTarget
            }
        }

        // ====================================================================
        // ARTIFACT SELECTION (bumpers and triangle)
        // ====================================================================
        if (input.isPressed(Button.RIGHT_BUMPER)) {
            val context = CommandContext(parameters = mapOf("artifact" to Artifact.PURPLE))
            commands.add(moveToNextOuttakeCommand to context)
        }
        if (input.isPressed(Button.LEFT_BUMPER)) {
            val context = CommandContext(parameters = mapOf("artifact" to Artifact.GREEN))
            commands.add(moveToNextOuttakeCommand to context)
        }
        if (input.isPressed(Button.TRIANGLE)) {
            commands.add(moveToNextOuttakeCommand to CommandContext.EMPTY)
        }

        // ====================================================================
        // LAUNCH CONTROL (square button)
        // ====================================================================

        // Check if we finished shooting the previous artifact
        if (shootingArtifact && bot.launcher?.isReset == true) {
            shootingArtifact = false
            commands.add(popArtifactCommand to CommandContext.EMPTY)
        }

        // Trigger new launch
        if (input.isPressed(Button.SQUARE) && flywheelEnabled) {
            // Normal launch: check if spindexer is in position
            if (bot.spindexer?.withinDetectionTolerance == true &&
                bot.spindexer?.isOuttakePosition == true) {
                commands.add(triggerLauncherCommand to CommandContext.EMPTY)
                shootingArtifact = true
                launchPressedTimer.reset()
            }
            // Quick double-tap launch (within 0.5s)
            else if (launchPressedTimer.seconds() < 0.5) {
                commands.add(triggerLauncherCommand to CommandContext.EMPTY)
                shootingArtifact = true
            }

            launchPressedTimer.reset()
        }

        // ====================================================================
        // LED INDICATOR (based on flywheel readiness)
        // ====================================================================
        val currentFlywheelSpeed = bot.flywheel?.velocity ?: 0.0
        val speedDifference = kotlin.math.abs(targetFlywheelSpeed - currentFlywheelSpeed)

        when {
            speedDifference < 20.0 -> {
                // Ready to shoot - green
                commands.add(setLEDCommand to CommandContext(parameters = mapOf("color" to Color.GREEN)))
                commands.add(setGamepadLEDCommand to CommandContext(parameters = mapOf(
                    "r" to 0.0, "g" to 1.0, "b" to 0.0
                )))
            }
            currentFlywheelSpeed < targetFlywheelSpeed - 20.0 -> {
                // Spooling up - yellow
                commands.add(setLEDCommand to CommandContext(parameters = mapOf("color" to Color.YELLOW)))
                commands.add(setGamepadLEDCommand to CommandContext(parameters = mapOf(
                    "r" to 255.0, "g" to 165.0, "b" to 0.0
                )))
            }
            else -> {
                // Overshooting - red
                commands.add(setLEDCommand to CommandContext(parameters = mapOf("color" to Color.RED)))
                commands.add(setGamepadLEDCommand to CommandContext(parameters = mapOf(
                    "r" to 1.0, "g" to 0.0, "b" to 0.0
                )))
            }
        }

        return commands
    }

    // ====================================================================
    // PUBLIC METHODS (for telemetry and coordination)
    // ====================================================================

    fun onStart(bot: Bot) {
        // Set target goal based on alliance
        targetGoal = if (bot.allianceColor == AllianceColor.BLUE) {
            GoalTag.BLUE
        } else {
            GoalTag.RED
        }
        tagShootingTarget = targetGoal.shootingPose
        offsetShootingTarget = tagShootingTarget
    }

    fun resetTurretOffsets() {
        flywheelSpeedOffset = 0.0
        useAutoTrackOffset = false
        offsetShootingTarget = tagShootingTarget
    }

    // Telemetry accessors
    fun getFlywheelSpeedOffset(): Double = flywheelSpeedOffset
    fun getTurretAngle(): Double = manualTurretAngle
    fun getUseAutoTrackOffset(): Boolean = useAutoTrackOffset
    fun getCurrentFlywheelSpeed(): Double = if (useManualFlywheelSpeed) manualFlywheelSpeed else 0.0
}
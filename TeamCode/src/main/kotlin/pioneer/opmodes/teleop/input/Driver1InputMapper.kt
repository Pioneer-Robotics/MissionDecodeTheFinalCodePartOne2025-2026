package pioneer.opmodes.teleop.input

import pioneer.Bot
import pioneer.opmodes.teleop.commands.*

/**
 * Input mapping configuration for a driver.
 * 
 * This class defines which buttons trigger which commands.
 * Separating this from the driver class makes it easy to:
 * - Remap buttons for different drivers
 * - Save/load button configurations
 * - Debug input issues
 * 
 * Design principle: Interface Segregation - input mapping is separate from
 * input reading and command execution.
 */
interface InputMapper {
    /**
     * Map the current input state to a list of commands to execute.
     * 
     * @param input The current input state
     * @param bot The robot (needed for some context like alliance color)
     * @return List of (Command, Context) pairs to execute
     */
    fun mapInputsToCommands(input: InputState, bot: Bot): List<Pair<Command, CommandContext>>
}

/**
 * Default input mapping for Driver 1.
 * 
 * This replicates the original TeleopDriver1 behavior but with better
 * separation of concerns.
 */
class Driver1InputMapper(
    private val gamepad: com.qualcomm.robotcore.hardware.Gamepad
) : InputMapper {
    
    // Commands (reusable, stateless)
    private val driveCommand = DriveCommand()
    private val startIntakeCommand = StartIntakeCommand()
    private val stopIntakeCommand = StopIntakeCommand()
    private val reverseIntakeCommand = ReverseIntakeCommand()
    private val moveToNextIntakeCommand = MoveSpindexerToNextIntakeCommand()
    private val moveSpindexerManualCommand = MoveSpindexerManualCommand()
    private val resetSpindexerCommand = ResetSpindexerCommand()
    private val resetOdometryCommand = ResetOdometryCommand()
    
    // State (toggles and values that persist between frames)
    private var drivePower = pioneer.Constants.Drive.DEFAULT_POWER
    private var fieldCentric = false
    private var intakeEnabled = false
    
    override fun mapInputsToCommands(input: InputState, bot: Bot): List<Pair<Command, CommandContext>> {
        val commands = mutableListOf<Pair<Command, CommandContext>>()
        
        // ====================================================================
        // DRIVE (always active)
        // ====================================================================
        val driveX = input.getStickValue(Stick.LEFT_X)
        val driveY = -input.getStickValue(Stick.LEFT_Y)  // Inverted
        val driveOmega = input.getStickValue(Stick.RIGHT_X)
        
        if (driveX != 0.0 || driveY != 0.0 || driveOmega != 0.0) {
            val driveContext = CommandContext(
                vectorX = driveX,
                vectorY = driveY,
                vectorOmega = driveOmega,
                parameters = mapOf(
                    "drivePower" to drivePower,
                    "fieldCentric" to fieldCentric,
                    "robotTheta" to (bot.pinpoint?.pose?.theta ?: 0.0)
                )
            )
            commands.add(driveCommand to driveContext)
        }
        
        // ====================================================================
        // DRIVE POWER ADJUSTMENT (bumpers)
        // ====================================================================
        if (input.isPressed(Button.RIGHT_BUMPER)) {
            drivePower = (drivePower + 0.1).coerceIn(0.1, 1.0)
        }
        if (input.isPressed(Button.LEFT_BUMPER)) {
            drivePower = (drivePower - 0.1).coerceIn(0.1, 1.0)
        }
        
        // ====================================================================
        // FIELD-CENTRIC TOGGLE (touchpad)
        // ====================================================================
        if (input.isPressed(Button.TOUCHPAD)) {
            fieldCentric = !fieldCentric
        }
        
        // ====================================================================
        // INTAKE CONTROL
        // ====================================================================
        // Toggle intake on/off with circle button
        if (input.isPressed(Button.CIRCLE)) {
            intakeEnabled = !intakeEnabled
            
            // When intake is turned on, move spindexer to next position
            if (intakeEnabled) {
                commands.add(moveToNextIntakeCommand to CommandContext.EMPTY)
            }
        }
        
        // Override: reverse intake with dpad down
        if (input.isHeld(Button.DPAD_DOWN)) {
            commands.add(reverseIntakeCommand to CommandContext.EMPTY)
        } else {
            // Normal intake operation based on toggle state
            if (intakeEnabled) {
                commands.add(startIntakeCommand to CommandContext.EMPTY)
            } else {
                commands.add(stopIntakeCommand to CommandContext.EMPTY)
            }
        }
        
        // ====================================================================
        // SPINDEXER MANUAL CONTROL (triggers)
        // ====================================================================
        val rightTrigger = input.getTriggerValue(Trigger.RIGHT)
        val leftTrigger = input.getTriggerValue(Trigger.LEFT)
        
        if (rightTrigger > 0.0) {
            commands.add(moveSpindexerManualCommand to CommandContext(analogValue = rightTrigger))
        } else if (leftTrigger > 0.0) {
            commands.add(moveSpindexerManualCommand to CommandContext(analogValue = -leftTrigger))
        }
        
        // ====================================================================
        // SPINDEXER RESET (share button)
        // ====================================================================
        if (input.isPressed(Button.SHARE)) {
            commands.add(resetSpindexerCommand to CommandContext.EMPTY)
        }
        
        // ====================================================================
        // ODOMETRY RESET (options button)
        // ====================================================================
        if (input.isPressed(Button.OPTIONS)) {
            val resetPose = if (bot.allianceColor == pioneer.general.AllianceColor.RED) {
                pioneer.helpers.Pose(-86.7, -99.0, theta = 0.0)
            } else {
                pioneer.helpers.Pose(86.7, -99.0, theta = 0.0)
            }
            
            val context = CommandContext(parameters = mapOf("pose" to resetPose))
            commands.add(resetOdometryCommand to context)
        }
        
        return commands
    }
    
    /**
     * Get current drive power (for telemetry).
     */
    fun getDrivePower(): Double = drivePower
    
    /**
     * Get field-centric state (for telemetry).
     */
    fun isFieldCentric(): Boolean = fieldCentric
}

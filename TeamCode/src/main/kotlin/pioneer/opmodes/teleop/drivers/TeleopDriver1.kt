package pioneer.opmodes.teleop.drivers

import com.qualcomm.robotcore.hardware.Gamepad
import pioneer.Bot
import pioneer.opmodes.teleop.commands.CommandContext
import pioneer.opmodes.teleop.input.Driver1InputMapper
import pioneer.opmodes.teleop.input.InputState
import pioneer.helpers.FileLogger

/**
 * Refactored TeleopDriver1 using SOLID principles.
 *
 * DESIGN PRINCIPLES APPLIED:
 *
 * 1. Single Responsibility:
 *    - This class ONLY coordinates input reading and command execution
 *    - Input mapping logic is in Driver1InputMapper
 *    - Action logic is in individual Command classes
 *
 * 2. Open/Closed:
 *    - Easy to add new commands without modifying this class
 *    - Easy to remap buttons by changing the InputMapper
 *
 * 3. Liskov Substitution:
 *    - Could swap different input mappers for different control schemes
 *
 * 4. Interface Segregation:
 *    - InputState, InputMapper, Command are separate interfaces
 *
 * 5. Dependency Inversion:
 *    - Depends on Bot abstraction, not concrete hardware classes
 *
 * BENEFITS OF THIS REFACTOR:
 * - No more button timing issues (input captured once per frame)
 * - Easy to debug (can log which commands are executing)
 * - Easy to test (can test commands independently)
 * - Easy to remap buttons (just change the mapper)
 * - Clear separation of concerns
 */
class TeleopDriver1(
    private val gamepad: Gamepad,
    private val bot: Bot,
) {
    // Input mapper handles button-to-command mapping
    private val inputMapper = Driver1InputMapper(gamepad)

    // Track previous input state for edge detection
    private var previousInputState: InputState? = null

    /**
     * Main update loop.
     *
     * This method follows a simple pattern:
     * 1. Capture current input state (immutable snapshot)
     * 2. Map inputs to commands
     * 3. Execute all commands
     *
     * This pattern eliminates button timing issues because the input
     * state is captured ONCE at the start of the frame.
     */
    fun update() {
        // Step 1: Capture input state
        val currentInput = InputState.fromGamepad(gamepad, previousInputState)

        // Step 2: Map inputs to commands
        val commandsToExecute = inputMapper.mapInputsToCommands(currentInput, bot)

        // Step 3: Execute all commands
        for ((command, context) in commandsToExecute) {
            try {
                command.execute(bot, context)
            } catch (e: Exception) {
                // Log error but continue executing other commands
                // This prevents one broken command from breaking everything
                FileLogger.error(
                    "TeleopDriver1",
                    "Error executing command ${command.description()}: ${e.message}"
                )
            }
        }

        // Update previous state for next frame
        previousInputState = currentInput
    }

    /**
     * Get current drive power (for telemetry).
     */
    val drivePower: Double
        get() = inputMapper.getDrivePower()

    /**
     * Get field-centric state (for telemetry).
     */
    val fieldCentric: Boolean
        get() = inputMapper.isFieldCentric()

    // ====================================================================
    // LEGACY PROPERTIES (for compatibility with existing telemetry)
    // These can be removed once telemetry is updated
    // ====================================================================

    @Deprecated("Use bot.camera?.getProcessor<AprilTagProcessor>()?.detections?.firstOrNull() instead")
    var detection: org.firstinspires.ftc.vision.apriltag.AprilTagDetection? = null
        get() {
            return bot.camera?.getProcessor<org.firstinspires.ftc.vision.apriltag.AprilTagProcessor>()
                ?.detections?.firstOrNull()
        }

    @Deprecated("Calculate from detection if needed")
    var robotPoseTag: pioneer.helpers.Pose? = null
        get() {
            // This was commented out in original code anyway
            return null
        }
}
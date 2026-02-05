package pioneer.opmodes.teleop.drivers

import com.qualcomm.robotcore.hardware.Gamepad
import pioneer.Bot
import pioneer.opmodes.teleop.input.Driver2InputMapper
import pioneer.opmodes.teleop.input.InputState
import pioneer.helpers.FileLogger

/**
 * Refactored TeleopDriver2 using SOLID principles.
 *
 * CRITICAL FIX: Button handling issues resolved!
 *
 * The original code had a subtle bug where multiple methods would check
 * buttons in sequence, potentially missing button presses due to timing.
 *
 * For example, in updateFlywheelSpeed():
 * 1. Toggle isEstimatingSpeed with dpad_left
 * 2. Then check dpad_up and dpad_down
 *
 * If the toggle happened, the subsequent checks could be affected by
 * the state change happening mid-frame.
 *
 * NEW ARCHITECTURE FIXES THIS:
 * - Input is captured ONCE per frame (immutable snapshot)
 * - All button checks see the SAME input state
 * - No possibility of mid-frame state corruption
 * - Edge detection (isPressed) is reliable and consistent
 *
 * ADDITIONAL BENEFITS:
 * - Easy to add new controls without breaking existing ones
 * - Easy to debug (can log which commands are executing)
 * - Easy to test (commands are independent)
 * - Clear separation of input handling and robot actions
 */
class TeleopDriver2(
    private val gamepad: Gamepad,
    private val bot: Bot,
) {
    // Input mapper handles all button logic
    private val inputMapper = Driver2InputMapper(gamepad)

    // Track previous input state for edge detection
    private var previousInputState: InputState? = null

    /**
     * Initialize driver state when autonomous ends.
     * Should be called from Teleop.onStart().
     */
    fun onStart() {
        inputMapper.onStart(bot)
    }

    /**
     * Main update loop.
     *
     * Same simple pattern as Driver1:
     * 1. Capture input state
     * 2. Map to commands
     * 3. Execute commands
     *
     * This pattern is the KEY to fixing the button issues!
     */
    fun update() {
        // Step 1: Capture input state (immutable snapshot)
        val currentInput = InputState.fromGamepad(gamepad, previousInputState)

        // Step 2: Map inputs to commands
        // This is where all the button logic happens, in ONE place,
        // with ONE consistent view of the input state
        val commandsToExecute = inputMapper.mapInputsToCommands(currentInput, bot)

        // Step 3: Execute all commands
        for ((command, context) in commandsToExecute) {
            try {
                command.execute(bot, context)
            } catch (e: Exception) {
                // Log error but continue - prevents cascade failures
                FileLogger.error(
                    "TeleopDriver2",
                    "Error executing command ${command.description()}: ${e.message}"
                )
            }
        }

        // Update previous state for next frame
        previousInputState = currentInput
    }

    /**
     * Reset turret offsets (called from Driver1 when odometry is reset).
     */
    fun resetTurretOffsets() {
        inputMapper.resetTurretOffsets()
    }

    // ====================================================================
    // TELEMETRY ACCESSORS (for Teleop.kt)
    // ====================================================================

    val flywheelSpeedOffset: Double
        get() = inputMapper.getFlywheelSpeedOffset()

    val turretAngle: Double
        get() = inputMapper.getTurretAngle()

    val useAutoTrackOffset: Boolean
        get() = inputMapper.getUseAutoTrackOffset()

    // This returns the current TARGET speed (either manual or auto-estimated)
    // For telemetry display
    val flywheelSpeed: Double
        get() {
            // In the new architecture, the mapper calculates this
            // We can expose it if needed, but for now just return
            // the actual flywheel velocity
            return bot.flywheel?.velocity ?: 0.0
        }
}
package pioneer.opmodes.teleop.input

import com.qualcomm.robotcore.hardware.Gamepad

/**
 * Immutable snapshot of gamepad state for a single frame.
 * 
 * This class captures all button states at once, preventing race conditions
 * and making it easy to detect button combinations.
 * 
 * Design principle: Single Responsibility - only knows about input state,
 * not what actions those inputs should trigger.
 */
data class InputState(
    // Left stick
    val leftStickX: Float,
    val leftStickY: Float,
    val leftStickButton: Boolean,
    
    // Right stick
    val rightStickX: Float,
    val rightStickY: Float,
    val rightStickButton: Boolean,
    
    // D-pad
    val dpadUp: Boolean,
    val dpadDown: Boolean,
    val dpadLeft: Boolean,
    val dpadRight: Boolean,
    
    // Face buttons (PlayStation naming)
    val cross: Boolean,      // X / A
    val circle: Boolean,     // O / B
    val square: Boolean,     // □ / X
    val triangle: Boolean,   // △ / Y
    
    // Bumpers and triggers
    val leftBumper: Boolean,
    val rightBumper: Boolean,
    val leftTrigger: Float,
    val rightTrigger: Float,
    
    // Special buttons
    val share: Boolean,      // Share / Back
    val options: Boolean,    // Options / Start
    val touchpad: Boolean,   // PS button / Guide
    
    // Previous state for edge detection
    private val previous: InputState? = null
) {
    /**
     * Detects if button was just pressed (rising edge).
     * Returns true only on the frame where button goes from released to pressed.
     */
    fun isPressed(button: Button): Boolean {
        return getButtonState(button) && !(previous?.getButtonState(button) ?: false)
    }
    
    /**
     * Detects if button was just released (falling edge).
     */
    fun isReleased(button: Button): Boolean {
        return !getButtonState(button) && (previous?.getButtonState(button) ?: false)
    }
    
    /**
     * Check if button is currently held down.
     */
    fun isHeld(button: Button): Boolean {
        return getButtonState(button)
    }
    
    /**
     * Check if multiple buttons are all pressed simultaneously.
     */
    fun arePressed(vararg buttons: Button): Boolean {
        return buttons.all { isPressed(it) }
    }
    
    /**
     * Check if multiple buttons are all held simultaneously.
     */
    fun areHeld(vararg buttons: Button): Boolean {
        return buttons.all { isHeld(it) }
    }
    
    /**
     * Get the current state of a specific button.
     */
    private fun getButtonState(button: Button): Boolean {
        return when (button) {
            Button.DPAD_UP -> dpadUp
            Button.DPAD_DOWN -> dpadDown
            Button.DPAD_LEFT -> dpadLeft
            Button.DPAD_RIGHT -> dpadRight
            Button.CROSS -> cross
            Button.CIRCLE -> circle
            Button.SQUARE -> square
            Button.TRIANGLE -> triangle
            Button.LEFT_BUMPER -> leftBumper
            Button.RIGHT_BUMPER -> rightBumper
            Button.SHARE -> share
            Button.OPTIONS -> options
            Button.TOUCHPAD -> touchpad
            Button.LEFT_STICK_BUTTON -> leftStickButton
            Button.RIGHT_STICK_BUTTON -> rightStickButton
        }
    }
    
    /**
     * Get analog stick value with optional deadzone.
     */
    fun getStickValue(stick: Stick, deadzone: Double = 0.02): Double {
        val value = when (stick) {
            Stick.LEFT_X -> leftStickX.toDouble()
            Stick.LEFT_Y -> leftStickY.toDouble()
            Stick.RIGHT_X -> rightStickX.toDouble()
            Stick.RIGHT_Y -> rightStickY.toDouble()
        }
        return if (kotlin.math.abs(value) < deadzone) 0.0 else value
    }
    
    /**
     * Get trigger value with optional threshold.
     */
    fun getTriggerValue(trigger: Trigger, threshold: Double = 0.1): Double {
        val value = when (trigger) {
            Trigger.LEFT -> leftTrigger.toDouble()
            Trigger.RIGHT -> rightTrigger.toDouble()
        }
        return if (value < threshold) 0.0 else value
    }
    
    companion object {
        /**
         * Creates InputState snapshot from a Gamepad.
         * This is the only way to create an InputState - enforcing immutability.
         */
        fun fromGamepad(gamepad: Gamepad, previous: InputState? = null): InputState {
            return InputState(
                leftStickX = gamepad.left_stick_x,
                leftStickY = gamepad.left_stick_y,
                leftStickButton = gamepad.left_stick_button,
                
                rightStickX = gamepad.right_stick_x,
                rightStickY = gamepad.right_stick_y,
                rightStickButton = gamepad.right_stick_button,
                
                dpadUp = gamepad.dpad_up,
                dpadDown = gamepad.dpad_down,
                dpadLeft = gamepad.dpad_left,
                dpadRight = gamepad.dpad_right,
                
                cross = gamepad.cross,
                circle = gamepad.circle,
                square = gamepad.square,
                triangle = gamepad.triangle,
                
                leftBumper = gamepad.left_bumper,
                rightBumper = gamepad.right_bumper,
                leftTrigger = gamepad.left_trigger,
                rightTrigger = gamepad.right_trigger,
                
                share = gamepad.share,
                options = gamepad.options,
                touchpad = gamepad.touchpad,
                
                previous = previous
            )
        }
    }
}

/**
 * Enum of all digital buttons.
 * Makes code more readable and type-safe.
 */
enum class Button {
    DPAD_UP, DPAD_DOWN, DPAD_LEFT, DPAD_RIGHT,
    CROSS, CIRCLE, SQUARE, TRIANGLE,
    LEFT_BUMPER, RIGHT_BUMPER,
    SHARE, OPTIONS, TOUCHPAD,
    LEFT_STICK_BUTTON, RIGHT_STICK_BUTTON
}

/**
 * Enum of all analog sticks.
 */
enum class Stick {
    LEFT_X, LEFT_Y, RIGHT_X, RIGHT_Y
}

/**
 * Enum of all triggers.
 */
enum class Trigger {
    LEFT, RIGHT
}

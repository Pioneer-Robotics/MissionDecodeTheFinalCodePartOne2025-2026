package pioneer.helpers


/**
 * This class is a wrapper class for booleans, but it allows you to negate
 * the boolean by pressing a button. The intended use case is that there is some boolean
 * value used in an OpMode, and you want it to change every time you press a specific button
 * on the gamepad. Make that boolean a Toggle and call toggle(boolean)
 * every cycle using the desired button as the input to toggle()
 */
class Toggle(startState: Boolean) {
    var justChanged: Boolean = false
        private set // Prevents outside classes from changing value

    var state: Boolean = startState
        set(value) {
            // If the value is different from what it was before, justChanged is true
            if (value != field) {
                field = value
                justChanged = true
            }
        }

    private var prevState: Boolean = false

    /**
     * Changes the state of the boolean if it hasn't been changed in the previous cycle
     */
    fun toggle(button: Boolean) {
        // Reset justChanged to false at the start of the cycle
        justChanged = false
        // If the button is pressed and it wasn't pressed last cycle, change the state
        if (button && !prevState) {
            state = !state
            justChanged = true
        }
        prevState = button
    }
}

package pioneer.helpers

import kotlin.math.abs

/**
 * A PID/PIDF controller with integral clamping and output saturation.
 *
 * @param kp Proportional gain
 * @param ki Integral gain
 * @param kd Derivative gain
 * @param kf Feedforward gain
 */
class PIDController(
    val kp: Double,
    val ki: Double = 0.0,
    val kd: Double = 0.0,
    val kf: Double = 0.0,
) {
    init {
        require(kp >= 0.0) { "kp must be non-negative" }
        require(ki >= 0.0) { "ki must be non-negative" }
        require(kd >= 0.0) { "kd must be non-negative" }
    }

    // Internal state
    private var integral = 0.0
    private var lastError = 0.0
    private var lastTime = System.nanoTime()
    private var isFirstUpdate = true

    // Configuration
    var integralClamp = 1.0
        set(value) {
            field = abs(value)
        }

    var outputMin = Double.NEGATIVE_INFINITY
    var outputMax = Double.POSITIVE_INFINITY

    /**
     * Updates the controller with an error value and time delta.
     * @param error The error (target - current)
     * @param dt Time delta in seconds
     * @return Control output clamped to [outputMin, outputMax]
     */
    fun update(
        error: Double,
        dt: Double,
    ): Double {
        if (dt <= 0) return 0.0

        // Update integral with clamping
        integral = (integral + error * dt).coerceIn(-integralClamp, integralClamp)

        // Calculate derivative
        val derivative =
            if (isFirstUpdate) {
                lastError = error
                isFirstUpdate = false
                0.0
            } else {
                val deriv = (error - lastError) / dt
                lastError = error
                deriv
            }

        // Calculate and clamp output
        val output = (kp * error) + (ki * integral) + (kd * derivative)
        return output.coerceIn(outputMin, outputMax)
    }

    /**
     * Updates the controller with target and current values.
     * @param target The desired setpoint
     * @param current The current measured value
     * @param dt Time delta in seconds
     * @param normalizeRadians Normalize angular error to [-π, π]
     * @return Control output including feedforward
     */
    fun update(
        target: Double,
        current: Double,
        dt: Double,
        normalizeRadians: Boolean = false,
    ): Double {
        val error =
            if (normalizeRadians) {
                MathUtils.normalizeRadians(target - current)
            } else {
                target - current
            }

        return update(error, dt) + (kf * target)
    }

    /**
     * Auto-timestamped update using system time.
     */
    fun update(
        target: Double,
        current: Double,
        normalizeRadians: Boolean = false,
    ): Double {
        val currentTime = System.nanoTime()
        val dt = (currentTime - lastTime) / 1e9
        lastTime = currentTime
        return update(target, current, dt, normalizeRadians)
    }

    /**
     * Resets the controller state.
     */
    fun reset() {
        integral = 0.0
        lastError = 0.0
        lastTime = System.nanoTime()
        isFirstUpdate = true
    }

    /**
     * Sets output limits.
     */
    fun setOutputLimits(
        min: Double,
        max: Double,
    ) {
        require(min <= max) { "Minimum must be <= maximum" }
        outputMin = min
        outputMax = max
    }

    val hasFeedforward: Boolean
        get() = kf != 0.0

    val integralTerm: Double
        get() = integral

    override fun toString(): String {
        return if (hasFeedforward) {
            "PIDF(kp=$kp, ki=$ki, kd=$kd, kf=$kf)"
        } else {
            "PID(kp=$kp, ki=$ki, kd=$kd)"
        }
    }
}

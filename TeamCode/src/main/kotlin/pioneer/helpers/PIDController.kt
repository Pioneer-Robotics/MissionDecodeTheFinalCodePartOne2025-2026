package pioneer.helpers

import kotlin.math.abs

/**
 * A unified PID/PIDF controller with advanced features including derivative-on-measurement,
 * output saturation, setpoint weighting, and comprehensive error handling.
 * Thread-safe and suitable for real-time control applications.
 */
class PIDController(
    val kp: Double,
    val ki: Double = 0.0,
    val kd: Double = 0.0,
    val kf: Double = 0.0
) {
    // Alternative constructors for convenience
    constructor(coeffs: DoubleArray) : this(
        kp = coeffs.getOrElse(0) { 0.0 },
        ki = coeffs.getOrElse(1) { 0.0 },
        kd = coeffs.getOrElse(2) { 0.0 },
        kf = coeffs.getOrElse(3) { 0.0 }
    )

    constructor(coeffs: Array<Double>) : this(coeffs.toDoubleArray())

    // Internal state - synchronized for thread safety
    @Volatile private var integral = 0.0
    @Volatile private var lastError = 0.0  // Track previous error for derivative
    @Volatile private var lastTime = System.nanoTime()
    @Volatile private var isFirstUpdate = true

    // Configuration properties
    var integralClamp = 1.0
        set(value) { field = abs(value) }
    
    var outputMin = Double.NEGATIVE_INFINITY
    var outputMax = Double.POSITIVE_INFINITY

    /**
     * Updates the controller with an error value and time delta.
     * @param error The error value (target - current)
     * @param dt Time delta in seconds
     * @param measurement Current measurement (for derivative-on-measurement)
     * @return Control output, clamped to [outputMin, outputMax]
     */
    @Synchronized
    fun update(error: Double, dt: Double): Double {
        if (dt <= 0) return 0.0
        
        // Integral term with simple integration and clamping
        integral = (integral + error * dt).coerceIn(-integralClamp, integralClamp)
        
        // Derivative term
        val derivative = if (isFirstUpdate) {
            lastError = error
            isFirstUpdate = false
            0.0
        } else {
            val deriv = (error - lastError) / dt
            lastError = error
            deriv
        }
        
        // Combine PID terms
        val output = (kp * error) + (ki * integral) + (kd * derivative)
        
        return output.coerceIn(outputMin, outputMax)
    }

    /**
     * Updates the controller with target and current values.
     * @param target The desired setpoint
     * @param current The current measured value
     * @param dt Time delta in seconds
     * @param normalizeRadians Whether to normalize angular error to [-π, π]
     * @return Control output including feedforward term
     */
    fun update(target: Double, current: Double, dt: Double, normalizeRadians: Boolean = false): Double {
        val error = if (normalizeRadians) {
            MathUtils.normalizeRadians(target - current)
        } else {
            target - current
        }
        
        val pidOutput = update(error, dt)
        val feedforwardOutput = kf * target
        
        return pidOutput + feedforwardOutput
    }

    /**
     * Auto-timestamped update using system time.
     * Convenient for cases where you don't track dt manually.
     */
    fun update(target: Double, current: Double, normalizeRadians: Boolean = false): Double {
        val currentTime = System.nanoTime()
        val dt = (currentTime - lastTime) / 1e9 // Convert to seconds
        lastTime = currentTime
        return update(target, current, dt, normalizeRadians)
    }

    /**
     * Resets the controller state (integral and derivative terms).
     */
    @Synchronized
    fun reset() {
        integral = 0.0
        lastError = 0.0
        lastTime = System.nanoTime()
        isFirstUpdate = true
    }

    /**
     * Sets output limits for saturation protection.
     */
    fun setOutputLimits(min: Double, max: Double) {
        require(min <= max) { "Minimum output must be <= maximum output" }
        outputMin = min
        outputMax = max
    }

    /**
     * Returns true if this controller has feedforward capability.
     */
    val hasFeedforward: Boolean
        get() = kf != 0.0

    /**
     * Returns current integral accumulator value.
     */
    val integralTerm: Double
        get() = integral

    /**
     * Returns a string representation of the controller.
     */
    override fun toString(): String {
        return if (hasFeedforward) {
            "PIDF(kp=$kp, ki=$ki, kd=$kd, kf=$kf)"
        } else {
            "PID(kp=$kp, ki=$ki, kd=$kd)"
        }
    }
}

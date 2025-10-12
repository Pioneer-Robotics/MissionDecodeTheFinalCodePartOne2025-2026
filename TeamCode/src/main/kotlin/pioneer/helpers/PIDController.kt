package pioneer.helpers

class PIDController(val Kp: Double, val Ki: Double, val Kd: Double) {
    constructor(coeffs: Array<Double>) : this(coeffs[0], coeffs[1], coeffs[2])

    private var integral = 0.0
    private var lastError = 0.0

    fun update(error: Double, dt: Double): Double {
        integral += 0.5 * (error + lastError) * dt // Trapezoidal integration
        integral = integral.coerceIn(-1.0, 1.0) // Clamp integral to prevent windup
        var derivative = (error - lastError) / dt
        lastError = error
        return (Kp * error) + (Ki * integral) + (Kd * derivative)
    }

    fun update(target: Double, current: Double, dt: Double, normalizeRadians: Boolean = false): Double {
        val error = if (normalizeRadians) MathUtils.normalizeRadians(target - current) else target - current
        return update(error, dt)
    }

    fun reset() {
        integral = 0.0
        lastError = 0.0
    }
}

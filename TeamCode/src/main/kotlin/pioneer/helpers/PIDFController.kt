package pioneer.helpers

class PIDFController(val Kp: Double, val Ki: Double, val Kd: Double, val Kf: Double) {
    constructor(coeffs: Array<Double>) : this(coeffs[0], coeffs[1], coeffs[2], coeffs[3])

    private var integral = 0.0
    private var lastError = 0.0

    fun update(target: Double, current: Double, dt: Double): Double {
        val error = target - current
        integral += error * dt
        integral = integral.coerceIn(-1.0, 1.0) // Clamp integral to prevent windup
        val derivative = (error - lastError) / dt
        lastError = error

        return (Kp * error) + (Ki * integral) + (Kd * derivative) + (Kf * target)
    }

    fun reset() {
        integral = 0.0
        lastError = 0.0
    }
}

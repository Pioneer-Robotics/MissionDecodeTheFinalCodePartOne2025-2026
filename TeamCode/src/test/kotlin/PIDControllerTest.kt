import pioneer.helpers.PIDController
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

class PIDControllerTest {
    private val epsilon = 1e-6

    @Test
    fun testConstructorValidation() {
        // Valid construction
        val pid1 = PIDController(kp = 1.0, ki = 0.5, kd = 0.1)
        assertEquals(1.0, pid1.kp, epsilon)
        assertEquals(0.5, pid1.ki, epsilon)
        assertEquals(0.1, pid1.kd, epsilon)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testNegativeKp() {
        PIDController(kp = -1.0, ki = 0.0, kd = 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testNegativeKi() {
        PIDController(kp = 1.0, ki = -0.5, kd = 0.0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testNegativeKd() {
        PIDController(kp = 1.0, ki = 0.0, kd = -0.1)
    }

    @Test
    fun testProportionalOnly() {
        val pid = PIDController(kp = 2.0, ki = 0.0, kd = 0.0)
        val error = 5.0
        val dt = 0.1
        
        val output = pid.update(error, dt)
        assertEquals(10.0, output, epsilon) // 2.0 * 5.0
    }

    @Test
    fun testIntegralAccumulation() {
        val pid = PIDController(kp = 0.0, ki = 1.0, kd = 0.0)
        val error = 5.0
        val dt = 0.1
        
        pid.update(error, dt)
        val output = pid.update(error, dt)
        
        // Integral should accumulate: 5.0 * 0.1 * 2 = 1.0
        assertEquals(1.0, output, epsilon)
    }

    @Test
    fun testIntegralClamping() {
        val pid = PIDController(kp = 0.0, ki = 1.0, kd = 0.0)
        pid.integralClamp = 0.5
        
        val error = 100.0
        val dt = 0.1
        
        // This should cause integral windup, but it should be clamped
        for (i in 0..10) {
            pid.update(error, dt)
        }
        
        val output = pid.update(error, dt)
        // Should be clamped to integralClamp
        assertTrue(abs(output) <= pid.integralClamp)
    }

    @Test
    fun testDerivative() {
        val pid = PIDController(kp = 0.0, ki = 0.0, kd = 1.0)
        val dt = 0.1
        
        pid.update(0.0, dt) // First update
        val output = pid.update(10.0, dt) // Second update with change
        
        // Derivative = (10.0 - 0.0) / 0.1 = 100.0
        assertEquals(100.0, output, epsilon)
    }

    @Test
    fun testFeedforward() {
        val pid = PIDController(kp = 1.0, ki = 0.0, kd = 0.0, kf = 0.5)
        val target = 10.0
        val current = 8.0
        val dt = 0.1
        
        val output = pid.update(target, current, dt)
        
        // P term: 1.0 * (10.0 - 8.0) = 2.0
        // Feedforward: 0.5 * 10.0 = 5.0
        // Total: 7.0
        assertEquals(7.0, output, epsilon)
    }

    @Test
    fun testOutputLimits() {
        val pid = PIDController(kp = 10.0, ki = 0.0, kd = 0.0)
        pid.setOutputLimits(-5.0, 5.0)
        
        val error = 10.0
        val dt = 0.1
        
        val output = pid.update(error, dt)
        
        // Without limits: 10.0 * 10.0 = 100.0
        // With limits: should be clamped to 5.0
        assertEquals(5.0, output, epsilon)
    }

    @Test
    fun testNormalizeRadians() {
        val pid = PIDController(kp = 1.0, ki = 0.0, kd = 0.0)
        val target = 0.1
        val current = 2 * PI - 0.1 // Nearly 360 degrees
        val dt = 0.1
        
        val output = pid.update(target, current, dt, normalizeRadians = true)
        
        // Error should be normalized to ~0.2 instead of ~-6.28
        assertTrue(abs(output) < 1.0)
    }

    @Test
    fun testReset() {
        val pid = PIDController(kp = 1.0, ki = 1.0, kd = 1.0)
        val dt = 0.1
        
        // Build up some state
        pid.update(10.0, dt)
        pid.update(10.0, dt)
        pid.update(10.0, dt)
        
        // Reset
        pid.reset()
        
        // After reset, integral should be 0
        assertEquals(0.0, pid.integralTerm, epsilon)
    }

    @Test
    fun testZeroDeltaTime() {
        val pid = PIDController(kp = 1.0, ki = 1.0, kd = 1.0)
        val output = pid.update(10.0, 0.0)
        assertEquals(0.0, output, epsilon)
    }

    @Test
    fun testHasFeedforward() {
        val pid1 = PIDController(kp = 1.0, ki = 0.0, kd = 0.0, kf = 0.5)
        assertTrue(pid1.hasFeedforward)
        
        val pid2 = PIDController(kp = 1.0, ki = 0.0, kd = 0.0, kf = 0.0)
        assertFalse(pid2.hasFeedforward)
    }

    @Test
    fun testToString() {
        val pid1 = PIDController(kp = 1.0, ki = 0.5, kd = 0.1, kf = 0.2)
        val str1 = pid1.toString()
        assertTrue(str1.contains("PIDF"))
        assertTrue(str1.contains("kp=1.0"))
        
        val pid2 = PIDController(kp = 1.0, ki = 0.5, kd = 0.1)
        val str2 = pid2.toString()
        assertTrue(str2.contains("PID"))
        assertFalse(str2.contains("kf"))
    }

    @Test
    fun testCompleteScenario() {
        // Simulate a robot trying to reach position 100
        val pid = PIDController(kp = 0.5, ki = 0.1, kd = 0.2)
        val dt = 0.02 // 20ms updates
        
        var position = 0.0
        val target = 100.0
        
        for (i in 0..100) {
            val output = pid.update(target, position, dt)
            // Simple integration of output as velocity
            position += output * dt
        }
        
        // Should have moved significantly toward target
        assertTrue(position > 50.0)
    }
}

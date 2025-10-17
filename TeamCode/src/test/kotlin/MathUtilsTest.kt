import pioneer.helpers.MathUtils
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI

class MathUtilsTest {
    private val epsilon = 1e-6

    @Test
    fun testNormalizeRadiansZero() {
        val result = MathUtils.normalizeRadians(0.0)
        assertEquals(0.0, result, epsilon)
    }

    @Test
    fun testNormalizeRadiansPositive() {
        val result = MathUtils.normalizeRadians(PI / 2)
        assertEquals(PI / 2, result, epsilon)
    }

    @Test
    fun testNormalizeRadiansNegative() {
        val result = MathUtils.normalizeRadians(-PI / 2)
        assertEquals(-PI / 2, result, epsilon)
    }

    @Test
    fun testNormalizeRadiansLargePositive() {
        val result = MathUtils.normalizeRadians(3 * PI)
        assertTrue(result > -PI && result <= PI)
        assertEquals(PI, result, epsilon)
    }

    @Test
    fun testNormalizeRadiansLargeNegative() {
        val result = MathUtils.normalizeRadians(-3 * PI)
        assertTrue(result > -PI && result <= PI)
        assertEquals(-PI, result, epsilon)
    }

    @Test
    fun testNormalizeRadiansVeryLarge() {
        val result = MathUtils.normalizeRadians(10 * PI)
        assertTrue(result > -PI && result <= PI)
    }

    @Test
    fun testNormalizeRadiansVerySmall() {
        val result = MathUtils.normalizeRadians(-10 * PI)
        assertTrue(result > -PI && result <= PI)
    }

    @Test
    fun testNormalizeRadiansAtBoundary() {
        // Test at exactly PI
        val resultPI = MathUtils.normalizeRadians(PI)
        assertEquals(PI, resultPI, epsilon)
        
        // Test at exactly -PI (should wrap to PI)
        val resultNegPI = MathUtils.normalizeRadians(-PI)
        // -PI should normalize to PI (upper bound is inclusive)
        assertTrue(resultNegPI <= PI && resultNegPI > -PI)
    }

    @Test
    fun testNormalizeRadiansMultipleRevolutions() {
        // Test 2 full revolutions + PI/4
        val result1 = MathUtils.normalizeRadians(4 * PI + PI / 4)
        assertEquals(PI / 4, result1, epsilon)
        
        // Test -2 full revolutions - PI/4
        val result2 = MathUtils.normalizeRadians(-4 * PI - PI / 4)
        assertEquals(-PI / 4, result2, epsilon)
    }

    @Test
    fun testNormalizeRadiansSymmetry() {
        val angle = PI / 3
        val positive = MathUtils.normalizeRadians(angle)
        val negative = MathUtils.normalizeRadians(-angle)
        
        assertEquals(angle, positive, epsilon)
        assertEquals(-angle, negative, epsilon)
    }

    @Test
    fun testNormalizeRadiansConsecutive() {
        // Normalize the same angle multiple times should give same result
        val angle = 7 * PI / 4
        val result1 = MathUtils.normalizeRadians(angle)
        val result2 = MathUtils.normalizeRadians(result1)
        
        assertEquals(result1, result2, epsilon)
    }
}

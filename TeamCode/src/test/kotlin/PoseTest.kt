import pioneer.localization.Pose
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs

class PoseTest {
    private val epsilon = 1e-6

    @Test
    fun testDefaultConstructor() {
        val pose = Pose()
        assertEquals(0.0, pose.x, epsilon)
        assertEquals(0.0, pose.y, epsilon)
        assertEquals(0.0, pose.vx, epsilon)
        assertEquals(0.0, pose.vy, epsilon)
        assertEquals(0.0, pose.ax, epsilon)
        assertEquals(0.0, pose.ay, epsilon)
        assertEquals(0.0, pose.theta, epsilon)
        assertEquals(0.0, pose.omega, epsilon)
        assertEquals(0.0, pose.alpha, epsilon)
    }

    @Test
    fun testConstructorWithValues() {
        val pose = Pose(x = 10.0, y = 20.0, theta = PI / 4, vx = 5.0, vy = 3.0)
        assertEquals(10.0, pose.x, epsilon)
        assertEquals(20.0, pose.y, epsilon)
        assertEquals(PI / 4, pose.theta, epsilon)
        assertEquals(5.0, pose.vx, epsilon)
        assertEquals(3.0, pose.vy, epsilon)
    }

    @Test
    fun testNormalize() {
        // Test angle wrapping
        val pose1 = Pose(theta = 3 * PI)
        val normalized1 = pose1.normalize()
        assertTrue(abs(normalized1.theta - PI) < epsilon)

        val pose2 = Pose(theta = -3 * PI)
        val normalized2 = pose2.normalize()
        assertTrue(abs(normalized2.theta + PI) < epsilon)

        val pose3 = Pose(theta = 0.5 * PI)
        val normalized3 = pose3.normalize()
        assertEquals(0.5 * PI, normalized3.theta, epsilon)
    }

    @Test
    fun testIntegrate() {
        val pose = Pose(x = 0.0, y = 0.0, vx = 10.0, vy = 5.0, ax = 1.0, ay = 2.0)
        val dt = 0.1
        val integrated = pose.integrate(dt)

        // x = x0 + vx*dt + 0.5*ax*dt^2
        val expectedX = 0.0 + 10.0 * 0.1 + 0.5 * 1.0 * 0.01
        val expectedY = 0.0 + 5.0 * 0.1 + 0.5 * 2.0 * 0.01
        val expectedVx = 10.0 + 1.0 * 0.1
        val expectedVy = 5.0 + 2.0 * 0.1

        assertEquals(expectedX, integrated.x, epsilon)
        assertEquals(expectedY, integrated.y, epsilon)
        assertEquals(expectedVx, integrated.vx, epsilon)
        assertEquals(expectedVy, integrated.vy, epsilon)
    }

    @Test
    fun testGetLength() {
        val pose = Pose(x = 3.0, y = 4.0)
        assertEquals(5.0, pose.getLength(), epsilon) // 3-4-5 triangle
    }

    @Test
    fun testDistanceTo() {
        val pose1 = Pose(x = 0.0, y = 0.0)
        val pose2 = Pose(x = 3.0, y = 4.0)
        assertEquals(5.0, pose1.distanceTo(pose2), epsilon)
        assertEquals(5.0, pose2.distanceTo(pose1), epsilon)
    }

    @Test
    fun testAngleTo() {
        val pose1 = Pose(x = 0.0, y = 0.0)
        val pose2 = Pose(x = 1.0, y = 0.0)
        assertEquals(0.0, pose1.angleTo(pose2), epsilon)

        val pose3 = Pose(x = 0.0, y = 1.0)
        assertEquals(PI / 2, pose1.angleTo(pose3), epsilon)
    }

    @Test
    fun testRotate() {
        // Test rotation around origin
        val pose = Pose(x = 1.0, y = 0.0)
        val rotated = pose.rotate(PI / 2)
        assertEquals(0.0, rotated.x, epsilon)
        assertEquals(1.0, rotated.y, epsilon)
        assertEquals(PI / 2, rotated.theta, epsilon)
    }

    @Test
    fun testRotateAboutPoint() {
        // Test rotation around a specific point
        val pose = Pose(x = 2.0, y = 0.0)
        val origin = Pose(x = 1.0, y = 0.0)
        val rotated = pose.rotate(PI / 2, origin)
        
        assertEquals(1.0, rotated.x, epsilon)
        assertEquals(1.0, rotated.y, epsilon)
    }

    @Test
    fun testRoughlyEquals() {
        val pose1 = Pose(x = 10.0, y = 20.0, theta = 0.0)
        val pose2 = Pose(x = 10.005, y = 20.005, theta = 0.0005)
        
        assertTrue(pose1.roughlyEquals(pose2, positionTolerance = 0.01, angleTolerance = 0.001))
        assertFalse(pose1.roughlyEquals(pose2, positionTolerance = 0.001, angleTolerance = 0.0001))
    }

    @Test
    fun testAddition() {
        val pose1 = Pose(x = 10.0, y = 20.0, vx = 5.0, theta = PI / 4)
        val pose2 = Pose(x = 5.0, y = 10.0, vx = 3.0, theta = PI / 4)
        val result = pose1 + pose2

        assertEquals(15.0, result.x, epsilon)
        assertEquals(30.0, result.y, epsilon)
        assertEquals(8.0, result.vx, epsilon)
        // Theta should be wrapped
        assertTrue(abs(result.theta - PI / 2) < epsilon)
    }

    @Test
    fun testSubtraction() {
        val pose1 = Pose(x = 10.0, y = 20.0, vx = 5.0)
        val pose2 = Pose(x = 5.0, y = 10.0, vx = 3.0)
        val result = pose1 - pose2

        assertEquals(5.0, result.x, epsilon)
        assertEquals(10.0, result.y, epsilon)
        assertEquals(2.0, result.vx, epsilon)
    }

    @Test
    fun testMultiplication() {
        val pose = Pose(x = 10.0, y = 20.0, vx = 5.0)
        val result = pose * 2.0

        assertEquals(20.0, result.x, epsilon)
        assertEquals(40.0, result.y, epsilon)
        assertEquals(10.0, result.vx, epsilon)
    }

    @Test
    fun testDivision() {
        val pose = Pose(x = 10.0, y = 20.0, vx = 4.0)
        val result = pose / 2.0

        assertEquals(5.0, result.x, epsilon)
        assertEquals(10.0, result.y, epsilon)
        assertEquals(2.0, result.vx, epsilon)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testDivisionByZero() {
        val pose = Pose(x = 10.0, y = 20.0)
        pose / 0.0
    }

    @Test
    fun testUnaryMinus() {
        val pose = Pose(x = 10.0, y = 20.0, vx = 5.0, theta = PI / 4)
        val result = -pose

        assertEquals(-10.0, result.x, epsilon)
        assertEquals(-20.0, result.y, epsilon)
        assertEquals(-5.0, result.vx, epsilon)
        assertEquals(-PI / 4, result.theta, epsilon)
    }

    @Test
    fun testDerivative() {
        val pose = Pose(x = 10.0, y = 20.0, vx = 5.0, vy = 3.0, ax = 1.0, ay = 2.0, 
                       theta = PI / 4, omega = 0.5, alpha = 0.1)
        val derivative = pose.derivative()

        assertEquals(5.0, derivative.x, epsilon)
        assertEquals(3.0, derivative.y, epsilon)
        assertEquals(1.0, derivative.vx, epsilon)
        assertEquals(2.0, derivative.vy, epsilon)
        assertEquals(0.5, derivative.theta, epsilon)
        assertEquals(0.1, derivative.omega, epsilon)
    }

    @Test
    fun testToString() {
        val pose = Pose(x = 10.123, y = 20.456, theta = 1.234)
        val str = pose.toString()
        assertTrue(str.contains("10.123"))
        assertTrue(str.contains("20.456"))
        assertTrue(str.contains("1.234"))
    }

    @Test
    fun testToDesmosString() {
        val pose = Pose(x = 10.123, y = 20.456)
        val str = pose.toDesmosString()
        assertTrue(str.contains("10.123"))
        assertTrue(str.contains("20.456"))
        assertTrue(str.startsWith("("))
        assertTrue(str.endsWith(")"))
    }
}

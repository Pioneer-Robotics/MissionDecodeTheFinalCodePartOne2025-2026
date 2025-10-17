import pioneer.localization.Pose
import pioneer.pathing.paths.LinearPath
import pioneer.pathing.paths.HermitePath
import pioneer.pathing.paths.CompoundPath
import org.junit.Assert.*
import org.junit.Test
import kotlin.math.abs

class PathTest {
    private val epsilon = 1e-6

    @Test
    fun testLinearPathLength() {
        val path = LinearPath(Pose(0.0, 0.0), Pose(3.0, 4.0))
        assertEquals(5.0, path.getLength(), epsilon) // 3-4-5 triangle
    }

    @Test
    fun testLinearPathGetPoint() {
        val path = LinearPath(Pose(0.0, 0.0), Pose(10.0, 0.0))
        
        val point0 = path.getPoint(0.0)
        assertEquals(0.0, point0.x, epsilon)
        assertEquals(0.0, point0.y, epsilon)
        
        val point1 = path.getPoint(1.0)
        assertEquals(10.0, point1.x, epsilon)
        assertEquals(0.0, point1.y, epsilon)
        
        val pointMid = path.getPoint(0.5)
        assertEquals(5.0, pointMid.x, epsilon)
        assertEquals(0.0, pointMid.y, epsilon)
    }

    @Test
    fun testLinearPathTangent() {
        val path = LinearPath(Pose(0.0, 0.0), Pose(10.0, 0.0))
        val tangent = path.getTangent(0.5)
        
        assertEquals(1.0, tangent.x, epsilon)
        assertEquals(0.0, tangent.y, epsilon)
    }

    @Test
    fun testLinearPathNormal() {
        val path = LinearPath(Pose(0.0, 0.0), Pose(10.0, 0.0))
        val normal = path.getNormal(0.5)
        
        // Normal should be perpendicular to tangent
        assertEquals(0.0, normal.x, epsilon)
        assertEquals(1.0, normal.y, epsilon)
    }

    @Test
    fun testLinearPathCurvature() {
        val path = LinearPath(Pose(0.0, 0.0), Pose(10.0, 0.0))
        val curvature = path.getCurvature(0.5)
        
        // Linear paths have zero curvature
        assertEquals(0.0, curvature, epsilon)
    }

    @Test
    fun testLinearPathSecondDerivative() {
        val path = LinearPath(Pose(0.0, 0.0), Pose(10.0, 0.0))
        val secondDeriv = path.getSecondDerivative(0.5)
        
        // Linear paths have zero second derivative
        assertEquals(0.0, secondDeriv.x, epsilon)
        assertEquals(0.0, secondDeriv.y, epsilon)
    }

    @Test
    fun testLinearPathClosestPoint() {
        val path = LinearPath(Pose(0.0, 0.0), Pose(10.0, 0.0))
        
        // Point directly above the line
        val testPoint = Pose(5.0, 5.0)
        val t = path.getClosestPointT(testPoint)
        
        assertEquals(0.5, t, epsilon)
    }

    @Test
    fun testLinearPathGetTFromLength() {
        val path = LinearPath(Pose(0.0, 0.0), Pose(10.0, 0.0))
        
        val t = path.getTFromLength(5.0)
        assertEquals(0.5, t, epsilon)
        
        val tFull = path.getTFromLength(10.0)
        assertEquals(1.0, tFull, epsilon)
    }

    @Test
    fun testLinearPathLengthSoFar() {
        val path = LinearPath(Pose(0.0, 0.0), Pose(10.0, 0.0))
        
        val length = path.getLengthSoFar(0.5)
        assertEquals(5.0, length, epsilon)
    }

    @Test
    fun testLinearPathBuilder() {
        val path = LinearPath.Builder()
            .addPoint(Pose(0.0, 0.0))
            .addPoint(Pose(10.0, 0.0))
            .build()
        
        assertTrue(path is LinearPath)
        assertEquals(10.0, path.getLength(), epsilon)
    }

    @Test
    fun testLinearPathBuilderMultiplePoints() {
        val path = LinearPath.Builder()
            .addPoint(Pose(0.0, 0.0))
            .addPoint(Pose(10.0, 0.0))
            .addPoint(Pose(10.0, 10.0))
            .build()
        
        // Should create a CompoundPath
        assertTrue(path is CompoundPath)
        assertEquals(20.0, path.getLength(), epsilon)
    }

    @Test(expected = IllegalArgumentException::class)
    fun testLinearPathBuilderInsufficientPoints() {
        LinearPath.Builder()
            .addPoint(Pose(0.0, 0.0))
            .build()
    }

    @Test
    fun testHermitePathBasic() {
        val path = HermitePath.Builder()
            .addPoint(Pose(0.0, 0.0))
            .addPoint(Pose(100.0, 0.0))
            .build()
        
        assertTrue(path is HermitePath)
        assertTrue(path.getLength() > 0.0)
    }

    @Test
    fun testHermitePathGetPoint() {
        val path = HermitePath.Builder()
            .addPoint(Pose(0.0, 0.0))
            .addPoint(Pose(100.0, 100.0))
            .build()
        
        val startPoint = path.getPoint(0.0)
        assertEquals(0.0, startPoint.x, 1.0)
        assertEquals(0.0, startPoint.y, 1.0)
        
        val endPoint = path.getPoint(1.0)
        assertEquals(100.0, endPoint.x, 1.0)
        assertEquals(100.0, endPoint.y, 1.0)
    }

    @Test
    fun testHermitePathWithVelocities() {
        val path = HermitePath.Builder()
            .addPoint(Pose(0.0, 0.0), Pose(10.0, 0.0))
            .addPoint(Pose(100.0, 0.0), Pose(10.0, 0.0))
            .build()
        
        assertTrue(path is HermitePath)
        assertTrue(path.getLength() > 0.0)
    }

    @Test
    fun testHermitePathTangent() {
        val path = HermitePath.Builder()
            .addPoint(Pose(0.0, 0.0))
            .addPoint(Pose(100.0, 0.0))
            .build()
        
        val tangent = path.getTangent(0.5)
        
        // Tangent should exist
        assertNotNull(tangent)
    }

    @Test
    fun testCompoundPathLength() {
        val path1 = LinearPath(Pose(0.0, 0.0), Pose(10.0, 0.0))
        val path2 = LinearPath(Pose(10.0, 0.0), Pose(10.0, 10.0))
        val compound = CompoundPath(listOf(path1, path2))
        
        assertEquals(20.0, compound.getLength(), epsilon)
    }

    @Test
    fun testCompoundPathGetPoint() {
        val path1 = LinearPath(Pose(0.0, 0.0), Pose(10.0, 0.0))
        val path2 = LinearPath(Pose(10.0, 0.0), Pose(10.0, 10.0))
        val compound = CompoundPath(listOf(path1, path2))
        
        val point0 = compound.getPoint(0.0)
        assertEquals(0.0, point0.x, epsilon)
        
        val point1 = compound.getPoint(1.0)
        assertEquals(10.0, point1.y, epsilon)
    }

    @Test
    fun testPathHeadingInterpolation() {
        val path = LinearPath(Pose(0.0, 0.0, theta = 0.0), 
                             Pose(10.0, 0.0, theta = Math.PI))
        
        val heading0 = path.getHeading(0.0)
        val heading1 = path.getHeading(1.0)
        val headingMid = path.getHeading(0.5)
        
        assertEquals(0.0, heading0, epsilon)
        assertEquals(Math.PI, heading1, epsilon)
        assertTrue(headingMid > 0.0 && headingMid < Math.PI)
    }

    @Test
    fun testPathClosestPoint() {
        val path = LinearPath(Pose(0.0, 0.0), Pose(10.0, 0.0))
        
        val testPoint = Pose(5.0, 5.0)
        val closestPoint = path.getClosestPoint(testPoint)
        
        assertEquals(5.0, closestPoint.x, epsilon)
        assertEquals(0.0, closestPoint.y, epsilon)
    }
}

import pioneer.localization.Pose
import pioneer.pathing.follower.Follower
import pioneer.pathing.paths.HermitePath
import org.junit.Test

class MotionProfileTest {
    @Test
    fun test() {
        val path = HermitePath.Builder()
            .addPoint(Pose(0.0, 0.0))
            .addPoint(Pose(0.0, 100.0))
            .build()
        val follower = Follower()
        follower.path = path
        val motionProfile = follower.motionProfile!!
        val startState = motionProfile.start()
        val endState = motionProfile.end()
        val duration = motionProfile.duration()
        println("Start State: $startState")
        println("End State: $endState")
        println("Duration: $duration")
        for (i in 0..100) {
            val t = i / 100.0 * duration
            val state = motionProfile[t]
            println("$t: $state")
        }
    }

    @Test
    fun followerTest() {
        val path = HermitePath.Builder()
            .addPoint(Pose(0.0, 0.0))
            .addPoint(Pose(0.0, 100.0))
            .build()
        val follower = Follower()
        follower.path = path

        // Simulate update steps every 10ms
        for (i in 0..1000) {
            val targetState = follower.targetState!!
            val pathT = targetState.x / path.getLength()
            val targetPoint = path.getPoint(pathT)
                        val tangent = path.getTangent(pathT).normalize()
            val targetPointSecondDerivative = path.getSecondDerivative(pathT)
            val targetVelocity = targetPointFirstDerivative * targetState.v
            val targetAcceleration = targetPointSecondDerivative * (targetState.v * targetState.v) +
                    targetPointFirstDerivative * targetState.a

            println("Time: ${i * 0.01}s, Target State: $targetState, Target Point: $targetPoint, " +
                    "Target Velocity: $targetVelocity, Target Acceleration: $targetAcceleration")

            if (pathT >= 1.0 - 1e-6) {
                println("Reached end of path")
                break
            }

            // Simulate a delay of 10ms
            Thread.sleep(10)
        }
    }

    @Test
    fun tempTest() {
        val path = HermitePath.Builder()
            .addPoint(Pose(0.0, 0.0))
            .addPoint(Pose(0.0, 100.0))
            .build()

        for (i in 0..100) {
            val t = i / 100.0
            val p = path.getPoint(t)
            val d = path.getTangent(t)
            println("$t, $p, $d")
        }
    }
}

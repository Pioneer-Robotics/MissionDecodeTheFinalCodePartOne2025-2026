package pioneer.pathing.paths

import pioneer.helpers.Polynomial
import pioneer.helpers.Pose
import kotlin.math.pow

/**
 * HermitePath class representing a Hermite curve
 * @param startPose The starting pose of the path
 * @param endPose The ending pose of the path
 * @param startVelocity The starting velocity vector [x,y]
 * @param endVelocity The ending velocity vector [x,y]
 */
class HermitePath(
    override var startPose: Pose,
    override var endPose: Pose,
    startVelocity: Pose = Pose(),
    endVelocity: Pose = Pose(),
) : Path {
    // Hermite basis functions
    private val basis00 = Polynomial(arrayOf(1.0, 0.0, -3.0, 2.0))
    private val basis01 = Polynomial(arrayOf(0.0, 1.0, -2.0, 1.0))
    private val basis10 = Polynomial(arrayOf(0.0, 0.0, 3.0, -2.0))
    private val basis11 = Polynomial(arrayOf(0.0, 0.0, -1.0, 1.0))

    private val xHermite =
        Polynomial.add(
            basis00.vScale(startPose.x),
            basis01.vScale(startVelocity.x),
            basis10.vScale(endPose.x),
            basis11.vScale(endVelocity.x),
        )
    private val yHermite =
        Polynomial.add(
            basis00.vScale(startPose.y),
            basis01.vScale(startVelocity.y),
            basis10.vScale(endPose.y),
            basis11.vScale(endVelocity.y),
        )

    // Compound path for simplified calculations
    private val resolution: Int = 100 // Resolution for the compound path
    val compoundPath: Path = createCompoundPath(resolution)

    override fun getLength(): Double {
        // Use the compound path to estimate the length
        return compoundPath.getLength()
    }

    override fun getLengthSoFar(t: Double): Double {
        // Use the compound path to estimate the length so far
        return compoundPath.getLengthSoFar(t)
    }

    override fun getTFromLength(length: Double): Double {
        // Use the compound path to estimate t from length
        return compoundPath.getTFromLength(length)
    }

    override fun getPoint(t: Double): Pose =
        Pose(
            x = xHermite.eval(t),
            y = yHermite.eval(t),
        )

    override fun getPose(t: Double): Pose =
        Pose(
            x = xHermite.eval(t),
            y = yHermite.eval(t),
            vx = xHermite.nDerEval(t, 1),
            vy = yHermite.nDerEval(t, 1),
            ax = xHermite.nDerEval(t, 2),
            ay = yHermite.nDerEval(t, 2),
        )

    override fun getCurvature(t: Double): Double {
        val xDer = xHermite.derEval(t)
        val yDer = yHermite.derEval(t)
        val xDer2 = xHermite.nDerEval(t, 2) // Second derivative
        val yDer2 = yHermite.nDerEval(t, 2)

        val numerator = xDer * yDer2 - yDer * xDer2
        var denominator = xDer.pow(2.0) + yDer.pow(2.0)
        denominator = denominator.pow(1.5)

        return (numerator / denominator)
    }

    override fun getClosestPointT(position: Pose): Double = compoundPath.getClosestPointT(position)

    fun createCompoundPath(resolution: Int): Path {
        // Create a compound linear path to represent the Hermite path
        val builder = LinearPath.Builder()
        for (i in 0..resolution) {
            val t = i.toDouble() / resolution
            val point = getPoint(t)
            builder.addPoint(point)
        }
        return builder.build()
    }

    class Builder {
        private val points = mutableListOf<Pose>()
        private val velocities = mutableListOf<Pose?>()
        private var t = 0.0 // Cardinal spline tension parameter (higher values = sharper turns)

        fun addPoint(
            point: Pose,
            velocity: Pose? = null,
        ): Builder {
            points.add(point)
            velocities.add(velocity)
            return this
        }

        fun setTension(tension: Double): Builder {
            if (tension !in 0.0..1.0) {
                throw IllegalArgumentException("Tension must be in the range [0, 1]")
            }
            this.t = tension
            return this
        }

        fun build(): Path {
            if (points.size < 2) {
                throw IllegalArgumentException("At least two points are required to create a HermitePath")
            } else if (points.size == 2) {
                val defaultVel = (points[1] - points[0]) * (1.0 - t) // Default start velocity
                return HermitePath(points[0], points[1], velocities[0] ?: defaultVel, velocities[1] ?: defaultVel)
            } else {
                val paths = mutableListOf<HermitePath>()
                for (i in 0..points.size - 2) {
                    val prevPoint = if (i > 0) points[i - 1] else null
                    val startPoint = points[i]
                    val endPoint = points[i + 1]
                    val nextPoint = if (i < points.size - 2) points[i + 2] else null

                    // Use cardinal spline velocities by default
                    val defaultStartVel =
                        when {
                            i > 0 -> (endPoint - prevPoint!!) * (1.0 - t) / 2.0
                            else -> (endPoint - startPoint) * (1.0 - t) // Path endpoint
                        }

                    val defaultEndVel =
                        when {
                            i < points.size - 2 -> (nextPoint!! - startPoint) * (1.0 - t) / 2.0
                            else -> (endPoint - startPoint) * (1.0 - t) // Path endpoint
                        }

                    paths.add(HermitePath(startPoint, endPoint, velocities[i] ?: defaultStartVel, velocities[i + 1] ?: defaultEndVel))
                }
                return CompoundPath(paths)
            }
        }
    }
}

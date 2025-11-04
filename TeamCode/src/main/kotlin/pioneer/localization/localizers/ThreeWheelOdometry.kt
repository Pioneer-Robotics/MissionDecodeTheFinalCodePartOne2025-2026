package pioneer.localization.localizers

import com.qualcomm.robotcore.hardware.HardwareMap
import pioneer.helpers.Pose
import pioneer.localization.Localizer
import kotlin.math.cos
import kotlin.math.sin

/**
 * Three-wheel odometry localizer using two parallel and one perpendicular tracking wheel.
 */
class ThreeWheelOdometry(
    hardwareMap: HardwareMap,
    startPose: Pose = Pose(),
    leftName: String = "odoLeft",
    rightName: String = "odoRight",
    centerName: String = "odoCenter",
    ticksPerRev: Double = 2000.0,
    wheelDiameterCM: Double = 4.8,
    val trackWidthCM: Double = 26.5,
    val forwardOffsetCM: Double = 15.1,
) : Localizer {
    override var pose: Pose = startPose
    override var prevPose: Pose = startPose.copy()

    // Odometry instances
    private val odoLeft = Odometry(hardwareMap, leftName, ticksPerRev, wheelDiameterCM)
    private val odoRight = Odometry(hardwareMap, rightName, ticksPerRev, wheelDiameterCM)
    private val odoCenter = Odometry(hardwareMap, centerName, ticksPerRev, wheelDiameterCM)

    // Previous encoder values
    private var prevLeftTicks = 0
    private var prevRightTicks = 0
    private var prevCenterTicks = 0

    override var encoderXTicks: Int = 0
        get() = (prevRightTicks + prevLeftTicks) / 2

    override var encoderYTicks: Int = 0
        get() = prevCenterTicks

    override fun update(dt: Double) {
        // Get current encoder values
        val dLeftCM = odoLeft.toCentimeters()
        val dRightCM = odoRight.toCentimeters()
        val dCenterCM = odoCenter.toCentimeters()

        // Calculate robot motion
        val dTheta = (dLeftCM - dRightCM) / trackWidthCM
        val forwardDisplacement = (dLeftCM + dRightCM) / 2.0
        val lateralDisplacement = dCenterCM - (forwardOffsetCM * dTheta)

        // Arc motion transformation to global coordinates
        val globalX: Double
        val globalY: Double
        if (dTheta != 0.0) {
            val sinTheta = sin(dTheta)
            val cosTheta = cos(dTheta)
            val invTheta = 1.0 / dTheta
            globalX = sinTheta * invTheta * forwardDisplacement + (cosTheta - 1.0) * invTheta * lateralDisplacement
            globalY = (1.0 - cosTheta) * invTheta * forwardDisplacement + sinTheta * invTheta * lateralDisplacement
        } else {
            globalX = forwardDisplacement
            globalY = lateralDisplacement
        }

        // Transform to world frame and calculate new position
        val sinCurrentTheta = sin(pose.theta)
        val cosCurrentTheta = cos(pose.theta)
        val newX = pose.x + sinCurrentTheta * globalX + cosCurrentTheta * globalY
        val newY = pose.y + cosCurrentTheta * globalX - sinCurrentTheta * globalY
        val newTheta = pose.theta + dTheta

        // Update poses
        prevPose = pose
        pose = Pose(newX, newY, vx = 0.0, vy = 0.0, ax = 0.0, ay = 0.0, theta = newTheta)
    }

    override fun reset(pose: Pose) {
        this.pose = pose
        prevPose = pose.copy()

        // Reset odometry instances
        odoLeft.reset()
        odoRight.reset()
        odoCenter.reset()
    }
}

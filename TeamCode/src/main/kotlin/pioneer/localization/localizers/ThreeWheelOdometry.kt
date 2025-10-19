package pioneer.localization.localizers

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import pioneer.Constants.HardwareNames
import pioneer.helpers.MathUtils
import pioneer.localization.Localizer
import pioneer.localization.Pose
import kotlin.math.cos
import kotlin.math.sin
import pioneer.Constants.Odometry as OdometryConstants

/**
 * Three-wheel odometry localizer using two parallel and one perpendicular tracking wheel.
 */
class ThreeWheelOdometry(
    hardwareMap: HardwareMap,
    startPose: Pose = Pose(),
) : Localizer {
    override var pose: Pose = startPose
    override var prevPose: Pose = startPose.copy()

    // Previous encoder values
    private var prevLeftTicks = 0
    private var prevRightTicks = 0
    private var prevCenterTicks = 0

    // Hardware
    private val odoLeft: DcMotorEx = hardwareMap.get(DcMotorEx::class.java, HardwareNames.ODO_LEFT)
    private val odoRight: DcMotorEx = hardwareMap.get(DcMotorEx::class.java, HardwareNames.ODO_RIGHT)
    private val odoCenter: DcMotorEx = hardwareMap.get(DcMotorEx::class.java, HardwareNames.ODO_CENTER)

    init {
        odoLeft.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        odoRight.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        odoCenter.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
    }

    override fun update(dt: Double) {
        // Get current encoder values
        val curLeftTicks = -odoLeft.currentPosition
        val curRightTicks = -odoRight.currentPosition
        val curCenterTicks = -odoCenter.currentPosition

        // Calculate wheel deltas in cm
        val dLeftCM = (curLeftTicks - prevLeftTicks) * OdometryConstants.TICKS_TO_CM
        val dRightCM = (curRightTicks - prevRightTicks) * OdometryConstants.TICKS_TO_CM
        val dCenterCM = (curCenterTicks - prevCenterTicks) * OdometryConstants.TICKS_TO_CM

        // Calculate robot motion
        val dTheta = (dLeftCM - dRightCM) / OdometryConstants.TRACK_WIDTH_CM
        val forwardDisplacement = (dLeftCM + dRightCM) / 2.0
        val lateralDisplacement = dCenterCM - (OdometryConstants.FORWARD_OFFSET_CM * dTheta)

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
        val newTheta = MathUtils.normalizeRadians(pose.theta + dTheta)

        // Calculate velocities and accelerations
        val vx = (newX - pose.x) / dt
        val vy = (newY - pose.y) / dt
        val omega = dTheta / dt
        val ax = (vx - prevPose.vx) / dt
        val ay = (vy - prevPose.vy) / dt
        val alpha = (omega - prevPose.omega) / dt

        // Update poses
        prevPose = pose
        pose = Pose(newX, newY, vx, vy, ax, ay, newTheta, omega, alpha)

        // Update encoder values
        prevLeftTicks = curLeftTicks
        prevRightTicks = curRightTicks
        prevCenterTicks = curCenterTicks
    }

    override fun reset(pose: Pose) {
        this.pose = pose
        prevPose = pose.copy()

        // Reset encoder values
        prevLeftTicks = 0
        prevRightTicks = 0
        prevCenterTicks = 0

        // Reset hardware encoders
        odoLeft.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        odoRight.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        odoCenter.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
    }
}

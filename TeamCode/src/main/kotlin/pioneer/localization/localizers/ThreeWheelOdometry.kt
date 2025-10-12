package pioneer.localization.localizers

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import pioneer.HardwareNames
import pioneer.helpers.MathUtils
import pioneer.localization.Localizer
import pioneer.localization.Pose
import pioneer.Constants.Odometry as OdometryConstants
import pioneer.Constants.HardwareNames as HardwareNames
import kotlin.math.cos
import kotlin.math.sin

class ThreeWheelOdometry (hardwareMap: HardwareMap, startPose: Pose = Pose()) : Localizer {
    // Current pose, velocity, and acceleration
    override var pose : Pose = startPose
    override var velocity : Pose = Pose()
    override var acceleration : Pose = Pose()

    // Previous encoder values
    private var prevLeftTicks = 0
    private var prevRightTicks = 0
    private var prevCenterTicks = 0

    // Hardware
    private val odoLeft : DcMotorEx = hardwareMap.get(DcMotorEx::class.java, HardwareNames.ODO_LEFT)
    private val odoRight : DcMotorEx = hardwareMap.get(DcMotorEx::class.java, HardwareNames.ODO_RIGHT)
    private val odoCenter : DcMotorEx = hardwareMap.get(DcMotorEx::class.java, HardwareNames.ODO_CENTER)

    init {
        // Reset encoders
        odoLeft.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        odoRight.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        odoCenter.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
    }

    override fun update(deltaTime: Double) {
        // Get encoder values
        val curLeftTicks = -odoLeft.currentPosition
        val curRightTicks = -odoRight.currentPosition
        val curCenterTicks = -odoCenter.currentPosition

        // Calculate deltas
        val dLeftCM = (curLeftTicks - prevLeftTicks) * OdometryConstants.TICKS_TO_CM
        val dRightCM = (curRightTicks - prevRightTicks) * OdometryConstants.TICKS_TO_CM
        val dCenterCM = (curCenterTicks - prevCenterTicks) * OdometryConstants.TICKS_TO_CM
        val dTheta = (dLeftCM - dRightCM) / OdometryConstants.TRACK_WIDTH

        // Displacement in the robot's local frame
        val verticalDisplacement = (dLeftCM + dRightCM) / 2
        val horizontalDisplacement = dCenterCM - (OdometryConstants.FORWARD_OFFSET * dTheta)

        // Update pose, velocity, and acceleration
        var a : Double; var b : Double
        if (dTheta != 0.0) {
            a = (sin(dTheta) / dTheta) * verticalDisplacement + ((cos(dTheta) - 1) / dTheta) * horizontalDisplacement
            b = ((1 - cos(dTheta)) / dTheta) * verticalDisplacement + (sin(dTheta) / dTheta) * horizontalDisplacement
        } else {
            a = verticalDisplacement
            b = horizontalDisplacement
        }

        val newPose = Pose(
            pose.x + sin(pose.heading) * a + cos(pose.heading) * b,
            pose.y + cos(pose.heading) * a - sin(pose.heading) * b,
            MathUtils.normalizeRadians(pose.heading + dTheta)
        )

        val newVelocity = Pose(
            (newPose.x - pose.x) / deltaTime,
            (newPose.y - pose.y) / deltaTime,
            dTheta / deltaTime
        )

        acceleration = Pose(
            (newVelocity.x - velocity.x) / deltaTime,
            (newVelocity.y - velocity.y) / deltaTime,
            (newVelocity.heading - velocity.heading) / deltaTime
        )

        // Assign new pose and velocity
        pose = newPose
        velocity = newVelocity

        // Update previous values
        prevLeftTicks = curLeftTicks
        prevRightTicks = curRightTicks
        prevCenterTicks = curCenterTicks
    }

    override fun reset(pose: Pose) {
        // Reset pose, velocity, and acceleration
        this.pose = pose
        this.velocity = Pose()
        this.acceleration = Pose()

        // Reset previous encoder values
        prevLeftTicks = 0
        prevRightTicks = 0
        prevCenterTicks = 0

        // Reset encoders
        odoLeft.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        odoRight.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        odoCenter.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
    }
}

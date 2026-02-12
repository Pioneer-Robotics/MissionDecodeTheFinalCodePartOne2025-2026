package pioneer.hardware

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.PIDFCoefficients
import org.firstinspires.ftc.vision.apriltag.AprilTagDetection
import pioneer.Constants
import pioneer.decode.GoalTag
import pioneer.helpers.Chrono
import pioneer.helpers.FileLogger
import pioneer.helpers.MathUtils
import pioneer.helpers.PIDController
import pioneer.helpers.Pose
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.atan2

class Turret(
    private val hardwareMap: HardwareMap,
    private val motorName: String = Constants.HardwareNames.TURRET_MOTOR,
    private val motorRange: Pair<Double, Double> = -3 * PI / 2 to PI / 2,
) : HardwareComponent {
    private lateinit var turret: DcMotorEx
    private lateinit var tagTrackPID: PIDController

    private val chrono = Chrono()

    private val ticksPerRadian: Double = Constants.Turret.TICKS_PER_REV / (2 * PI)

    enum class Mode {
        MANUAL,
        AUTO_TRACK,
    }

    var mode: Mode = Mode.MANUAL

    var offsetTicks = 0

    val currentTicks: Int
        get() {
            check(::turret.isInitialized)
            return turret.currentPosition + offsetTicks
        }

    val rawTicks: Int
        get() {
            check(::turret.isInitialized)
            return turret.currentPosition
        }

    val reachedTarget: Boolean
        get() {
            return abs(currentAngle - targetAngle) < Constants.Turret.ANGLE_TOLERANCE_RADIANS
        }

    init {
        require(motorRange.first < motorRange.second) {
            "Motor range must be valid: ${motorRange.first} to ${motorRange.second}"
        }
    }

    override fun init() {
        turret =
            hardwareMap.get(DcMotorEx::class.java, motorName).apply {
                mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
                mode = DcMotor.RunMode.RUN_USING_ENCODER
                zeroPowerBehavior = DcMotor.ZeroPowerBehavior.FLOAT
                direction = DcMotorSimple.Direction.FORWARD
            }

        tagTrackPID = PIDController(
            Constants.Turret.KP,
            Constants.Turret.KI,
            Constants.Turret.KD,
        )
        tagTrackPID.integralClamp = 1.0
    }

    val currentAngle: Double
        get() = currentTicks / ticksPerRadian

    val targetAngle: Double
        get() = (turret.targetPosition + offsetTicks) / ticksPerRadian

    val targetTicks: Int
        get() = turret.targetPosition

    fun resetMotorPosition(resetTicks: Int = 0) {
        turret.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        offsetTicks = resetTicks
        turret.mode = DcMotor.RunMode.RUN_USING_ENCODER
    }

    fun gotoAngle(
        angle: Double,
        power: Double = 0.75,
        overrideRange: Boolean = false
    ) {
        require(power in -1.0..1.0)
        check(::turret.isInitialized)

        val desiredAngle: Double = if (overrideRange) {
            angle
        } else {
            MathUtils.normalizeRadians(angle, motorRange)
        }

        // Logical ticks uses offset
        val logicalTargetTicks =
            (desiredAngle * ticksPerRadian).toInt()
        val rawTargetTicks = logicalTargetTicks - offsetTicks

        with(turret) {
            targetPosition = rawTargetTicks
            mode = DcMotor.RunMode.RUN_TO_POSITION
            this.power = power
        }
    }

    fun autoTrack(
        pose: Pose,
        target: Pose,
    ) {
        val shootPose = pose + Pose(
            x = Constants.Turret.OFFSET * sin(-pose.theta),
            y = Constants.Turret.OFFSET * cos(-pose.theta)
        )

        // General Angle (From robot 0 to target):
        val targetTheta = (shootPose angleTo target)
        val turretTheta = (PI / 2 + targetTheta) - shootPose.theta
        gotoAngle(MathUtils.normalizeRadians(turretTheta), 0.85)
    }

    fun velocityTagTrack(angleToTag: Double, odoPose: Pose, ballSpeed: Double) {
        val robotTheta = odoPose.theta

//        val tagDistance = hypot(detection.ftcPose.x, detection.ftcPose.y)
//        val fieldOffset = Pose(cos(PI/2 + robotTheta), sin(PI/2 + robotTheta)) * tagDistance
//        val tagPosition = when (detection.id) {
//            20 -> GoalTag.BLUE.pose
//            24 -> GoalTag.RED.pose
//            else -> return
//        }
//        val robotPoseTag = tagPosition - fieldOffset
        val ballVector = Pose(vx=cos(angleToTag)*ballSpeed, vy=sin(angleToTag)*ballSpeed)
        val turretVector = ballVector - Pose(vx=odoPose.vx, vy=odoPose.vy)
        val turretAngle = atan2(turretVector.vy,turretVector.vx) + PI/2

        tagTrack(turretAngle)
    }

    fun tagTrack(errorDegrees: Double?) {
        if (errorDegrees == null) {
            turret.power = 0.0
            return
        }

//        val desiredDelta = Math.toRadians(errorDegrees)
//        val rawTarget = currentAngle + desiredDelta
//        // FIXME: For now the turret can't go past the motor range and it can't wrap
////        val legalTarget = MathUtils.normalizeRadians(rawTarget, motorRange)
//        // TODO: Test turret wrapping
////        if (rawTarget !in motorRange.first..motorRange.second) {
////            gotoAngle(rawTarget) // Use goToAngle to wrap
////        }
//        val legalTarget = rawTarget.coerceIn(motorRange.first, motorRange.second)
//        val legalError = legalTarget - currentAngle

//        FileLogger.debug("Turret", "Adjusted Error: $legalError")

        val dt = chrono.dt
        var power = tagTrackPID.update(errorDegrees, dt)

        if (abs(power) > 0.01) {
            power += Constants.Turret.KS * sign(power)
        }

        turret.mode = DcMotor.RunMode.RUN_USING_ENCODER
        turret.power = power.coerceIn(-1.0, 1.0)
    }

    fun tagTrackOld(errorDegrees: Double?) {
        if (errorDegrees == null) {
            turret.power = 0.0
            return
        }

        val desiredDelta = Math.toRadians(errorDegrees)
        val rawTarget = currentAngle + desiredDelta
        // FIXME: For now the turret can't go past the motor range and it can't wrap
//        val legalTarget = MathUtils.normalizeRadians(rawTarget, motorRange)
        // TODO: Test turret wrapping
//        if (rawTarget !in motorRange.first..motorRange.second) {
//            gotoAngle(rawTarget) // Use goToAngle to wrap
//        }
        val legalTarget = rawTarget.coerceIn(motorRange.first, motorRange.second)
        val legalError = legalTarget - currentAngle

        val power = tagTrackPID.update(legalError, chrono.dt)
        val static = if (abs(power) > 0.001) Constants.Turret.KS * sign(power) else 0.0

        turret.mode = DcMotor.RunMode.RUN_USING_ENCODER
        turret.power = power + static
    }


    fun setCustomTarget(pose: Pose, distance: Double): Pose {
        val shootPose = pose + Pose(
            x = Constants.Turret.OFFSET * sin(-pose.theta),
            y = Constants.Turret.OFFSET * cos(-pose.theta)
        ) +
                Pose(pose.vx * Constants.Turret.LAUNCH_TIME, pose.vy * Constants.Turret.LAUNCH_TIME)
        val theta = shootPose.theta + currentAngle
        val targetPose = shootPose + Pose(x = distance * sin(theta), y = distance * -cos(theta))

        return targetPose
    }
}

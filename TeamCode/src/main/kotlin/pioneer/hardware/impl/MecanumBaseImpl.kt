package pioneer.hardware.impl

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import pioneer.Constants.HardwareNames
import pioneer.Constants.Drive as DriveConstants
import pioneer.hardware.interfaces.MecanumBase
import pioneer.helpers.Pose
import kotlin.math.abs
import kotlin.math.sign

class MecanumBaseImpl(hardwareMap: HardwareMap, LF: String, LB: String, RF: String, RB: String) : MecanumBase {
    private val leftFront: DcMotorEx = hardwareMap.get(DcMotorEx::class.java, LF)
    private val leftBack: DcMotorEx = hardwareMap.get(DcMotorEx::class.java, LB)
    private val rightFront: DcMotorEx = hardwareMap.get(DcMotorEx::class.java, RF)
    private val rightBack: DcMotorEx = hardwareMap.get(DcMotorEx::class.java, RB)

    private val motors = arrayOf(leftFront, leftBack, rightFront, rightBack)

    init {
        motors.forEachIndexed { index, motor ->
            motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
            motor.mode = DcMotor.RunMode.RUN_USING_ENCODER
            motor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
            motor.direction = DriveConstants.MOTOR_DIRECTIONS[index]
        }
    }

    override fun setZeroPowerBehavior(behavior: DcMotor.ZeroPowerBehavior) {
        motors.forEach { it.zeroPowerBehavior = behavior }
    }

    /**
     * Drive using robot-centric coordinates: x=strafe, y=forward, rotation=turn
     */
    override fun setDrivePower(
        x: Double,
        y: Double,
        rotation: Double,
        power: Double,
        motor_max_vel_tps: Double = DriveConstants.MOTOR_MAX_VELOCITY_TPS,
    ) {
        val leftFrontPower = y + x + rotation
        val leftBackPower = y - x + rotation
        val rightFrontPower = y - x - rotation
        val rightBackPower = y + x - rotation

        val maxPower = maxOf(abs(leftFrontPower), abs(leftBackPower), abs(rightFrontPower), abs(rightBackPower), 1.0)
        val scale = power / maxPower

        leftFront.velocity = leftFrontPower * scale * max_motor_vel_tps
        leftBack.velocity = leftBackPower * scale * max_motor_vel_tps
        rightFront.velocity = rightFrontPower * scale * max_motor_vel_tps
        rightBack.velocity = rightBackPower * scale * max_motor_vel_tps
    }

    fun setDrivePower(
        pose: Pose,
        power: Double = DriveConstants.DEFAULT_POWER,
    ) {
        setDrivePower(pose.vx, pose.vy, pose.omega, power)
    }

    /**
     * Feedforward control for motion profiling
     */
    override fun setDriveVA(
        pose: Pose,
    ) {
        // Calculate feedforward for each axis
        val ffX = pose.vx * DriveConstants.kV.x + pose.ax * DriveConstants.kA.x +
                  if (abs(pose.vx) > 1e-3) DriveConstants.kS.x * sign(pose.vx) else 0.0
        val ffY = pose.vy * DriveConstants.kV.y + pose.ay * DriveConstants.kA.y +
                  if (abs(pose.vy) > 1e-3) DriveConstants.kS.y * sign(pose.vy) else 0.0
        val ffTheta = pose.omega * DriveConstants.kV.theta + pose.alpha * DriveConstants.kA.theta +
                      if (abs(pose.omega) > 1e-3) DriveConstants.kS.theta * sign(pose.omega) else 0.0

        leftFront.power = (ffY + ffX + ffTheta).coerceIn(-1.0, 1.0)
        leftBack.power = (ffY - ffX + ffTheta).coerceIn(-1.0, 1.0)
        rightFront.power = (ffY - ffX - ffTheta).coerceIn(-1.0, 1.0)
        rightBack.power = (ffY + ffX - ffTheta).coerceIn(-1.0, 1.0)
    }

    override fun stop() {
        motors.forEach { it.power = 0.0 }
    }
}

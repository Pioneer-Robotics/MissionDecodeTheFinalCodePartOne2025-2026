package pioneer.hardware

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import pioneer.Constants
import pioneer.helpers.Pose
import kotlin.math.abs
import kotlin.math.sign

class MecanumBase(
    private val hardwareMap: HardwareMap,
    private val motorConfig: Map<String, DcMotorSimple.Direction> = Constants.Drive.MOTOR_CONFIG,
) : HardwareComponent {
    private lateinit var motors: Map<String, DcMotorEx>

    override fun init() {
        motors =
            motorConfig.mapValues { (name, direction) ->
                hardwareMap.get(DcMotorEx::class.java, name).apply {
                    configureMotor(direction)
                }
            }
    }

    private val leftFront get() = motors.getValue(Constants.HardwareNames.DRIVE_LEFT_FRONT)
    private val leftBack get() = motors.getValue(Constants.HardwareNames.DRIVE_LEFT_BACK)
    private val rightFront get() = motors.getValue(Constants.HardwareNames.DRIVE_RIGHT_FRONT)
    private val rightBack get() = motors.getValue(Constants.HardwareNames.DRIVE_RIGHT_BACK)

    private fun DcMotorEx.configureMotor(direction: DcMotorSimple.Direction) {
        mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        mode = DcMotor.RunMode.RUN_USING_ENCODER
        zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        this.direction = direction
    }

    fun setZeroPowerBehavior(behavior: DcMotor.ZeroPowerBehavior) {
        motors.values.forEach { it.zeroPowerBehavior = behavior }
    }

    /**
     * Drive using robot-centric coordinates: x=strafe, y=forward, rotation=turn
     */
    fun setDrivePower(
        pose: Pose,
        power: Double,
        maxMotorVelocityTps: Double,
    ) {
        val motorPowers = calculateMotorPowers(pose)
        val maxPower = motorPowers.maxOf { abs(it) }.coerceAtLeast(1.0)
        val scale = power / maxPower

        motors.values.forEachIndexed { index, motor ->
            motor.velocity = motorPowers[index] * scale * maxMotorVelocityTps
        }
    }

    /**
     * Feedforward control for motion profiling
     */
    fun setDriveVA(pose: Pose) {
        val ffX = calculateFeedforward(pose.vx, pose.ax, Constants.Drive.kV.x, Constants.Drive.kA.x, Constants.Drive.kS.x)
        val ffY = calculateFeedforward(pose.vy, pose.ay, Constants.Drive.kV.y, Constants.Drive.kA.y, Constants.Drive.kS.y)
        val ffTheta =
            calculateFeedforward(pose.omega, pose.alpha, Constants.Drive.kV.theta, Constants.Drive.kA.theta, Constants.Drive.kS.theta)

//        val ffX = calculateFeedforward(pose.vx, pose.ax, Constants.Drive.kV.x, Constants.Drive.kAX, Constants.Drive.kS.x)
//        val ffY = calculateFeedforward(pose.vy, pose.ay, Constants.Drive.kV.y, Constants.Drive.kAY, Constants.Drive.kS.y)
//        val ffTheta = calculateFeedforward(pose.omega, pose.alpha, Constants.Drive.kV.theta, Constants.Drive.kAT, Constants.Drive.kS.theta)

        val motorPowers = calculateMotorPowers(Pose(vx = ffX, vy = ffY, omega = ffTheta))
        motors.values.forEachIndexed { index, motor ->
            motor.power = motorPowers[index].coerceIn(-1.0, 1.0)
        }
    }

    private fun calculateMotorPowers(pose: Pose): List<Double> {
        val leftFrontPower = pose.vy + pose.vx + pose.omega
        val leftBackPower = pose.vy - pose.vx + pose.omega
        val rightFrontPower = pose.vy - pose.vx - pose.omega
        val rightBackPower = pose.vy + pose.vx - pose.omega
        return listOf(leftFrontPower, leftBackPower, rightFrontPower, rightBackPower)
    }

    private fun calculateFeedforward(
        v: Double,
        a: Double,
        kV: Double,
        kA: Double,
        kS: Double,
    ): Double = v * kV + a * kA + if (abs(v) > 1e-3) kS * sign(v) else 0.0

    fun stop() {
        motors.values.forEach { it.power = 0.0 }
    }
}

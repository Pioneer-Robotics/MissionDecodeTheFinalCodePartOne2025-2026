package pioneer.hardware

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import pioneer.Constants
import pioneer.helpers.Pose
import pioneer.pathing.follower.RobotFeedforward
import kotlin.math.abs

class MecanumBase(
    private val hardwareMap: HardwareMap,
    private val motorConfig: Map<String, DcMotorSimple.Direction> = Constants.Drive.MOTOR_CONFIG,
) : HardwareComponent {
    private lateinit var motors: Map<String, DcMotorEx>

    private val feedforward = RobotFeedforward(
        Constants.Drive.kV.x, Constants.Drive.kA.x,
        Constants.Drive.kV.y, Constants.Drive.kA.y,
        Constants.Drive.kV.theta, Constants.Drive.kA.theta,
        Constants.Drive.kS.x, Constants.Drive.kS.y,
        Constants.Drive.kS.theta,
    )

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
        val ffX = feedforward.calculateX(pose.vx, pose.ax)
        val ffY = feedforward.calculateY(pose.vy, pose.ay)
        val ffTheta = feedforward.calculateTheta(pose.omega, pose.alpha)

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

    fun stop() {
        motors.values.forEach { it.power = 0.0 }
    }
}

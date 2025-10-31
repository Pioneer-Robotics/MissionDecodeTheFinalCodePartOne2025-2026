package pioneer.hardware.impl

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.DcMotorSimple
import pioneer.hardware.base.MecanumBaseBase
import pioneer.helpers.Pose
import pioneer.constants.Drive
import kotlin.math.abs
import kotlin.math.sign

class MecanumBaseImpl(
    hardwareMap: HardwareMap,
    motorConfig: Map<String, DcMotorSimple.Direction> = mapOf(
        "leftFront" to DcMotorSimple.Direction.REVERSE,
        "leftBack" to DcMotorSimple.Direction.REVERSE,
        "rightFront" to DcMotorSimple.Direction.FORWARD,
        "rightBack" to DcMotorSimple.Direction.FORWARD
    )
) : MecanumBaseBase() {
    private val motors = motorConfig.mapValues { (name, direction) ->
        hardwareMap.get(DcMotorEx::class.java, name).apply {
            configureMotor(direction)
        }
    }

    private val leftFront get() = motors.getValue("leftFront")
    private val leftBack get() = motors.getValue("leftBack")
    private val rightFront get() = motors.getValue("rightFront")
    private val rightBack get() = motors.getValue("rightBack")

    private fun DcMotorEx.configureMotor(direction: DcMotorSimple.Direction) {
        mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        mode = DcMotor.RunMode.RUN_USING_ENCODER
        zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        this.direction = direction
    }

    override fun setZeroPowerBehavior(behavior: DcMotor.ZeroPowerBehavior) {
        motors.values.forEach { it.zeroPowerBehavior = behavior }
    }

    /**
     * Drive using robot-centric coordinates: x=strafe, y=forward, rotation=turn
     */
    override fun setDrivePower(
        pose: Pose,
        power: Double,
        maxMotorVelocityTps: Double
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
    override fun setDriveVA(pose: Pose) {
        val ffX = calculateFeedforward(pose.vx, pose.ax, Drive.kV.x, Drive.kA.x, Drive.kS.x)
        val ffY = calculateFeedforward(pose.vy, pose.ay, Drive.kV.y, Drive.kA.y, Drive.kS.y)
        val ffTheta = calculateFeedforward(pose.omega, pose.alpha, Drive.kV.theta, Drive.kA.theta, Drive.kS.theta)

        val motorPowers = calculateMotorPowers(Pose(ffX, ffY, ffTheta))
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

    private fun calculateFeedforward(v: Double, a: Double, kV: Double, kA: Double, kS: Double): Double {
        return v * kV + a * kA + if (abs(v) > 1e-3) kS * sign(v) else 0.0
    }

    override fun stop() {
        motors.forEach { it.power = 0.0 }
    }
}

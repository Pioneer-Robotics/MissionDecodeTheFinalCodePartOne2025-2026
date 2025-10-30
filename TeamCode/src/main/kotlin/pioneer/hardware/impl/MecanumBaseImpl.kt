package pioneer.hardware.impl

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.DcMotorSimple
import pioneer.hardware.interfaces.MecanumBase
import pioneer.helpers.Pose
import pioneer.Constants.Drive as DriveConstants
import kotlin.math.abs
import kotlin.math.sign

class MecanumBaseImpl(
    hardwareMap: HardwareMap,
    motorConfig: Map<String, DcMotorSimple.Direction>
) : MecanumBase {
    private val motors = motorConfig.map { (name, direction) ->
        hardwareMap.get(DcMotorEx::class.java, name).apply {
            mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
            mode = DcMotor.RunMode.RUN_USING_ENCODER
            zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
            this.direction = direction
        }
    }

    private val leftFront: DcMotorEx = motors[0]
    private val leftBack: DcMotorEx = motors[1]
    private val rightFront: DcMotorEx = motors[2]
    private val rightBack: DcMotorEx = motors[3]

    override fun setZeroPowerBehavior(behavior: DcMotor.ZeroPowerBehavior) {
        motors.forEach { it.zeroPowerBehavior = behavior }
    }

    /**
     * Drive using robot-centric coordinates: x=strafe, y=forward, rotation=turn
     */
    override fun setDrivePower(
        pose: Pose,
        power: Double,
        maxMotorVelocityTps: Double
    ) {
        val leftFrontPower = pose.vy + pose.vx + pose.omega
        val leftBackPower = pose.vy - pose.vx + pose.omega
        val rightFrontPower = pose.vy - pose.vx - pose.omega
        val rightBackPower = pose.vy + pose.vx - pose.omega

        val maxPower = maxOf(abs(leftFrontPower), abs(leftBackPower), abs(rightFrontPower), abs(rightBackPower), 1.0)
        val scale = power / maxPower

        leftFront.velocity = leftFrontPower * scale * maxMotorVelocityTps
        leftBack.velocity = leftBackPower * scale * maxMotorVelocityTps
        rightFront.velocity = rightFrontPower * scale * maxMotorVelocityTps
        rightBack.velocity = rightBackPower * scale * maxMotorVelocityTps
    }

    /**
     * Feedforward control for motion profiling
     */
    override fun setDriveVA(
        pose: Pose
    ) {
        // Calculate feedforward for each axis
        val ffX = pose.vx * DriveConstants.kV.x + pose.ax * DriveConstants.kA.x +
                  if (abs(pose.vx) > 1e-3) DriveConstants.kS.x * sign(pose.vx) else 0.0
        val ffY = pose.vy * DriveConstants.kV.y + pose.ay * DriveConstants.kA.y +
                  if (abs(pose.vy) > 1e-3) DriveConstants.kS.y * sign(pose.vy) else 0.0
        val ffTheta = pose.omega * DriveConstants.kV.theta + pose.alpha * DriveConstants.kA.theta +
                      if (abs(pose.omega) > 1e-3) DriveConstants.kS.theta * sign(pose.omega) else 0.0

        leftFront.power = (ffY + ffX - ffTheta).coerceIn(-1.0, 1.0)
        leftBack.power = (ffY - ffX - ffTheta).coerceIn(-1.0, 1.0)
        rightFront.power = (ffY - ffX + ffTheta).coerceIn(-1.0, 1.0)
        rightBack.power = (ffY + ffX + ffTheta).coerceIn(-1.0, 1.0)
    }

    override fun stop() {
        motors.forEach { it.power = 0.0 }
    }
}

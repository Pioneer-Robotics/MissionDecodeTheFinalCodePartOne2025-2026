package pioneer.hardware.drivebase

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import pioneer.Constants.HardwareNames
import pioneer.localization.Pose
import kotlin.math.abs
import kotlin.math.sign
import pioneer.Constants.Drive as DriveConstants

class MecanumBase(
    hardwareMap: HardwareMap,
) {
    private val leftFront: DcMotorEx = hardwareMap.get(DcMotorEx::class.java, HardwareNames.DRIVE_LEFT_FRONT)
    private val leftBack: DcMotorEx = hardwareMap.get(DcMotorEx::class.java, HardwareNames.DRIVE_LEFT_BACK)
    private val rightFront: DcMotorEx = hardwareMap.get(DcMotorEx::class.java, HardwareNames.DRIVE_RIGHT_FRONT)
    private val rightBack: DcMotorEx = hardwareMap.get(DcMotorEx::class.java, HardwareNames.DRIVE_RIGHT_BACK)

    private val motors = arrayOf(leftFront, leftBack, rightFront, rightBack)

    init {
        motors.forEachIndexed { index, motor ->
            motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
            motor.mode = DcMotor.RunMode.RUN_USING_ENCODER
            motor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
            motor.direction = DriveConstants.MOTOR_DIRECTIONS[index]
        }
    }

    fun setZeroPowerBehavior(behavior: DcMotor.ZeroPowerBehavior) {
        motors.forEach { it.zeroPowerBehavior = behavior }
    }

    /**
     * Drive using robot-centric coordinates: x=strafe, y=forward, rotation=turn
     */
    fun setDrivePower(
        x: Double,
        y: Double,
        rotation: Double,
        power: Double = DriveConstants.DEFAULT_DRIVE_POWER,
    ) {
        val leftFrontPower = y + x + rotation
        val leftBackPower = y - x + rotation
        val rightFrontPower = y - x - rotation
        val rightBackPower = y + x - rotation

        val maxPower = maxOf(abs(leftFrontPower), abs(leftBackPower), abs(rightFrontPower), abs(rightBackPower), 1.0)
        val scale = power / maxPower

        leftFront.velocity = leftFrontPower * scale * DriveConstants.MAX_DRIVE_MOTOR_VELOCITY_TPS
        leftBack.velocity = leftBackPower * scale * DriveConstants.MAX_DRIVE_MOTOR_VELOCITY_TPS
        rightFront.velocity = rightFrontPower * scale * DriveConstants.MAX_DRIVE_MOTOR_VELOCITY_TPS
        rightBack.velocity = rightBackPower * scale * DriveConstants.MAX_DRIVE_MOTOR_VELOCITY_TPS
    }

    /**
     * Feedforward control for motion profiling
     */
    fun setDriveVA(
        velocity: Pose,
        acceleration: Pose,
    ) {
        // Calculate feedforward for each axis
        val ffX = velocity.x * DriveConstants.kV.x + acceleration.x * DriveConstants.kA.x +
                  if (abs(velocity.x) > 1e-3) DriveConstants.kS.x * sign(velocity.x) else 0.0
        val ffY = velocity.y * DriveConstants.kV.y + acceleration.y * DriveConstants.kA.y +
                  if (abs(velocity.y) > 1e-3) DriveConstants.kS.y * sign(velocity.y) else 0.0
        val ffTheta = velocity.theta * DriveConstants.kV.theta + acceleration.theta * DriveConstants.kA.theta +
                      if (abs(velocity.theta) > 1e-3) DriveConstants.kS.theta * sign(velocity.theta) else 0.0

        leftFront.power = (ffY + ffX + ffTheta).coerceIn(-1.0, 1.0)
        leftBack.power = (ffY - ffX + ffTheta).coerceIn(-1.0, 1.0)
        rightFront.power = (ffY - ffX - ffTheta).coerceIn(-1.0, 1.0)
        rightBack.power = (ffY + ffX - ffTheta).coerceIn(-1.0, 1.0)
    }

    fun stop() {
        motors.forEach { it.power = 0.0 }
    }
}

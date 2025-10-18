package org.firstinspires.ftc.teamcode.hardware.drivebase

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.teamcode.HardwareNames
import org.firstinspires.ftc.teamcode.localization.Pose
import kotlin.math.*

class MecanumBase (hardwareMap: HardwareMap) {
    // Drive motors
    val leftFront = hardwareMap.get(DcMotorEx::class.java, HardwareNames.DRIVE_LEFT_FRONT)
    val leftBack = hardwareMap.get(DcMotorEx::class.java, HardwareNames.DRIVE_LEFT_BACK)
    val rightFront = hardwareMap.get(DcMotorEx::class.java, HardwareNames.DRIVE_RIGHT_FRONT)
    val rightBack = hardwareMap.get(DcMotorEx::class.java, HardwareNames.DRIVE_RIGHT_BACK)
    val motors = arrayOf(leftFront, leftBack, rightFront, rightBack)

    init {
        // Initialize motors
        for (motor in motors) {
            motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
            motor.mode = DcMotor.RunMode.RUN_USING_ENCODER
            motor.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.BRAKE
        }
        leftFront.direction = MecanumConstants.MOTOR_DIRECTIONS[0]
        leftBack.direction = MecanumConstants.MOTOR_DIRECTIONS[1]
        rightFront.direction = MecanumConstants.MOTOR_DIRECTIONS[2]
        rightBack.direction = MecanumConstants.MOTOR_DIRECTIONS[3]
    }

    /**
     * Sets the zero power behavior of all drive motors.
     */
    fun setZeroPowerBehavior(behavior: DcMotor.ZeroPowerBehavior) {
        for (motor in motors) {
            motor.zeroPowerBehavior = behavior
        }
    }

    /**
     * Sets the power of all drive motors to move in a specific direction.
     * Robot centric (local coordinates) movement is used.
     * @param x The x component of the movement vector (right is positive).
     * @param y The y component of the movement vector (forward is positive).
     * @param rotation The rotation component (counter-clockwise is positive).
     * @param power Scaling factor for the drive power, default is [MecanumConstants.DEFAULT_DRIVE_POWER].
     * @param adjustForStrafe Whether to adjust the x component for strafing inefficiency.
     */
    fun setDrivePower(x: Double, y: Double, rotation: Double, power: Double = MecanumConstants.DEFAULT_DRIVE_POWER, adjustForStrafe: Boolean = false) {
        // Adjust x for strafing if necessary
        val strafeFactor = MecanumConstants.MAX_FORWARD_VELOCITY / MecanumConstants.MAX_HORIZONTAL_VELOCITY
        val adjX = if (adjustForStrafe) x * strafeFactor else x

        val denominator = (abs(y) + abs(adjX) + abs(rotation)).coerceAtLeast(1.0)
        leftFront.velocity = ((y + adjX + rotation) / denominator) * power * MecanumConstants.MAX_DRIVE_MOTOR_VELOCITY
        leftBack.velocity = ((y - adjX + rotation) / denominator) * power * MecanumConstants.MAX_DRIVE_MOTOR_VELOCITY
        rightFront.velocity = ((y - adjX - rotation) / denominator) * power * MecanumConstants.MAX_DRIVE_MOTOR_VELOCITY
        rightBack.velocity = ((y + adjX - rotation) / denominator) * power * MecanumConstants.MAX_DRIVE_MOTOR_VELOCITY
    }

    /**
     * Calculates and sets drive powers using kinematics based on a target velocity and acceleration.
     * Used in conjunction with a motion profile or trajectory following.
     * @param vel The target velocity vector.
     * @param accel The target acceleration vector.
     */
    fun setDriveVA(vel: Pose, accel: Pose) {
        val ff = vel * MecanumConstants.KV + accel * MecanumConstants.KA
        // Add static friction component if velocity is non-zero
        if (abs(vel.y) > 1e-3) ff.y += MecanumConstants.KS.y * sign(vel.y)
        if (abs(vel.x) > 1e-3) ff.x += MecanumConstants.KS.x * sign(vel.x)
        if (abs(vel.heading) > 1e-3) ff.heading += MecanumConstants.KS.heading * sign(vel.heading)

        val motorPowers = arrayOf(
            ff.y + ff.x + ff.heading, // left front
            ff.y - ff.x + ff.heading, // left back
            ff.y - ff.x - ff.heading, // right front
            ff.y + ff.x - ff.heading, // right back
        )

        // Set the motor powers
        for (i in motors.indices) {
            motors[i].power = motorPowers[i].coerceIn(-1.0, 1.0) // Ensure powers are within [-1.0, 1.0]
        }
    }

    /**
     * Sets the power of all drive motors to 0.0
     */
    fun stop() {
        for (motor in motors) {
            motor.power = 0.0
        }
    }
}

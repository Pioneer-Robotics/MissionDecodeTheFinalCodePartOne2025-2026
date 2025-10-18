package org.firstinspires.ftc.teamcode.hardware.interfaces

import com.qualcomm.robotcore.hardware.DcMotor
import org.firstinspires.ftc.teamcode.hardware.constants.MecanumConstants
import org.firstinspires.ftc.teamcode.localization.Pose

interface MecanumBase {
    /**
     * Sets the zero power behavior of all drive motors.
     */
    fun setZeroPowerBehavior(behavior: DcMotor.ZeroPowerBehavior)

    /**
     * Sets the power of all drive motors to move in a specific direction.
     * Robot centric (local coordinates) movement is used.
     * @param x The x component of the movement vector (right is positive).
     * @param y The y component of the movement vector (forward is positive).
     * @param rotation The rotation component (counter-clockwise is positive).
     * @param power Scaling factor for the drive power, default is [MecanumConstants.DEFAULT_DRIVE_POWER].
     * @param adjustForStrafe Whether to adjust the x component for strafing inefficiency.
     */
    fun setDrivePower(x: Double, y: Double, rotation: Double, power: Double = MecanumConstants.DEFAULT_DRIVE_POWER, adjustForStrafe: Boolean = false)

    /**
     * Calculates and sets drive powers using kinematics based on a target velocity and acceleration.
     * Used in conjunction with a motion profile or trajectory following.
     * @param vel The target velocity vector.
     * @param accel The target acceleration vector.
     */
    fun setDriveVA(vel: Pose, accel: Pose)

    /**
     * Sets the power of all drive motors to 0.0
     */
    fun stop()
}
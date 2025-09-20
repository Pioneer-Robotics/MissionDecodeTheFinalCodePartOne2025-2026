package org.firstinspires.ftc.teamcode.pathing.follower

import com.acmerobotics.dashboard.config.Config

@Config
object FollowerConstants{
    /** The threshold in cm to consider the target reached.*/
    const val POSITION_THRESHOLD = 0.5

    /** The threshold in degrees to consider the target reached.*/
    const val ROTATION_THRESHOLD = 0.01

    /** The maximum drive velocity in cm per second. */
    const val MAX_DRIVE_VELOCITY = 100.0

    /** The maximum drive acceleration in cm per second squared. */
    const val MAX_DRIVE_ACCELERATION = 50.0

    /** The maximum centripetal acceleration that the robot can handle in cm/s^2 **/
    const val MAX_CENTRIPETAL_ACCELERATION = (70.0 * 70.0) / 25.0

    // PID coefficients for the trajectory follower.
    @JvmField var PID_X = arrayOf(
        0.0,    // P
        0.0,    // I
        0.0,    // D
    )

    @JvmField var PID_Y = arrayOf(
        0.0,    // P
        0.0,    // I
        0.0,    // D
    )
}

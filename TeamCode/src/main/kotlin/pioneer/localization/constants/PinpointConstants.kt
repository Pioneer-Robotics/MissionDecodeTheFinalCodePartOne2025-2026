package pioneer.localization.constants

import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver

object PinpointConstants{
    /**
     * How far forwards (in mm) from the tracking point the Y (strafe) odometry pod is.
     * Forward increases (when looking from the top of the robot).
     */
    const val Y_POD_OFFSET = 0.0

    /**
     * How far sideways (in mm) from the tracking point the X (forward) odometry pod is.
     * Left increases (when looking from the top of the robot).
     */
    const val X_POD_OFFSET = 134.0

    /** The direction of the Y (strafe) odometry pod. It should increase when moving left. */
    val Y_ENCODER_DIRECTION = GoBildaPinpointDriver.EncoderDirection.FORWARD

    /** The direction of the X (forward) odometry pod. It should increase when moving forward. */
    val X_ENCODER_DIRECTION = GoBildaPinpointDriver.EncoderDirection.FORWARD

    /** The encoder resolution for the odometry pods. */
    val ENCODER_RESOLUTION = GoBildaPinpointDriver.GoBildaOdometryPods.goBILDA_SWINGARM_POD
}

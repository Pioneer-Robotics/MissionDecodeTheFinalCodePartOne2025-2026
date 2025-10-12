package pioneer.localization.constants

object ThreeWheelOdometryConstants {
    /** The distance between the left and right odometry wheels
     * In CM */
    const val TRACK_WIDTH = 26.5 // in cm

    /** The distance between the center odometry wheel and the left/right odometry wheels
     * In CM */
    const val FORWARD_OFFSET = 15.1

    /** Diameter of odometry wheels
     * In CM */
    const val WHEEL_DIAMETER = 4.8 // in cm

    /** Number of ticks per revolution of the odometry wheels
     * In CM */
    const val TICKS_PER_REV = 2000

    /** Multiplier to convert ticks to cm */
    const val TICKS_TO_CM = (WHEEL_DIAMETER * Math.PI) / TICKS_PER_REV
}

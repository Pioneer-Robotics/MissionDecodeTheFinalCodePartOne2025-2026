package pioneer.localization

import pioneer.helpers.Pose

interface Localizer {
    /** Current pose of the robot */
    var pose: Pose

    /** Previous pose for numerical differentiation */
    var prevPose: Pose

    var encoderXTicks: Int
    var encoderYTicks: Int

    /**
     * Updates the pose of the robot based on sensor data
     * @param dt The time since the last update in seconds
     */
    fun update(dt: Double)

    /**
     * Resets the localizer to a specific pose
     * @param pose The pose to reset to
     */
    fun reset(pose: Pose)

    /**
     * Resets the localizer to the origin (0, 0, 0)
     */
    fun reset() {
        reset(Pose())
    }
}

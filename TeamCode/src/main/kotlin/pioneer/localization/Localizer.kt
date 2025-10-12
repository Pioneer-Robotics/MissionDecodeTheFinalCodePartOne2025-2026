package pioneer.localization

interface Localizer {
    /** Current pose of the robot */
    var pose: Pose
    
    /** Previous pose for numerical differentiation */
    var prevPose: Pose

    /**
     * Updates the pose of the robot based on sensor data
     * @param deltaTime The time since the last update in seconds
     */
    fun update(deltaTime: Double)

    /**
     * Resets the localizer to a specific pose
     * @param pose The pose to reset to
     */
    fun reset(pose: Pose)

    /**
     * Resets the localizer to the origin (0, 0, 0)
     */
    fun reset() { reset(Pose()) }
}

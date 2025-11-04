package pioneer.localization.localizers

import pioneer.helpers.FileLogger
import pioneer.helpers.Pose
import pioneer.localization.Localizer

class LocalizerMock : Localizer {
    override var pose: Pose = Pose()
        get() {
            FileLogger.warn("NOT INITIALIZED", "pose getter called on LocalizerMock")
            return field
        }

    override var prevPose: Pose = Pose()
        get() {
            FileLogger.warn("NOT INITIALIZED", "prevPose getter called on LocalizerMock")
            return field
        }

    override var encoderXTicks: Int = 0
        get() {
            FileLogger.warn("NOT INITIALIZED", "encoderXTicks getter called on LocalizerMock")
            return field
        }

    override var encoderYTicks: Int = 0
        get() {
            FileLogger.warn("NOT INITIALIZED", "encoderYTicks getter called on LocalizerMock")
            return field
        }

    override fun update(dt: Double) {
        FileLogger.warn("NOT INITIALIZED", "update called on LocalizerMock")
    }

    override fun reset(pose: Pose) {
        FileLogger.warn("NOT INITIALIZED", "reset called on LocalizerMock")
    }
}

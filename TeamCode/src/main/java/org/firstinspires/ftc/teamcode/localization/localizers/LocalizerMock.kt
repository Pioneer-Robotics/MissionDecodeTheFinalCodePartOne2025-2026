package org.firstinspires.ftc.teamcode.localization.localizers

import org.firstinspires.ftc.teamcode.helpers.FileLogger
import org.firstinspires.ftc.teamcode.localization.Localizer
import org.firstinspires.ftc.teamcode.localization.Pose

class LocalizerMock : Localizer {
    override val pose: Pose
        get() {
            FileLogger.warn("Localizer", "Localizer not initialized")
            return Pose()
        }
    override val velocity: Pose
        get() {
            FileLogger.warn("Localizer", "Localizer not initialized")
            return Pose()
        }
    override val acceleration: Pose
        get() {
            FileLogger.warn("Localizer", "Localizer not initialized")
            return Pose()
        }

    override fun update(deltaTime: Double) {
        FileLogger.warn("Localizer", "Localizer not initialized")
    }

    override fun reset(pose: Pose) {
        FileLogger.warn("Localizer", "Localizer not initialized")
    }
}
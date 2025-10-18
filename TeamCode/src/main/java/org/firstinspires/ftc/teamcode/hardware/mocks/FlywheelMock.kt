package org.firstinspires.ftc.teamcode.hardware.mocks

import org.firstinspires.ftc.teamcode.hardware.Flywheel
import org.firstinspires.ftc.teamcode.helpers.FileLogger

class FlywheelMock : Flywheel {
    override fun setSpeed(velocity: Double) {
        FileLogger.warn("Flywheel", "Flywheel not initialized")
    }
}

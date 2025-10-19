package pioneer.hardware.mock

import pioneer.hardware.interfaces.Flywheel
import pioneer.helpers.FileLogger

class FlywheelMock : Flywheel {
    override fun setSpeed(velocity: Double) {
        FileLogger.warn("NOT INITIALIZED", "setSpeed called on FlywheelMock")
    }
}

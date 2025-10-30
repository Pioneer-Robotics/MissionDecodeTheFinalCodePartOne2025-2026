package pioneer.hardware.mock

import pioneer.hardware.interfaces.Flywheel
import pioneer.helpers.FileLogger

class FlywheelMock : Flywheel {
    override val velocity: Double
        get() {
            FileLogger.warn("NOT INITIALIZED", "velocity getter called on FlywheelMock")
            return 0.0
        }

    override fun setSpeed(velocity: Double) {
        FileLogger.warn("NOT INITIALIZED", "setSpeed called on FlywheelMock")
    }
}

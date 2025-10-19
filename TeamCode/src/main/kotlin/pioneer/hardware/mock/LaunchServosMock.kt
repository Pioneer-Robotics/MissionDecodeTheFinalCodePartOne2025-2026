package pioneer.hardware.mock

import pioneer.hardware.interfaces.LaunchServos
import pioneer.helpers.FileLogger

class LaunchServosMock : LaunchServos {
    override fun triggerLaunch() {
        FileLogger.warn("NOT INITIALIZED", "triggerLaunch called on LaunchServosMock")
    }

    override fun triggerRetract() {
        FileLogger.warn("NOT INITIALIZED", "triggerRetract called on LaunchServosMock")
    }

    override fun update() {
        FileLogger.warn("NOT INITIALIZED", "update called on LaunchServosMock")
    }
}

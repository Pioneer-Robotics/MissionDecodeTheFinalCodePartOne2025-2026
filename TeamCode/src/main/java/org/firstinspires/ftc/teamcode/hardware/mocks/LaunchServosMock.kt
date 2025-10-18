package org.firstinspires.ftc.teamcode.hardware.mocks

import org.firstinspires.ftc.teamcode.hardware.interfaces.LaunchServos
import org.firstinspires.ftc.teamcode.helpers.FileLogger

class LaunchServosMock : LaunchServos {
    override fun triggerLaunch() {
        FileLogger.warn("LaunchServos", "Launch servos not initialized")
    }

    override fun triggerRetract() {
        FileLogger.warn("LaunchServos", "Launch servos not initialized")
    }

    override fun update() {
        FileLogger.warn("LaunchServos", "Launch servos not initialized")
    }
}
package org.firstinspires.ftc.teamcode.hardware.mocks

import org.firstinspires.ftc.teamcode.hardware.interfaces.VoltageHandler
import org.firstinspires.ftc.teamcode.helpers.FileLogger

class VoltageHandlerMock : VoltageHandler {
    override fun getVoltage(): Double {
        FileLogger.warn("Voltage Handler", "Voltage Handler not initialized")
        return -1.0
    }
}

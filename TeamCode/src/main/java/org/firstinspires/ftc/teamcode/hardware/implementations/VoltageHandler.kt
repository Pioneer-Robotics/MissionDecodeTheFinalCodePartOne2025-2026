package org.firstinspires.ftc.teamcode.hardware.implementations

import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.teamcode.hardware.interfaces.VoltageHandler

class VoltageHandlerImpl(hardwareMap: HardwareMap) : VoltageHandler{

    private val voltageSensors = hardwareMap.voltageSensor

    override fun getVoltage(): Double {
        var lowestVoltage = Double.POSITIVE_INFINITY
        for (sensor in voltageSensors) {
            val voltage = sensor.voltage
            if (voltage > 0 && voltage < lowestVoltage) {
                lowestVoltage = voltage
            }
        }
        return lowestVoltage
    }
}

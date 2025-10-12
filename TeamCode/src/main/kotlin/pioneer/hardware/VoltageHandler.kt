package pioneer.hardware

import com.qualcomm.robotcore.hardware.HardwareMap

class VoltageHandler(hardwareMap: HardwareMap) {

    private val voltageSensors = hardwareMap.voltageSensor

    fun getVoltage(): Double {
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
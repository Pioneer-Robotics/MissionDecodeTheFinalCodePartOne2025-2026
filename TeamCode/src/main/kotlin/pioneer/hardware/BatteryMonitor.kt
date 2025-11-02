package pioneer.hardware

import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.VoltageSensor

/**
 * Monitors battery health from all available voltage sensors.
 * Provides battery voltage information for power management and telemetry.
 */
class BatteryMonitor(
    hardwareMap: HardwareMap,
) {
    private val voltageSensors: List<VoltageSensor> = hardwareMap.voltageSensor.toList()

    /**
     * Gets the lowest voltage reading from all available sensors.
     * @return Minimum voltage in volts, or 0.0 if no valid readings
     */
    fun getVoltage(): Double =
        voltageSensors
            .mapNotNull { sensor -> sensor.voltage.takeIf { it > 0.0 } }
            .minOrNull() ?: 0.0

    /**
     * Gets the highest voltage reading from all available sensors.
     * @return Maximum voltage in volts, or 0.0 if no valid readings
     */
    fun getMaxVoltage(): Double =
        voltageSensors
            .mapNotNull { sensor -> sensor.voltage.takeIf { it > 0.0 } }
            .maxOrNull() ?: 0.0

    /**
     * Gets average voltage from all valid sensors.
     * @return Average voltage in volts, or 0.0 if no valid readings
     */
    fun getAverageVoltage(): Double {
        val validVoltages =
            voltageSensors
                .mapNotNull { sensor -> sensor.voltage.takeIf { it > 0.0 } }

        return if (validVoltages.isNotEmpty()) {
            validVoltages.average()
        } else {
            0.0
        }
    }

    /**
     * Checks if battery voltage is critically low.
     * @param threshold Minimum acceptable voltage (default: 11.0V)
     * @return True if voltage is below threshold
     */
    fun isVoltageLow(threshold: Double): Boolean = getVoltage() < threshold

    /**
     * Gets all valid voltage readings for debugging.
     * @return List of valid voltage readings
     */
    fun getAllVoltages(): List<Double> =
        voltageSensors
            .mapNotNull { sensor -> sensor.voltage.takeIf { it > 0.0 } }
}

package pioneer.hardware

import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.VoltageSensor

/**
 * Monitors battery health from all available voltage sensors.
 * Provides battery voltage information for power management and telemetry.
 */
class BatteryMonitor(
    private val hardwareMap: HardwareMap,
) : HardwareComponent {
    private lateinit var voltageSensors: List<VoltageSensor>

    override fun init() {
        voltageSensors = hardwareMap.voltageSensor.toList()
    }

    /**
     * Minimum voltage in volts, or 0.0 if no valid readings
     */
    val voltage: Double
        get() {
            return voltageSensors
                .mapNotNull { sensor -> sensor.voltage.takeIf { it > 0.0 } }
                .minOrNull() ?: 0.0
        }

    /**
     * Maximum voltage in volts, or 0.0 if no valid readings
     */
    val maxVoltage: Double
        get() {
            return voltageSensors
                .mapNotNull { sensor -> sensor.voltage.takeIf { it > 0.0 } }
                .maxOrNull() ?: 0.0
        }

    /**
     * Average voltage in volts, or 0.0 if no valid readings
     */
    val averageVoltage: Double
        get() {
            val validVoltages =
                voltageSensors
                    .mapNotNull { sensor -> sensor.voltage.takeIf { it > 0.0 } }
            return if (validVoltages.isNotEmpty()) validVoltages.average() else 0.0
        }

    /**
     * List of valid voltage readings
     */
    val allVoltages: List<Double>
        get() {
            return voltageSensors
                .mapNotNull { sensor -> sensor.voltage.takeIf { it > 0.0 } }
        }

    /**
     * Checks if battery voltage is critically low.
     * @param threshold Minimum acceptable voltage (default: 11.0V)
     * @return True if voltage is below threshold
     */
    fun isVoltageLow(threshold: Double): Boolean = voltage < threshold
}

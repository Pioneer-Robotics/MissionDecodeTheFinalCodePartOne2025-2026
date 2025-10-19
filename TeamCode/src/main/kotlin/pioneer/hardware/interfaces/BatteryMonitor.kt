package pioneer.hardware.interfaces

interface BatteryMonitor {
    fun getVoltage(): Double

    fun getMaxVoltage(): Double

    fun getAverageVoltage(): Double

    fun isVoltageLow(threshold: Double = 11.0): Boolean

    fun getAllVoltages(): List<Double>
}

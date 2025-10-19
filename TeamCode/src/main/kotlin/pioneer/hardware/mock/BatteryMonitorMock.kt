package pioneer.hardware.mock

import pioneer.hardware.interfaces.BatteryMonitor
import pioneer.helpers.FileLogger

class BatteryMonitorMock : BatteryMonitor {
    override fun getVoltage(): Double {
        FileLogger.warn("NOT INITIALIZED", "getVoltage called on BatteryMonitorMock")
        return -1.0
    }

    override fun getMaxVoltage(): Double {
        FileLogger.warn("NOT INITIALIZED", "getMaxVoltage called on BatteryMonitorMock")
        return -1.0
    }

    override fun getAverageVoltage(): Double {
        FileLogger.warn("NOT INITIALIZED", "getAverageVoltage called on BatteryMonitorMock")
        return -1.0
    }

    override fun isVoltageLow(threshold: Double): Boolean {
        FileLogger.warn("NOT INITIALIZED", "isVoltageLow called on BatteryMonitorMock")
        return false
    }

    override fun getAllVoltages(): List<Double> {
        FileLogger.warn("NOT INITIALIZED", "getAllVoltages called on BatteryMonitorMock")
        return emptyList()
    }
}

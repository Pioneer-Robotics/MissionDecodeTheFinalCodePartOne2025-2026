package pioneer.hardware.base

import pioneer.helpers.FileLogger

open class BatteryMonitorBase {
    open fun getVoltage(): Double {
        FileLogger.warn("NOT INITIALIZED", "getVoltage called on BatteryMonitorBase")
        return -1.0
    }

    open fun getMaxVoltage(): Double {
        FileLogger.warn("NOT INITIALIZED", "getMaxVoltage called on BatteryMonitorBase")
        return -1.0
    }

    open fun getAverageVoltage(): Double {
        FileLogger.warn("NOT INITIALIZED", "getAverageVoltage called on BatteryMonitorBase")
        return -1.0
    }

    open fun isVoltageLow(threshold: Double = 11.0): Boolean {
        FileLogger.warn("NOT INITIALIZED", "isVoltageLow called on BatteryMonitorBase")
        return false
    }

    open fun getAllVoltages(): List<Double> {
        FileLogger.warn("NOT INITIALIZED", "getAllVoltages called on BatteryMonitorBase")
        return emptyList()
    }
}

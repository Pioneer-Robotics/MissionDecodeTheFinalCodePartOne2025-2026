package pioneer.hardware.base

import pioneer.helpers.FileLogger

open class MecanumBaseBase {
    open fun setZeroPowerBehavior() {
        FileLogger.warn("NOT INITIALIZED", "setZeroPowerBehavior called on MecanumBaseBase")
    }

    open fun setDrivePower() {
        FileLogger.warn("NOT INITIALIZED", "setDrivePower called on MecanumBaseBase")
    }

    open fun setDriveVA() {
        FileLogger.warn("NOT INITIALIZED", "setDriveVA called on MecanumBaseBase")
    }

    open fun stop() {
        FileLogger.warn("NOT INITIALIZED", "stop called on MecanumBaseBase")
    }
}

package pioneer.hardware.base

import pioneer.helpers.FileLogger

open class AprilTagBase {
    open val processor: String
        get() {
            FileLogger.warn("NOT INITIALIZED", "processor getter called on AprilTagBase")
            return "NOT_INITIALIZED"
        }
}

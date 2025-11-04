package pioneer.helpers

import android.util.Log
import org.firstinspires.ftc.robotcore.external.Telemetry

/** Class used to update both the phone telemetry and the dashboard telemetry.
 * Also logs telemetry to Logcat */
class TelemetryInterface(val phone: Telemetry, val dashboard: Telemetry) {
    private val TAG = "Telemetry"
    var enableDashboard = true

    fun addData(
        name: String,
        data: Any,
        useDashboard: Boolean? = null,
    ) {
        phone.addData(name, data)
        if (useDashboard == null) {
            if (enableDashboard) addDashboardData(name, data)
        } else if (useDashboard) {
            addDashboardData(name, data)
        }
        FileLogger.debug(TAG, "$name: $data")
        Log.d(TAG, "$name: $data")
    }

    fun addLine(
        line: String,
        useDashboard: Boolean? = null,
    ) {
        phone.addLine(line)
        if (useDashboard == null) {
            if (enableDashboard) dashboard.addLine(line)
        } else if (useDashboard) {
            dashboard.addLine(line)
        }
        FileLogger.debug(TAG, line)
        Log.d(TAG, line)
    }

    fun update() {
        dashboard.update()
        phone.update()
    }

    private fun addDashboardData(
        name: String,
        data: Any,
    ) {
        // Convert Pose objects to separate X Y and Theta (heading) for graphing
        if (data is Pose) {
            dashboard.addData("$name X", "%.3f".format(data.x))
            dashboard.addData("$name Y", "%.3f".format(data.y))
            dashboard.addData("$name Heading", "%.3f".format(data.theta))
        } else {
            dashboard.addData(name, data)
        }
    }
}

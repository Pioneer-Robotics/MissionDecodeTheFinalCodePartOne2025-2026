package pioneer.opmodes.other

import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.helpers.FileLogger

@Disabled
@TeleOp(name = "Clear Logs", group = "Utils")
class ClearLogs : OpMode() {
    override fun init() {
        telemetry.addLine("Press start to clear logs.")
        telemetry.update()
    }

    override fun loop() {
        FileLogger.clearLogs() // Clear all logs
        terminateOpModeNow() // Stop the op mode
    }
}

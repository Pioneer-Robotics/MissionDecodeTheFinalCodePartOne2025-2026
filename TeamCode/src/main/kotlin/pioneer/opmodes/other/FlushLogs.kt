package pioneer.opmodes.other

import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.helpers.FileLogger

@Disabled
@TeleOp(name = "Flush Logs", group = "Utils")
class FlushLogs : OpMode() {
    override fun init() {
        telemetry.addLine("Press start to manually flush logs.")
        telemetry.update()
    }

    override fun loop() {
        FileLogger.flush() // Flush all logs
        terminateOpModeNow() // Stop the op mode
    }
}

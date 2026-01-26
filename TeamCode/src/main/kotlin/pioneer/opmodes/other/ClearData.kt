package pioneer.opmodes.other

import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.Constants
import pioneer.helpers.FileLogger

@TeleOp(name = "Clear Data", group = "Utils")
class ClearData : OpMode() {
    override fun init() {
        telemetry.addLine("Press start to manually clear all transfer data.")
        telemetry.update()
    }

    override fun loop() {
        Constants.TransferData.reset() // Reset data
        terminateOpModeNow() // Stop the op mode
    }
}

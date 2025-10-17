package pioneer.opmodes.teleop

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.Bot
import pioneer.helpers.DashboardPlotter
import pioneer.opmodes.teleop.drivers.*

@TeleOp(name = "Teleop")
class Teleop : OpMode() {
    private lateinit var driver1: TeleopDriver1
    private lateinit var driver2: TeleopDriver2

    private val dashboard = FtcDashboard.getInstance()

    override fun init() {
        Bot.initialize(hardwareMap, telemetry)
        driver1 = TeleopDriver1(gamepad1)
        driver2 = TeleopDriver2(gamepad2)
    }

    override fun loop() {
        // Update bot
        Bot.update()

        // Update gamepad inputs
        driver1.update()
        driver2.update()

        // Update telemetry
        updateTelemetry()
    }

    private fun updateTelemetry() {
        telemetry.addData("Drive Speed", driver1.driveSpeed)
        telemetry.addData("Field Centric", driver1.fieldCentric)
        telemetry.addData("Pose", Bot.localizer.pose)
        telemetry.addData("Velocity", "vx: %.2f, vy: %.2f".format(Bot.localizer.pose.vx, Bot.localizer.pose.vy))
        telemetry.addData("Voltage", Bot.batteryMonitor.getVoltage())
        telemetry.update()

        DashboardPlotter.plotBotPosition(Bot.telemetryPacket, Bot.localizer.pose)
        Bot.sendTelemetryPacket()
    }

    override fun stop() {
        Bot.stop()
    }
}

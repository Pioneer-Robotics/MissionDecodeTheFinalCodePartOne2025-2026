package org.firstinspires.ftc.teamcode.opmodes.teleop

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.GoBildaStarterBot
import org.firstinspires.ftc.teamcode.helpers.DashboardPlotter
import org.firstinspires.ftc.teamcode.opmodes.teleop.drivers.*

@TeleOp(name = "Teleop")
class Teleop : OpMode() {
    private lateinit var driver1: TeleopDriver1
    private lateinit var driver2: TeleopDriver2

    private val dashboard = FtcDashboard.getInstance()

    override fun init() {
        GoBildaStarterBot.initialize(hardwareMap, telemetry)
        driver1 = TeleopDriver1(gamepad1)
        driver2 = TeleopDriver2(gamepad2)
    }

    override fun loop() {
        // Update bot
        GoBildaStarterBot.update()

        // Update gamepad inputs
        driver1.update()
        driver2.update()

        // Update telemetry
        updateTelemetry()
    }

    private fun updateTelemetry() {
        telemetry.addData("Drive Speed", driver1.driveSpeed)
        telemetry.addData("Field Centric", driver1.fieldCentric)
        telemetry.addData("Voltage", GoBildaStarterBot.voltageHandler.getVoltage())
        telemetry.update()
    }

    override fun stop() {
        GoBildaStarterBot.stop()
    }
}

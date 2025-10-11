package org.firstinspires.ftc.teamcode.opmodes.teleop

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.teamcode.GoBildaStarterBot
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
        updateApril()
        updateTelemetry()
    }

    private fun updateTelemetry() {
        telemetry.addData("Drive Speed", driver1.driveSpeed)
        telemetry.addData("Field Centric", driver1.fieldCentric)
        telemetry.addData("Voltage", GoBildaStarterBot.voltageHandler.getVoltage())
        telemetry.update()
    }

    private fun updateApril() {
        val detections = GoBildaStarterBot.aprilTagProcessor.aprilTag.detections
        for (detection in detections) {
            // FIXME: If the processor loses the AprilTag during this loop, a null pointer error is thrown
            telemetry.addData("Detection", detection.id)
            telemetry.addLine("--Rel (x, y, z): (%.2f, %.2f, %.2f)".format(detection.ftcPose.x,detection.ftcPose.y,detection.ftcPose.z))
            telemetry.addLine("--Rel (Y, P, R): (%.2f, %.2f, %.2f)".format(detection.ftcPose.yaw,detection.ftcPose.pitch,detection.ftcPose.roll))
            telemetry.addLine("--Rel (R, B, E): (%.2f, %.2f, %.2f)".format(detection.ftcPose.range,detection.ftcPose.bearing,detection.ftcPose.elevation))
        }
    }

    override fun stop() {
        GoBildaStarterBot.stop()
    }
}

package pioneer.opmodes.teleop

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.Bot
import pioneer.BotType
import pioneer.helpers.DashboardPlotter
import pioneer.helpers.FileLogger
import pioneer.opmodes.teleop.drivers.*

@TeleOp(name = "Teleop")
class Teleop : OpMode() {
    private lateinit var bot: Bot

    private lateinit var driver1: TeleopDriver1
    private lateinit var driver2: TeleopDriver2

    private var telemetryPacket = TelemetryPacket()

    override fun init() {
        bot = Bot(BotType.GOBILDA_STARTER_BOT, hardwareMap)
        driver1 = TeleopDriver1(gamepad1, bot)
        driver2 = TeleopDriver2(gamepad2, bot)
    }

    override fun loop() {
        // Update bot
        bot.dtTracker.update()
        bot.localizer.update(bot.dtTracker.dt)

        // Update gamepad inputs
        driver1.update()
        driver2.update()

        // Update telemetry
        updateApril()
        updateTelemetry()
    }

    private fun updateApril() {
        val detections = bot.aprilTagProcessor.getDetections()
        for (detection in detections) {
            // Check if tag or its properties are null to avoid null pointer exceptions
            if (detection != null && detection.ftcPose != null) {
                telemetry.addData("Detection", detection.id)
                telemetry.addLine("--Rel (x, y, z): (%.2f, %.2f, %.2f)".format(detection.ftcPose.x, detection.ftcPose.y, detection.ftcPose.z))
                telemetry.addLine("--Rel (Y, P, R): (%.2f, %.2f, %.2f)".format(detection.ftcPose.yaw, detection.ftcPose.pitch, detection.ftcPose.roll))
                telemetry.addLine("--Rel (R, B, E): (%.2f, %.2f, %.2f)".format(detection.ftcPose.range, detection.ftcPose.bearing, detection.ftcPose.elevation))
            } else {
                telemetry.addLine("No valid AprilTag detections.")
            }
        }
    }

    private fun updateTelemetry() {
        telemetry.addData("Drive Speed", driver1.driveSpeed)
        telemetry.addData("Field Centric", driver1.fieldCentric)
        telemetry.addData("Pose", bot.localizer.pose)
        telemetry.addData("Velocity", "vx: %.2f, vy: %.2f".format(bot.localizer.pose.vx, bot.localizer.pose.vy))
        telemetry.addData("Voltage", bot.batteryMonitor.getVoltage())
        telemetry.update()

        DashboardPlotter.plotBotPosition(telemetryPacket, bot.localizer.pose)
        FtcDashboard.getInstance().sendTelemetryPacket(telemetryPacket)
        telemetryPacket = TelemetryPacket() // Reset packet for next loop
    }

    override fun stop() {
        bot.mecanumBase.stop()
        FileLogger.flush()
    }
}

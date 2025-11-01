package pioneer.opmodes.teleop

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotorEx
import pioneer.BotType
import pioneer.helpers.DashboardPlotter
import pioneer.opmodes.BaseOpMode
import pioneer.opmodes.teleop.drivers.TeleopDriver1
import pioneer.opmodes.teleop.drivers.TeleopDriver2

@TeleOp(name = "Teleop")
class Teleop : BaseOpMode(BotType.GOBILDA_STARTER_BOT) {
    private lateinit var driver1: TeleopDriver1
    private lateinit var driver2: TeleopDriver2

    private lateinit var flywheelEncoder: DcMotorEx

    override fun onInit() {
        driver1 = TeleopDriver1(gamepad1, bot)
        driver2 = TeleopDriver2(gamepad2, bot)

        flywheelEncoder = hardwareMap.get(DcMotorEx::class.java, "flywheelEncoder")
    }

    override fun onLoop() {
        // Update gamepad inputs
        driver1.update()
        driver2.update()

        // Add telemetry data
        addTelemetryData()
    }

    private fun addAprilTagTelemetryData() {
        val detections = bot.aprilTagProcessor.getDetections()
        for (detection in detections) {
            // Check if tag or its properties are null to avoid null pointer exceptions
            if (detection != null && detection.ftcPose != null) {
                telemetry.addData("Detection", detection.id)
                telemetry.addLine(
                    "--Rel (x, y, z): (%.2f, %.2f, %.2f)".format(
                        detection.ftcPose.x,
                        detection.ftcPose.y,
                        detection.ftcPose.z
                    )
                )
                telemetry.addLine(
                    "--Rel (Y, P, R): (%.2f, %.2f, %.2f)".format(
                        detection.ftcPose.yaw,
                        detection.ftcPose.pitch,
                        detection.ftcPose.roll
                    )
                )
                telemetry.addLine(
                    "--Rel (R, B, E): (%.2f, %.2f, %.2f)".format(
                        detection.ftcPose.range,
                        detection.ftcPose.bearing,
                        detection.ftcPose.elevation
                    )
                )
            } else {
                telemetry.addLine("No valid AprilTag detections.")
            }
        }
    }

    private fun addTelemetryData() {
        telemetry.addData("Drive Power", driver1.drivePower)
        telemetry.addData("Flywheel Speed", driver1.flywheelSpeed)
        telemetry.addData("Field Centric", driver1.fieldCentric)
        telemetry.addData("Pose", bot.localizer.pose)
        telemetry.addData("Velocity", "vx: %.2f, vy: %.2f".format(bot.localizer.pose.vx, bot.localizer.pose.vy))
        telemetry.addData("Voltage", bot.batteryMonitor.getVoltage())

        telemetryPacket.put("Flywheel Motor Velocity", -bot.flywheel.velocity)
        telemetryPacket.put("Flywheel Velocity", -flywheelEncoder.velocity)
        DashboardPlotter.plotBotPosition(telemetryPacket, bot.localizer.pose)
    }
}

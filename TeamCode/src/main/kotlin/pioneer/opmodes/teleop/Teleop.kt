package pioneer.opmodes.teleop

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotorEx
import pioneer.Bot
import pioneer.BotType
import pioneer.helpers.DashboardPlotter
import pioneer.opmodes.BaseOpMode
import pioneer.opmodes.teleop.drivers.TeleopDriver1
import pioneer.opmodes.teleop.drivers.TeleopDriver2

@TeleOp(name = "Teleop")
class Teleop : BaseOpMode() {
    private lateinit var driver1: TeleopDriver1
    private lateinit var driver2: TeleopDriver2

    private lateinit var flywheelEncoder: DcMotorEx

    override fun onInit() {
        bot = Bot.fromType(BotType.GOBILDA_STARTER_BOT, hardwareMap)

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

    private fun addTelemetryData() {
        telemetry.addData("Drive Power", driver1.drivePower)
        telemetry.addData("Flywheel Speed", driver2.flywheelSpeed)
        telemetry.addData("Field Centric", driver1.fieldCentric)
        telemetry.addData("Pose", bot.pinpoint!!.pose)
        telemetry.addData("Velocity", "vx: %.2f, vy: %.2f".format(bot.pinpoint?.pose?.vx, bot.pinpoint?.pose?.vy))
        telemetry.addData("Voltage", bot.batteryMonitor?.voltage)

        bot.flywheel?.power?.let { telemetryPacket.put("Flywheel Motor Velocity", -it) }
        telemetryPacket.put("Flywheel Velocity", -flywheelEncoder.velocity)
        DashboardPlotter.plotBotPosition(telemetryPacket, bot.pinpoint?.pose)
    }
}

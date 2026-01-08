package pioneer.opmodes.other

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import pioneer.Bot
import pioneer.BotType
import pioneer.Constants
import pioneer.hardware.Flywheel
import pioneer.hardware.Launcher
import pioneer.helpers.Toggle
import pioneer.helpers.next
import pioneer.opmodes.BaseOpMode
import pioneer.opmodes.teleop.drivers.TeleopDriver1
import pioneer.opmodes.teleop.drivers.TeleopDriver2
import pioneer.opmodes.teleop.drivers.TeleopDriver2Testing

@TeleOp(name = "Shooter Testing", group = "Testing")
class ShootingTesting : BaseOpMode() {
    private lateinit var driver1: TeleopDriver1
    private lateinit var driver2: TeleopDriver2Testing
    private val allianceToggle = Toggle(false)
    private var changedAllianceColor = false

    override fun onInit() {
        bot = Bot.fromType(BotType.COMP_BOT, hardwareMap)

        driver1 = TeleopDriver1(gamepad1, bot)
        driver2 = TeleopDriver2Testing(gamepad2, bot)
    }

    override fun init_loop() {
        allianceToggle.toggle(gamepad1.touchpad)
        if (allianceToggle.justChanged) {
            changedAllianceColor = true
            bot.allianceColor = bot.allianceColor.next()
        }
        telemetry.addData("Alliance Color", bot.allianceColor)
        telemetry.update()
    }

    override fun start() {
        if (!changedAllianceColor) bot.allianceColor = Constants.TransferData.allianceColor
//        bot.spindexer?.resetMotorPosition(Constants.TransferData.spindexerPositionTicks)
//        bot.turret?.resetMotorPosition(Constants.TransferData.turretPositionTicks)
    }

    override fun onLoop() {
        // Update gamepad inputs
        driver1.update(dt)
        driver2.update()

        // Add telemetry data
        addTelemetryData()
    }

    private fun addTelemetryData() {
//        telemetry.addData("Transfer Data", Constants.TransferData.turretPositionTicks)
//        telemetry.addData("Turret Offset Ticks", bot.turret?.offsetTicks)
//        telemetry.addData("Turret Angle", bot.turret?.currentAngle)
        telemetry.addData("Artifacts", bot.spindexer?.artifacts.contentDeepToString())
        telemetry.addData("Pose", bot.pinpoint!!.pose)
        telemetry.addData("Turret Mode", bot.turret?.mode)
        telemetry.addData("Shoot State", driver2.shootState)
        telemetry.addData("Estimating Flywheel Speed", driver2.isEstimateSpeed.state)
        telemetry.addData("Flywheel Target Speed", driver2.flywheelSpeed)
        telemetry.addData("Flywheel Speed", driver2.flywheelVelocityEnum)
        telemetry.addData("Flywheel TPS", bot.flywheel?.velocity)
        telemetry.addData("Turret Angle", driver2.turretAngle)
        telemetry.addData("Drive Power", driver1.drivePower)
        telemetry.addData("Field Centric", driver1.fieldCentric)
        telemetry.addData("Velocity", "vx: %.2f, vy: %.2f".format(bot.pinpoint?.pose?.vx, bot.pinpoint?.pose?.vy))
        telemetry.addData("Voltage", bot.batteryMonitor?.voltage)
        telemetry.addData("Flywheel Motor Current", bot.flywheel?.motor?.getCurrent(CurrentUnit.MILLIAMPS))
        telemetryPacket.addLine("Flywheel TPS" + (bot.flywheel?.velocity ?: 0.0))
    }

}
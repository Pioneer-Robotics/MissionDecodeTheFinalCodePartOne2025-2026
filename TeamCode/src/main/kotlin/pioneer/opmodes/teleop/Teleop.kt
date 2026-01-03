package pioneer.opmodes.teleop

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import pioneer.Bot
import pioneer.BotType
import pioneer.Constants
import pioneer.helpers.Toggle
import pioneer.helpers.next
import pioneer.opmodes.BaseOpMode
import pioneer.opmodes.teleop.drivers.TeleopDriver1
import pioneer.opmodes.teleop.drivers.TeleopDriver2

@TeleOp(name = "Teleop")
class Teleop : BaseOpMode() {
    private lateinit var driver1: TeleopDriver1
    private lateinit var driver2: TeleopDriver2
    private val allianceToggle = Toggle(false)
    private var changedAllianceColor = false

    override fun onInit() {
        bot = Bot.fromType(BotType.COMP_BOT, hardwareMap)

        driver1 = TeleopDriver1(gamepad1, bot)
        driver2 = TeleopDriver2(gamepad2, bot)
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
        driver2.update(dt)

        // Add telemetry data
        addTelemetryData()
    }

    private fun addTelemetryData() {
//        telemetry.addData("Transfer Data", Constants.TransferData.turretPositionTicks)
//        telemetry.addData("Turret Offset Ticks", bot.turret?.offsetTicks)
//        telemetry.addData("Turret Angle", bot.turret?.currentAngle)
        telemetry.addData("Drive Power", driver1.drivePower)
        telemetry.addData("Pose", bot.pinpoint!!.pose)
        telemetry.addData("Artifacts", bot.spindexer?.artifacts.contentDeepToString())
        telemetry.addData("Target Goal", driver2.targetGoal)
        telemetry.addData("Turret Mode", bot.turret?.mode)
        telemetry.addData("Flywheel Target Speed", driver2.flywheelSpeed)
        telemetry.addData("Flywheel TPS", bot.flywheel?.velocity)
        telemetry.addData("Turret Angle", driver2.turretAngle)
        telemetry.addData("Spindexer State", bot.spindexer?.motorState)

        telemetry.addData("Spindexer Target Ticks", bot.spindexer?.targetMotorTicks)
        telemetry.addData("Spindexer Ticks", bot.spindexer?.currentMotorTicks)
        telemetryPacket.put("Spindexer Target Ticks", bot.spindexer?.targetMotorTicks)
        telemetryPacket.put("Spindexer Ticks", bot.spindexer?.currentMotorTicks)
        telemetryPacket.put("Spindexer Velocity", bot.spindexer?.currentMotorVelocity)

        telemetry.addData("Field Centric", driver1.fieldCentric)
        telemetry.addData("Velocity", "vx: %.2f, vy: %.2f".format(bot.pinpoint?.pose?.vx, bot.pinpoint?.pose?.vy))
        telemetry.addData("Voltage", bot.batteryMonitor?.voltage)
        telemetry.addData("Flywheel Motor Current", bot.flywheel?.motor?.getCurrent(CurrentUnit.MILLIAMPS))
        telemetryPacket.put("Flywheel TPS", (bot.flywheel?.velocity ?: 0.0))
        telemetryPacket.put("Target Flywheel TPS", (driver2.flywheelSpeed))
    }
}

package pioneer.opmodes.teleop

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import pioneer.Bot
import pioneer.BotType
import pioneer.Constants
import pioneer.general.AllianceColor
import org.firstinspires.ftc.teamcode.prism.Color
import pioneer.decode.Artifact
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
            bot.led?.setColor(
                when(bot.allianceColor) {
                    AllianceColor.RED -> Color.RED
                    AllianceColor.BLUE -> Color.BLUE
                    AllianceColor.NEUTRAL -> Color.PURPLE
                },
                0,
                23,
            )
        }
        telemetry.addData("Alliance Color", bot.allianceColor)
        telemetry.update()
    }

    override fun onStart() {
        if (!changedAllianceColor) bot.allianceColor = Constants.TransferData.allianceColor
        driver2.onStart()
    }

    override fun onLoop() {
        // Update gamepad inputs
        driver1.update()
        driver2.update()

        // Display artifacts with LED
        bot.led?.displayArtifacts(listOf(Artifact.GREEN, Artifact.PURPLE, Artifact.PURPLE))

        // Add telemetry data
        addTelemetryData()
    }

    private fun addTelemetryData() {
        addTelemetryData("Alliance Color", bot.allianceColor, Verbose.INFO)
        addTelemetryData("Drive Power", driver1.drivePower, Verbose.INFO)
        addTelemetryData("Pose", bot.pinpoint!!.pose, Verbose.DEBUG)
        addTelemetryData("Artifacts", bot.spindexer?.artifacts.contentDeepToString(), Verbose.INFO)

        addTelemetryData("Turret Mode", bot.turret?.mode, Verbose.INFO)
        addTelemetryData("Multishot Mode", driver2.multishotState, Verbose.INFO)
        addTelemetryData("Flywheel Operating", bot.flywheel?.operatingMode, Verbose.INFO)
        addTelemetryData("Use Auto Track Offset", driver2.useAutoTrackOffset, Verbose.DEBUG)
        addTelemetryData("Flywheel Speed Offset", driver2.flywheelSpeedOffset, Verbose.DEBUG)
        addTelemetryData("Flywheel Target Speed", driver2.estimatedFlywheelSpeed, Verbose.DEBUG)
        addTelemetryData("Flywheel TPS", bot.flywheel?.velocity, Verbose.DEBUG)
        addTelemetryData("Turret Angle", driver2.turretAngle, Verbose.DEBUG)
//        addTelemetryData("Turret Target Ticks", bot.turret?.targetTicks, Verbose.DEBUG)
//        addTelemetryData("Turret Real Ticks", bot.turret?.currentTicks, Verbose.DEBUG)

        addTelemetryData("Drive Power", driver1.drivePower, Verbose.DEBUG)
        addTelemetryData("Spindexer State", bot.spindexer?.motorState, Verbose.INFO)

        addTelemetryData("April Tag Relative Error", driver2.errorDegrees, Verbose.INFO)

        addTelemetryData("Spindexer Target Ticks", bot.spindexer?.targetMotorTicks, Verbose.DEBUG)
        addTelemetryData("Spindexer Ticks", bot.spindexer?.currentMotorTicks, Verbose.DEBUG)

        telemetryPacket.put("Spindexer Target Ticks", bot.spindexer?.targetMotorTicks)
        telemetryPacket.put("Spindexer Ticks", bot.spindexer?.currentMotorTicks)
        telemetryPacket.put("Spindexer Velocity", bot.spindexer?.currentMotorVelocity)

        addTelemetryData("Field Centric", driver1.fieldCentric, Verbose.INFO)
        addTelemetryData("Velocity", "vx: %.2f, vy: %.2f".format(bot.pinpoint?.pose?.vx, bot.pinpoint?.pose?.vy), Verbose.DEBUG)
        addTelemetryData("Voltage", bot.batteryMonitor?.voltage, Verbose.INFO)

        addTelemetryData("Flywheel Motor Current", bot.flywheel?.motor?.getCurrent(CurrentUnit.MILLIAMPS), Verbose.DEBUG)
        telemetryPacket.put("Flywheel TPS", (bot.flywheel?.velocity ?: 0.0))
        telemetryPacket.put("Target Flywheel TPS", (driver2.finalFlywheelSpeed))
        telemetryPacket.addLine("Flywheel TPS" + (bot.flywheel?.velocity ?: 0.0))


        telemetryPacket.put("Turret Target Ticks", bot.turret?.targetTicks)
        telemetryPacket.put("Turret Current Ticks", bot.turret?.currentTicks)

    }
}

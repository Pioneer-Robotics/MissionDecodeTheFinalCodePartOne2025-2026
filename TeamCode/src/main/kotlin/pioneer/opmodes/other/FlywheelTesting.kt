package pioneer.opmodes.auto

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.Bot
import pioneer.hardware.Flywheel
import pioneer.hardware.Launcher
import pioneer.hardware.MecanumBase
import pioneer.helpers.FileLogger
import pioneer.helpers.Pose
import pioneer.helpers.Toggle
import pioneer.localization.localizers.Pinpoint
import pioneer.opmodes.BaseOpMode
import pioneer.pathing.paths.LinearPath

@TeleOp(name = "Flywheel Testing", group = "Testing")
class FlywheelTesting : BaseOpMode() {

    var incFlywheelSpeed = Toggle(false)
    var decFlywheelSpeed = Toggle(false)
    var flywheelSpeed = 0.0
    var scale = 1.0

    var flywheelToggle = Toggle(false)

    override fun onInit() {
        bot =
            Bot
                .Builder()
                .add(Flywheel(hardwareMap))
                .add(Launcher(hardwareMap))
                .build()
    }

    override fun onLoop() {
        if (gamepad1.triangleWasPressed()) {
            scale += 1.0
        }

        if (gamepad1.crossWasPressed()) {
            scale -= 1.0
        }

        incFlywheelSpeed.toggle(gamepad1.right_bumper)
        decFlywheelSpeed.toggle(gamepad1.left_bumper)
        flywheelToggle.toggle(gamepad1.dpad_left)

        if (incFlywheelSpeed.justChanged){
            flywheelSpeed += 50.0 * scale
        }
        if (decFlywheelSpeed.justChanged){
            flywheelSpeed -= 50.0 * scale
        }



        if (flywheelToggle.state){
            bot.flywheel?.velocity = flywheelSpeed
        } else {
            bot.flywheel?.velocity = 0.0
        }

        if (gamepad1.square){
            bot.launcher?.triggerLaunch()
        }

        telemetry.addData("Actual Flywheel Velocity", bot.flywheel?.velocity)
        telemetry.addData("Target Velocity", flywheelSpeed)
        telemetry.addData("Scale Factor", scale)

        telemetryPacket.put("Scale Factor", scale)
        telemetryPacket.put("Flywheel Velocity", bot.flywheel?.velocity)
        telemetryPacket.put("Target Velocity", flywheelSpeed)

        FileLogger.debug("Flywheel Velocity", bot.flywheel?.velocity.toString())
    }
}

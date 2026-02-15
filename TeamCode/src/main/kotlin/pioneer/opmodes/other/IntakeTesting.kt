package pioneer.opmodes.other;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.Bot
import pioneer.hardware.MecanumBase
import pioneer.hardware.Intake
import pioneer.helpers.Pose
import pioneer.helpers.Toggle
import pioneer.localization.localizers.Pinpoint
import pioneer.opmodes.BaseOpMode
import pioneer.pathing.paths.LinearPath

@TeleOp(name = "Intake Testing", group = "Testing")
class IntakeTesting :  BaseOpMode() {

    var incIntakeSpeed = Toggle(startState = false)
    var decIntakeSpeed = Toggle(startState = false)
    var intakeSpeed = 0.0

    var intakeToggle = Toggle(false)

    override fun onInit() {
        bot =
            Bot
                .Builder()
                .add(Intake(hardwareMap))
                .build()
    }

    override fun onLoop() {

        incIntakeSpeed.toggle(button = gamepad1.right_bumper)
        decIntakeSpeed.toggle(button = gamepad1.left_bumper)
        intakeToggle.toggle(button = gamepad1.dpad_left)

        if(incIntakeSpeed.justChanged){
            intakeSpeed += 0.1
        }
        if(decIntakeSpeed.justChanged){
            intakeSpeed -= 0.1
        }

        if(intakeToggle.state) {
            bot.intake?.power = -intakeSpeed
        } else {
                bot.intake?.power = 0.0
        }

        telemetry.addData("Actual Intake Speed", bot.intake?.power)
        telemetry.addData("Intake Speed", intakeSpeed)
    }
}
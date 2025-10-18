package org.firstinspires.ftc.teamcode.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import org.firstinspires.ftc.teamcode.Bot
import org.firstinspires.ftc.teamcode.localization.Pose

@Autonomous(name = "Horizontal KV Tuner", group = "Calibration")
class HorizontalKVTuner : OpMode() {

    private lateinit var bot: Bot

    override fun init() {
        bot = Bot(Bot.BotFlavor.GOBILDA_STARTER_BOT, hardwareMap)
    }

    override fun loop() {
        bot.update()
        bot.mecanumBase.setDriveVA(
            Pose(50.0, 0.0, 0.0),   // 50 cm/s sideways
            Pose(0.0, 0.0, 0.0)     // No acceleration, we are only tuning velocity
        )
        telemetry.addData("Velocity (cm/s)", bot.localizer.velocity.x)
        telemetry.addData("Position (cm)", bot.localizer.pose.x)
        telemetry.update()
    }
}

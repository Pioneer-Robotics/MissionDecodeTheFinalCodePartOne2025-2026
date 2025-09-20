package org.firstinspires.ftc.teamcode.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import org.firstinspires.ftc.teamcode.Bot
import org.firstinspires.ftc.teamcode.localization.Pose

@Autonomous(name = "Forward KV Tuner", group = "Calibration")
class ForwardKVTuner : OpMode() {
    override fun init() {
        Bot.initialize(hardwareMap, telemetry)
    }

    override fun loop() {
        Bot.update()
        Bot.mecanumBase.setDriveVA(
            Pose(0.0, 50.0, 0.0),   // 50 cm/s forward
            Pose(0.0, 0.0, 0.0)     // No acceleration, we are only tuning velocity
        )
        telemetry.addData("Velocity (cm/s)", Bot.localizer.velocity.y)
        telemetry.addData("Position (cm)", Bot.localizer.pose.y)
        telemetry.update()
    }
}

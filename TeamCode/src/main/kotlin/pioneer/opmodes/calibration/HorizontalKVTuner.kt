package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import pioneer.Bot
import pioneer.localization.Pose

@Autonomous(name = "Horizontal KV Tuner", group = "Calibration")
class HorizontalKVTuner : OpMode() {
    override fun init() {
        Bot.initialize(hardwareMap, telemetry)
    }

    override fun loop() {
        Bot.update()
        Bot.mecanumBase.setDriveVA(
            Pose(50.0, 0.0, 0.0),   // 50 cm/s sideways
            Pose(0.0, 0.0, 0.0)     // No acceleration, we are only tuning velocity
        )
        telemetry.addData("Velocity (cm/s)", Bot.localizer.velocity.x)
        telemetry.addData("Position (cm)", Bot.localizer.pose.x)
        telemetry.update()
    }
}

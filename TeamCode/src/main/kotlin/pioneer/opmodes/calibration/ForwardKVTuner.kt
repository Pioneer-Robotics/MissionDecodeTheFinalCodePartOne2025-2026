package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import pioneer.Bot
import pioneer.helpers.Pose

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
        telemetry.addData("Velocity (cm/s)", Bot.localizer.pose.vy)
        telemetry.addData("Position (cm)", Bot.localizer.pose.y)
        telemetry.update()
    }
}

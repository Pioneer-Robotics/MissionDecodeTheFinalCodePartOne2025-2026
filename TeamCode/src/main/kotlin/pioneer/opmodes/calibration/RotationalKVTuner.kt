package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import pioneer.Bot
import pioneer.localization.Pose

@Autonomous(name = "Rotational KV Tuner", group = "Calibration")
class RotationalKVTuner : OpMode() {
    override fun init() {
        Bot.initialize(hardwareMap, telemetry)
    }

    override fun loop() {
        Bot.update()
        Bot.mecanumBase.setDriveVA(
            Pose(theta = 1.0),    // 1 rad/s rotation (omega)
            Pose()                // No acceleration
        )
        telemetry.addData("Velocity (rad/s)", Bot.localizer.pose.omega)
        telemetry.addData("Rotation (rad)", Bot.localizer.pose.theta)
        telemetry.update()
    }
}

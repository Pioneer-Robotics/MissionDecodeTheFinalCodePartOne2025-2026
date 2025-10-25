package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import pioneer.Bot
import pioneer.helpers.Pose
import pioneer.opmodes.BaseOpMode

@Autonomous(name = "Forward KV Tuner", group = "Calibration")
class ForwardKVTuner : BaseOpMode() {
    override fun onLoop() {
        bot.mecanumBase.setDriveVA(
            Pose(vy = 50.0, ay = 0.0), // 50 cm/s forward
        )
        telemetry.addData("Velocity (cm/s)", bot.localizer.pose.vy)
        telemetry.addData("Position (cm)", bot.localizer.pose.y)
    }
}

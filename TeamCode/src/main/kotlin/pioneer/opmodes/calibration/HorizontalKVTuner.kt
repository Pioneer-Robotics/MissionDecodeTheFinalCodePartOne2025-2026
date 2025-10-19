package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import pioneer.helpers.Pose
import pioneer.opmodes.BaseOpMode

@Autonomous(name = "Horizontal KV Tuner", group = "Calibration")
class HorizontalKVTuner : BaseOpMode() {
    override fun onLoop() {
        bot.mecanumBase.setDriveVA(
            Pose(x = 50.0), // 50 cm/s right
            Pose()          // No acceleration, we are only tuning velocity
        )
        telemetry.addData("Velocity (cm/s)", bot.localizer.pose.vx)
        telemetry.addData("Position (cm)", bot.localizer.pose.x)
    }
}

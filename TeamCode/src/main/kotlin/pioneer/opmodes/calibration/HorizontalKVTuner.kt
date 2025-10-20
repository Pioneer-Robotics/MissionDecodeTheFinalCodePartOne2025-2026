package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import pioneer.helpers.Pose
import pioneer.opmodes.BaseOpMode

@Autonomous(name = "Horizontal KV Tuner", group = "Calibration")
class HorizontalKVTuner : BaseOpMode() {
    override fun onLoop() {
        bot.mecanumBase.setDriveVA(
            Pose(vx = 50.0, ax = 0.0), // 50 cm/s right
        )
        telemetry.addData("Velocity (cm/s)", bot.localizer.pose.vx)
        telemetry.addData("Position (cm)", bot.localizer.pose.x)
    }
}

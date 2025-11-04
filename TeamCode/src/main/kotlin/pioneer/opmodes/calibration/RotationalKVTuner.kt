package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import pioneer.helpers.Pose
import pioneer.opmodes.BaseOpMode

@Autonomous(name = "Rotational KV Tuner", group = "Calibration")
class RotationalKVTuner : BaseOpMode() {
    override fun onLoop() {
        bot.mecanumBase.setDriveVA(
            Pose(omega = 1.0, alpha = 0.0), // 1 rad/s clockwise
        )
        telemetry.addData("Velocity (cm/s)", bot.localizer.pose.omega)
        telemetry.addData("Position (cm)", bot.localizer.pose.theta)
    }
}

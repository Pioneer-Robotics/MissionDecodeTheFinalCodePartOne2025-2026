package pioneer.opmodes.calibration

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import pioneer.Bot
import pioneer.BotType
import pioneer.helpers.Pose
import pioneer.opmodes.BaseOpMode

@Disabled
@Autonomous(name = "Rotational KV Tuner", group = "Calibration")
class RotationalKVTuner : BaseOpMode() {
    override fun onInit() {
        bot = Bot.fromType(BotType.MECANUM_BOT, hardwareMap)
        FtcDashboard.getInstance().telemetry.addData("Velocity (cm/s)", 0.0)
        FtcDashboard.getInstance().telemetry.addData("Target (cm/s)", 1.0)
        FtcDashboard.getInstance().telemetry.update()
    }

    override fun onLoop() {
        bot.mecanumBase!!.setDriveVA(
            Pose(omega = 1.0, alpha = 0.0), // 1 rad/s
        )
        telemetry.addData("Velocity (cm/s)", bot.pinpoint!!.pose.omega)
        telemetry.addData("Position (cm)", bot.pinpoint!!.pose.theta)
        FtcDashboard.getInstance().telemetry.addData("Velocity (cm/s)", bot.pinpoint!!.pose.omega)
        FtcDashboard.getInstance().telemetry.addData("Target (cm/s)", 1.0)
        FtcDashboard.getInstance().telemetry.update()
    }
}

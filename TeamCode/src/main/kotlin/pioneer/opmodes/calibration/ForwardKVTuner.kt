package pioneer.opmodes.calibration

import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import pioneer.Bot
import pioneer.BotType
import pioneer.helpers.Pose
import pioneer.opmodes.BaseOpMode

@Disabled
@Autonomous(name = "Forward KV Tuner", group = "Calibration")
class ForwardKVTuner : BaseOpMode() {
    override fun onInit() {
        bot = Bot.fromType(BotType.MECANUM_BOT, hardwareMap)
        FtcDashboard.getInstance().telemetry.addData("Velocity (cm/s)", 0.0)
        FtcDashboard.getInstance().telemetry.addData("Target (cm/s)", 50.0)
        FtcDashboard.getInstance().telemetry.update()
    }

    override fun onLoop() {
        bot.mecanumBase!!.setDriveVA(
            Pose(vy = 50.0, ay = 0.0), // 50 cm/s forward
        )
        telemetry.addData("Velocity (cm/s)", bot.pinpoint!!.pose.vy)
        telemetry.addData("Position (cm)", bot.pinpoint!!.pose.y)
        FtcDashboard.getInstance().telemetry.addData("Velocity (cm/s)", bot.pinpoint!!.pose.vy)
        FtcDashboard.getInstance().telemetry.addData("Target (cm/s)", 50.0)
        FtcDashboard.getInstance().telemetry.update()
    }
}

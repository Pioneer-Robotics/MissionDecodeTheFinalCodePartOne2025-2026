package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import pioneer.helpers.Pose
import pioneer.opmodes.BaseOpMode
import kotlin.math.PI

@Autonomous(name = "Odometer Offset Calculator", group = "Calibration")
class OdometerOffsetCalculation : BaseOpMode() {
    private var stopped = false

    override fun onInit() {
        bot.localizer.reset() // Reset odometry to the origin
        bot.localizer.update(chrono.dt) // Update to get initial encoder values
    }

    override fun onLoop() {
        bot.localizer.update(dt) // Update odometry

        val dXEncoderTicks = bot.localizer.encoderXTicks
        val dYEncoderTicks = bot.localizer.encoderYTicks

        telemetry.addData("Theta", bot.localizer.pose.theta)

        if (bot.localizer.pose.theta > (3 * PI / 4) || stopped) {
            stopped = true
            bot.mecanumBase.stop()

            val xOffset = dXEncoderTicks / (3 * PI / 4) * 10
            val yOffset = dYEncoderTicks / (3 * PI / 4) * 10

            telemetry.addData("X Offset", xOffset)
            telemetry.addData("Y Offset", yOffset)
        } else {
            bot.mecanumBase.setDriveVA(Pose(omega = 0.75))
        }
    }
}

package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import pioneer.Constants
import pioneer.helpers.Pose
import pioneer.opmodes.BaseOpMode

@Autonomous(name = "Odometer Offset Calculator", group = "Calibration")
class OdometerOffsetCalculation : BaseOpMode() {
    private var initialXEncoderTicks = 0
    private var initialYEncoderTicks = 0
    private var stopped = false

    override fun onInit() {
        bot.localizer.update(chrono.dt) // Get initial encoder values
        initialXEncoderTicks = bot.localizer.encoderXTicks
        initialYEncoderTicks = bot.localizer.encoderYTicks
    }

    override fun onLoop() {
        val dXEncoderTicks = initialXEncoderTicks - bot.localizer.encoderXTicks
        val dYEncoderTicks = initialYEncoderTicks - bot.localizer.encoderYTicks

        telemetry.addData("Theta", bot.localizer.pose.theta)

        if (bot.localizer.pose.theta > (3 * Math.PI / 4) || stopped) {
            stopped = true
            bot.mecanumBase.stop()

            val xOffset = dXEncoderTicks / (3 * Math.PI / 4) * Constants.Odometry.TICKS_TO_CM * 10
            val yOffset = dYEncoderTicks / (3 * Math.PI / 4) * Constants.Odometry.TICKS_TO_CM * 10

            telemetry.addData("X Offset", xOffset)
            telemetry.addData("Y Offset", yOffset)
        } else {
            bot.mecanumBase.setDriveVA(Pose(omega = 0.75))
        }
    }
}

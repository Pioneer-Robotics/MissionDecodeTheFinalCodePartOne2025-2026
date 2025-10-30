package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import pioneer.Constants
import pioneer.helpers.Pose
import pioneer.opmodes.BaseOpMode

@Autonomous(name = "Odometer Offset Calculator", group = "Calibration")
class OdometerOffsetCalculation : BaseOpMode() {
    private var initialXEncoderTicks = 0
    private var initialYEncoderTicks = 0

    override fun onInit() {
        initialXEncoderTicks = bot.localizer.encoderXTicks
        initialYEncoderTicks = bot.localizer.encoderYTicks
    }

    override fun onLoop() {
        val dXEncoderTicks = initialXEncoderTicks - bot.localizer.encoderXTicks
        val dYEncoderTicks = initialYEncoderTicks - bot.localizer.encoderYTicks

        telemetry.addData("X Encoder Ticks", dXEncoderTicks)
        telemetry.addData("Y Encoder Ticks", dYEncoderTicks)

        if (bot.localizer.pose.theta > Math.PI/2) {
            bot.mecanumBase.stop()

            val xOffset = dXEncoderTicks / (Math.PI / 2) * Constants.Odometry.TICKS_TO_CM * 10
            val yOffset = dYEncoderTicks / (Math.PI / 2) * Constants.Odometry.TICKS_TO_CM * 10

            telemetry.addData("X Offset", xOffset)
            telemetry.addData("Y Offset", yOffset)
        } else {
            bot.mecanumBase.setDriveVA(Pose(omega = 0.25))
        }
    }
}

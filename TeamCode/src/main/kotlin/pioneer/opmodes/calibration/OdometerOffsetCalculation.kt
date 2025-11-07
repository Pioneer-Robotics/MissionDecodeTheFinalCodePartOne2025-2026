package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import pioneer.Constants
import pioneer.helpers.Pose
import pioneer.opmodes.BaseOpMode
import kotlin.math.PI

@Autonomous(name = "Odometer Offset Calculator", group = "Calibration")
class OdometerOffsetCalculation : BaseOpMode() {
    private val numRotations = 10
    private var accumulatedTheta = 0.0
    private var prevTheta = 0.0
    private var initialXEncoderTicks = 0
    private var initialYEncoderTicks = 0

    override fun onInit() {
        bot.localizer.update(dt) // Get initial encoder values
        initialXEncoderTicks = bot.localizer.encoderXTicks
        initialYEncoderTicks = bot.localizer.encoderYTicks
    }

    override fun onLoop() {
        telemetry.addData("Theta", accumulatedTheta)
        if (accumulatedTheta > 2 * PI * numRotations) {
            bot.mecanumBase.stop()

            val dXEncoderTicks = initialXEncoderTicks - bot.localizer.encoderXTicks
            val dYEncoderTicks = initialYEncoderTicks - bot.localizer.encoderYTicks

            val xOffset = dXEncoderTicks / (3 * Math.PI / 4) * Constants.Odometry.TICKS_TO_CM * 10
            val yOffset = dYEncoderTicks / (3 * Math.PI / 4) * Constants.Odometry.TICKS_TO_CM * 10

            telemetry.addData("X Offset", xOffset)
            telemetry.addData("Y Offset", yOffset)
        } else {
            val dTheta = (bot.localizer.pose.theta - prevTheta) % (2 * PI)

            accumulatedTheta += dTheta

            bot.mecanumBase.setDriveVA(Pose(omega = 0.75))

            prevTheta = bot.localizer.pose.theta
        }
    }
}

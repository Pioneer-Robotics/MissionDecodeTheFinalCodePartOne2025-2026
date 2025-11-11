package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import pioneer.Bot
import pioneer.BotType
import pioneer.constants.Odometry
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
        bot = Bot.fromType(BotType.MECANUM_BOT, hardwareMap)
        bot.pinpoint!!.update(dt) // Get initial encoder values
        initialXEncoderTicks = bot.pinpoint!!.encoderXTicks
        initialYEncoderTicks = bot.pinpoint!!.encoderYTicks
    }

    override fun onLoop() {
        telemetry.addData("Theta", accumulatedTheta)
        if (accumulatedTheta > 2 * PI * numRotations) {
            bot.mecanumBase!!.stop()

            val dXEncoderTicks = initialXEncoderTicks - bot.pinpoint!!.encoderXTicks
            val dYEncoderTicks = initialYEncoderTicks - bot.pinpoint!!.encoderYTicks

            val xOffset = dXEncoderTicks / (3 * Math.PI / 4) * Odometry.TICKS_TO_CM * 10
            val yOffset = dYEncoderTicks / (3 * Math.PI / 4) * Odometry.TICKS_TO_CM * 10

            telemetry.addData("X Offset", xOffset)
            telemetry.addData("Y Offset", yOffset)
        } else {
            val dTheta = (bot.pinpoint!!.pose.theta - prevTheta) % (2 * PI)

            accumulatedTheta += dTheta

            bot.mecanumBase!!.setDriveVA(Pose(omega = 0.75))

            prevTheta = bot.pinpoint!!.pose.theta
        }
    }
}

package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import pioneer.Bot
import pioneer.BotType
import pioneer.Constants
import pioneer.helpers.FileLogger
import pioneer.helpers.Pose
import pioneer.opmodes.BaseOpMode
import kotlin.math.hypot

@Disabled
@Autonomous(name = "Static Feedforward Tuner", group = "Calibration")
class StaticFeedforwardTuner : BaseOpMode() {
    enum class State {
        FORWARD,
        DELAY,
        HORIZONTAL,
    }

    val startPower = 0.0
    val step = 0.001 // Power increase per step
    val velocityThreshold = 1.0 // cm/s
    val velocityThresholdTime = 10 // Number of updates needed above velocity threshold

    var currentPower = startPower
    var velocityTime = 0
    var state: State = State.FORWARD

    override fun onInit() {
        bot = Bot.fromType(BotType.MECANUM_BOT, hardwareMap)
        bot.pinpoint!!.reset() // Reset the localizer to the origin

        telemetryPacket.put("Current Power", currentPower)
        telemetryPacket.put("Current Velocity", hypot(bot.pinpoint!!.pose.vx, bot.pinpoint!!.pose.vy))
    }

    override fun onLoop() {
        when (state) {
            State.FORWARD -> {
                // Move forward until the velocity exceeds the threshold
                if (bot.pinpoint!!.pose.vy < velocityThreshold) {
                    velocityTime = 0
                    bot.mecanumBase!!.setDrivePower(
                        Pose(vx = 0.0, vy = currentPower, omega = 0.0),
                        Constants.Drive.DEFAULT_POWER,
                        Constants.Drive.MAX_MOTOR_VELOCITY_TPS,
                    )
                    currentPower += step
                    telemetryPacket.put("Current Power", currentPower)
                    telemetryPacket.put("Current Velocity", bot.pinpoint!!.pose.vy)
                } else {
                    if (velocityTime > velocityThresholdTime) {
                        bot.mecanumBase!!.stop()
                        currentPower = startPower // Reset power for horizontal movement
                        FileLogger.info(
                            "StaticFeedforwardTuner",
                            "Forward movement complete. Power: $currentPower, Velocity: ${bot.pinpoint!!.pose.vy}",
                        )
                        state = State.DELAY
                    } else {
                        velocityTime++
                    }
                }
            }
            State.DELAY -> {
                // Wait for a short period before moving horizontally
                Thread.sleep(1000) // 1 second delay
                state = State.HORIZONTAL
            }
            State.HORIZONTAL -> {
                // Move horizontally until the velocity exceeds the threshold
                if (bot.pinpoint!!.pose.vx < velocityThreshold) {
                    velocityTime = 0
                    bot.mecanumBase!!.setDrivePower(
                        Pose(vx = currentPower, vy = 0.0, omega = 0.0),
                        Constants.Drive.DEFAULT_POWER,
                        Constants.Drive.MAX_MOTOR_VELOCITY_TPS,
                    )
                    currentPower += step
                    telemetryPacket.put("Current Power", currentPower)
                    telemetryPacket.put("Current Velocity", bot.pinpoint!!.pose.vx)
                } else {
                    if (velocityTime > velocityThresholdTime) {
                        bot.mecanumBase!!.stop()
                        FileLogger.info(
                            "StaticFeedforwardTuner",
                            "Horizontal movement complete. Power: $currentPower, Velocity: ${bot.pinpoint!!.pose.vx}",
                        )
                        requestOpModeStop() // Stop the op mode after both movements
                    } else {
                        velocityTime++
                    }
                }
            }
        }
    }
}

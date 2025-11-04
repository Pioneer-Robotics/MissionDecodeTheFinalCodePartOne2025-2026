package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import pioneer.constants.Drive
import pioneer.helpers.FileLogger
import pioneer.helpers.Pose
import pioneer.opmodes.BaseOpMode
import kotlin.math.hypot

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
        bot.localizer.reset() // Reset the localizer to the origin

        telemetryPacket.put("Current Power", currentPower)
        telemetryPacket.put("Current Velocity", hypot(bot.localizer.pose.vx, bot.localizer.pose.vy))
    }

    override fun onLoop() {
        when (state) {
            State.FORWARD -> {
                // Move forward until the velocity exceeds the threshold
                if (bot.localizer.pose.vy < velocityThreshold) {
                    velocityTime = 0
                    bot.mecanumBase.setDrivePower(
                        Pose(vx = 0.0, vy = currentPower, omega = 0.0),
                        Drive.DEFAULT_POWER,
                        Drive.MAX_MOTOR_VELOCITY_TPS,
                    )
                    currentPower += step
                    telemetryPacket.put("Current Power", currentPower)
                    telemetryPacket.put("Current Velocity", bot.localizer.pose.vy)
                } else {
                    if (velocityTime > velocityThresholdTime) {
                        bot.mecanumBase.stop()
                        currentPower = startPower // Reset power for horizontal movement
                        FileLogger.info(
                            "StaticFeedforwardTuner",
                            "Forward movement complete. Power: $currentPower, Velocity: ${bot.localizer.pose.vy}",
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
                if (bot.localizer.pose.vx < velocityThreshold) {
                    velocityTime = 0
                    bot.mecanumBase.setDrivePower(
                        Pose(vx = currentPower, vy = 0.0, omega = 0.0),
                        Drive.DEFAULT_POWER,
                        Drive.MAX_MOTOR_VELOCITY_TPS,
                    )
                    currentPower += step
                    telemetryPacket.put("Current Power", currentPower)
                    telemetryPacket.put("Current Velocity", bot.localizer.pose.vx)
                } else {
                    if (velocityTime > velocityThresholdTime) {
                        bot.mecanumBase.stop()
                        FileLogger.info(
                            "StaticFeedforwardTuner",
                            "Horizontal movement complete. Power: $currentPower, Velocity: ${bot.localizer.pose.vx}",
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

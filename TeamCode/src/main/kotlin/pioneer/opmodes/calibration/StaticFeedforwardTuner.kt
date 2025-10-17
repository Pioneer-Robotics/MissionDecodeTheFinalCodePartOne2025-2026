package pioneer.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import pioneer.Bot
import pioneer.helpers.FileLogger
import kotlin.math.hypot

@Autonomous(name = "Static Feedforward Tuner", group = "Calibration")
class StaticFeedforwardTuner : OpMode() {
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

    override fun init() {
        Bot.initialize(hardwareMap, telemetry)
        Bot.localizer.reset() // Reset the localizer to the origin

        Bot.telemetryPacket.put("Current Power", currentPower)
        Bot.telemetryPacket.put("Current Velocity", hypot(Bot.localizer.pose.vx, Bot.localizer.pose.vy))
        Bot.sendTelemetryPacket()
    }

    override fun loop() {
        Bot.update()
        when (state) {
            State.FORWARD -> {
                // Move forward until the velocity exceeds the threshold
                if (Bot.localizer.pose.vy < velocityThreshold) {
                    velocityTime = 0
                    Bot.mecanumBase.setDrivePower(0.0, currentPower, 0.0)
                    currentPower += step
                    Bot.telemetryPacket.put("Current Power", currentPower)
                    Bot.telemetryPacket.put("Current Velocity", Bot.localizer.pose.vy)
                    Bot.sendTelemetryPacket()
                } else {
                    if (velocityTime > velocityThresholdTime) {
                        Bot.mecanumBase.stop()
                        currentPower = startPower // Reset power for horizontal movement
                        FileLogger.info("StaticFeedforwardTuner", "Forward movement complete. Power: $currentPower, Velocity: ${Bot.localizer.pose.vy}")
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
                if (Bot.localizer.pose.vx < velocityThreshold) {
                    velocityTime = 0
                    Bot.mecanumBase.setDrivePower(currentPower, 0.0, 0.0)
                    currentPower += step
                    Bot.telemetryPacket.put("Current Power", currentPower)
                    Bot.telemetryPacket.put("Current Velocity", Bot.localizer.pose.vx)
                    Bot.sendTelemetryPacket()
                } else {
                    if (velocityTime > velocityThresholdTime) {
                        Bot.mecanumBase.stop()
                        FileLogger.info("StaticFeedforwardTuner", "Horizontal movement complete. Power: $currentPower, Velocity: ${Bot.localizer.pose.vx}")
                        requestOpModeStop() // Stop the op mode after both movements
                    } else {
                        velocityTime++
                    }
                }
            }
        }
    }
}

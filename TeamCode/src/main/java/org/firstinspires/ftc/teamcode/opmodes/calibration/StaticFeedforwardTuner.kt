package org.firstinspires.ftc.teamcode.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import org.firstinspires.ftc.teamcode.Bot
import org.firstinspires.ftc.teamcode.helpers.FileLogger

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
        Bot.telemetryPacket.put("Current Velocity", Bot.localizer.velocity.getLength())
        Bot.sendTelemetryPacket()
    }

    override fun loop() {
        Bot.update()
        when (state) {
            State.FORWARD -> {
                // Move forward until the velocity exceeds the threshold
                if (Bot.localizer.velocity.y < velocityThreshold) {
                    velocityTime = 0
                    Bot.mecanumBase.setDrivePower(0.0, currentPower, 0.0, adjustForStrafe = false)
                    currentPower += step
                    Bot.telemetryPacket.put("Current Power", currentPower)
                    Bot.telemetryPacket.put("Current Velocity", Bot.localizer.velocity.y)
                    Bot.sendTelemetryPacket()
                } else {
                    if (velocityTime > velocityThresholdTime) {
                        Bot.mecanumBase.stop()
                        currentPower = startPower // Reset power for horizontal movement
                        FileLogger.info("StaticFeedforwardTuner", "Forward movement complete. Power: $currentPower, Velocity: ${Bot.localizer.velocity.y}")
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
                if (Bot.localizer.velocity.x < velocityThreshold) {
                    velocityTime = 0
                    Bot.mecanumBase.setDrivePower(currentPower, 0.0, 0.0, adjustForStrafe = false)
                    currentPower += step
                    Bot.telemetryPacket.put("Current Power", currentPower)
                    Bot.telemetryPacket.put("Current Velocity", Bot.localizer.velocity.x)
                    Bot.sendTelemetryPacket()
                } else {
                    if (velocityTime > velocityThresholdTime) {
                        Bot.mecanumBase.stop()
                        FileLogger.info("StaticFeedforwardTuner", "Horizontal movement complete. Power: $currentPower, Velocity: ${Bot.localizer.velocity.x}")
                        requestOpModeStop() // Stop the op mode after both movements
                    } else {
                        velocityTime++
                    }
                }
            }
        }
    }
}

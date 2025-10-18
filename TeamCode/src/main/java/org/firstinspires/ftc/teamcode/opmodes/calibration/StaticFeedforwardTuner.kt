package org.firstinspires.ftc.teamcode.opmodes.calibration

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import org.firstinspires.ftc.teamcode.Bot
import org.firstinspires.ftc.teamcode.helpers.FileLogger

@Autonomous(name = "Static Feedforward Tuner", group = "Calibration")
class StaticFeedforwardTuner : OpMode() {

    private lateinit var bot: Bot
    
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
        bot = Bot(Bot.BotFlavor.GOBILDA_STARTER_BOT, hardwareMap)
        bot.localizer.reset() // Reset the localizer to the origin

        bot.telemetryPacket.put("Current Power", currentPower)
        bot.telemetryPacket.put("Current Velocity", bot.localizer.velocity.getLength())
        bot.sendTelemetryPacket()
    }

    override fun loop() {
        bot.update()
        when (state) {
            State.FORWARD -> {
                // Move forward until the velocity exceeds the threshold
                if (bot.localizer.velocity.y < velocityThreshold) {
                    velocityTime = 0
                    bot.mecanumBase.setDrivePower(0.0, currentPower, 0.0, adjustForStrafe = false)
                    currentPower += step
                    bot.telemetryPacket.put("Current Power", currentPower)
                    bot.telemetryPacket.put("Current Velocity", bot.localizer.velocity.y)
                    bot.sendTelemetryPacket()
                } else {
                    if (velocityTime > velocityThresholdTime) {
                        bot.mecanumBase.stop()
                        currentPower = startPower // Reset power for horizontal movement
                        FileLogger.info("StaticFeedforwardTuner", "Forward movement complete. Power: $currentPower, Velocity: ${bot.localizer.velocity.y}")
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
                if (bot.localizer.velocity.x < velocityThreshold) {
                    velocityTime = 0
                    bot.mecanumBase.setDrivePower(currentPower, 0.0, 0.0, adjustForStrafe = false)
                    currentPower += step
                    bot.telemetryPacket.put("Current Power", currentPower)
                    bot.telemetryPacket.put("Current Velocity", bot.localizer.velocity.x)
                    bot.sendTelemetryPacket()
                } else {
                    if (velocityTime > velocityThresholdTime) {
                        bot.mecanumBase.stop()
                        FileLogger.info("StaticFeedforwardTuner", "Horizontal movement complete. Power: $currentPower, Velocity: ${bot.localizer.velocity.x}")
                        requestOpModeStop() // Stop the op mode after both movements
                    } else {
                        velocityTime++
                    }
                }
            }
        }
    }
}

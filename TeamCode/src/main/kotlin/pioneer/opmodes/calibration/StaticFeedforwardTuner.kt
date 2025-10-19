package pioneer.opmodes.calibration

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import pioneer.helpers.Pose
import pioneer.Bot
import pioneer.helpers.FileLogger
import kotlin.math.hypot

@Autonomous(name = "Static Feedforward Tuner", group = "Calibration")
class StaticFeedforwardTuner : OpMode() {
    private lateinit var bot: Bot

    enum class State {
        FORWARD,
        DELAY,
        HORIZONTAL,
    }

    var telemetryPacket = TelemetryPacket()

    val startPower = 0.0
    val step = 0.001 // Power increase per step
    val velocityThreshold = 1.0 // cm/s
    val velocityThresholdTime = 10 // Number of updates needed above velocity threshold

    var currentPower = startPower
    var velocityTime = 0
    var state: State = State.FORWARD

    override fun init() {
        bot = Bot(pioneer.BotType.BASIC_MECANUM_BOT, hardwareMap)
        bot.localizer.reset() // Reset the localizer to the origin

        telemetryPacket.put("Current Power", currentPower)
        telemetryPacket.put("Current Velocity", hypot(bot.localizer.pose.vx, bot.localizer.pose.vy))
        FtcDashboard.getInstance().sendTelemetryPacket(telemetryPacket)
    }

    override fun loop() {
        bot.dtTracker.update()
        bot.localizer.update(bot.dtTracker.dt)
        when (state) {
            State.FORWARD -> {
                // Move forward until the velocity exceeds the threshold
                if (bot.localizer.pose.vy < velocityThreshold) {
                    velocityTime = 0
                    bot.mecanumBase.setDrivePower(Pose(vx = 0.0, vy = currentPower, omega = 0.0))
                    currentPower += step
                    telemetryPacket.put("Current Power", currentPower)
                    telemetryPacket.put("Current Velocity", bot.localizer.pose.vy)
                    FtcDashboard.getInstance().sendTelemetryPacket(telemetryPacket)
                    telemetryPacket = TelemetryPacket() // Reset packet for next loop
                } else {
                    if (velocityTime > velocityThresholdTime) {
                        bot.mecanumBase.stop()
                        currentPower = startPower // Reset power for horizontal movement
                        FileLogger.info("StaticFeedforwardTuner", "Forward movement complete. Power: $currentPower, Velocity: ${bot.localizer.pose.vy}")
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
                    bot.mecanumBase.setDrivePower(Pose(vx = currentPower, vy = 0.0, omega = 0.0))
                    currentPower += step
                    telemetryPacket.put("Current Power", currentPower)
                    telemetryPacket.put("Current Velocity", bot.localizer.pose.vx)
                    FtcDashboard.getInstance().sendTelemetryPacket(telemetryPacket)
                    telemetryPacket = TelemetryPacket() // Reset packet for next loop
                } else {
                    if (velocityTime > velocityThresholdTime) {
                        bot.mecanumBase.stop()
                        FileLogger.info("StaticFeedforwardTuner", "Horizontal movement complete. Power: $currentPower, Velocity: ${bot.localizer.pose.vx}")
                        requestOpModeStop() // Stop the op mode after both movements
                    } else {
                        velocityTime++
                    }
                }
            }
        }
    }
}

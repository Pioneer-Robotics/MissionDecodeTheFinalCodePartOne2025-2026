package pioneer.hardware

import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.util.ElapsedTime
import pioneer.Constants
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class Launcher(
    private val hardwareMap: HardwareMap,
    private val servoName: String = Constants.HardwareNames.LAUNCH_SERVO,
) : HardwareComponent {
    companion object {
        private const val SERVO_CYCLE_TIME_MS = 670L
        private const val RESET_THRESHOLD_MS = 750.0
    }

    private lateinit var launchServo: Servo
    private val resetTimer = ElapsedTime()
    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val isTriggered = AtomicBoolean(false)

    val isReset: Boolean
        get() = resetTimer.milliseconds() > RESET_THRESHOLD_MS

    override fun init() {
        launchServo = hardwareMap.get(Servo::class.java, servoName)
        launchServo.position = Constants.ServoPositions.LAUNCHER_REST
    }

    fun triggerLaunch() {
        if (!isTriggered.compareAndSet(false, true)) return

        launchServo.position = Constants.ServoPositions.LAUNCHER_TRIGGERED

        scheduler.schedule({
            try {
                launchServo.position = Constants.ServoPositions.LAUNCHER_REST
            } catch (_: Exception) {
            } finally {
                isTriggered.set(false)
                resetTimer.reset()
            }
        }, SERVO_CYCLE_TIME_MS, TimeUnit.MILLISECONDS)
    }
}

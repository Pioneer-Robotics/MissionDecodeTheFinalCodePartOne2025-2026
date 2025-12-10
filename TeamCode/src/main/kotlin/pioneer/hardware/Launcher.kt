package pioneer.hardware

import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.util.ElapsedTime
import pioneer.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
    private val isTriggered = AtomicBoolean(false)
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val isReset: Boolean
        get() = resetTimer.milliseconds() > RESET_THRESHOLD_MS

    override fun init() {
        launchServo = hardwareMap.get(Servo::class.java, servoName)
        launchServo.position = Constants.ServoPositions.LAUNCHER_REST
    }

    fun triggerLaunch() {
        if (!isTriggered.compareAndSet(false, true)) return

        launchServo.position = Constants.ServoPositions.LAUNCHER_TRIGGERED

        coroutineScope.launch {
            delay(SERVO_CYCLE_TIME_MS)
            try {
                launchServo.position = Constants.ServoPositions.LAUNCHER_REST
            } catch (_: Exception) {
                // Silently handle servo positioning errors
            } finally {
                isTriggered.set(false)
                resetTimer.reset()
            }
        }
    }

    fun stop() {
        coroutineScope.cancel()
    }
}

package pioneer.hardware

import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import pioneer.Constants
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class Launcher(
    val hardwareMap: HardwareMap,
    val servoName: String = Constants.HardwareNames.LAUNCH_SERVO
): HardwareComponent {
    lateinit var launchServo: Servo

    val isReset: Boolean
        get() = isTriggered.get()

    private val scheduler = Executors.newSingleThreadScheduledExecutor()
    private val isTriggered = AtomicBoolean(false)

    override fun init() {
        launchServo = hardwareMap.get(Servo::class.java, servoName)
    }

    fun triggerLaunch() {
        launchServo.position = Constants.ServoPositions.LAUNCHER_TRIGGERED
        if (!isTriggered.compareAndSet(false, true)) return

        scheduler.schedule({
            try {
                launchServo.position = Constants.ServoPositions.LAUNCHER_REST
            } catch (_: Exception) {
                // ignore hardware exceptions but ensure flag reset
            } finally {
                isTriggered.set(false)
            }
        }, 500, TimeUnit.MILLISECONDS)
    }
}
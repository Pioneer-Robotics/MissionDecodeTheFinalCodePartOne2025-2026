package pioneer.hardware

import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo
import com.qualcomm.robotcore.util.ElapsedTime
import pioneer.Constants
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class PTOServo(
    private val hardwareMap: HardwareMap,
    private val servoLName: String = Constants.HardwareNames.PTO_SERVO_L,
    private val servoRName: String = Constants.HardwareNames.PTO_SERVO_R,

    ) : HardwareComponent {
    private lateinit var servoL: Servo
    private lateinit var servoR: Servo

    private val resetTimer = ElapsedTime()

    val isReset: Boolean get() = resetTimer.seconds() > 1.0 && isTriggered
    var isTriggered: Boolean = false

    override fun init() {
        servoL = hardwareMap.get(Servo::class.java, servoLName)
        servoR = hardwareMap.get(Servo::class.java, servoRName)

        servoL.position = Constants.ServoPositions.L_PTO_UP
        servoR.position = Constants.ServoPositions.R_PTO_UP
    }

//    fun triggerTilt() {
//        if (!isReset) return
//        if (!isTriggered.compareAndSet(false, true)) return
//
//        launchServo.position = Constants.ServoPositions.LAUNCHER_TRIGGERED
//
//        scheduler.schedule({
//            try {
//                launchServo.position = Constants.ServoPositions.LAUNCHER_REST
//            } catch (_: Exception) {
//            } finally {
//                isTriggered.set(false)
//                resetTimer.reset()
//            }
//        }, SERVO_CYCLE_TIME_MS, TimeUnit.MILLISECONDS)
//    }

    fun  dropServos(){
        servoL.position = Constants.ServoPositions.L_PTO_DROP
        servoR.position = Constants.ServoPositions.R_PTO_DROP
        isTriggered = true
        resetTimer.reset()
    }

    fun raiseServos(){
        servoL.position = Constants.ServoPositions.L_PTO_UP
        servoR.position = Constants.ServoPositions.R_PTO_UP
        isTriggered = false
    }
}
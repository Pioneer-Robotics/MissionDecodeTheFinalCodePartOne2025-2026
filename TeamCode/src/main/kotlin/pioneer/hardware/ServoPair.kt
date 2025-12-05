package pioneer.hardware

import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.Servo

class ServoPair(
    private val hardwareMap: HardwareMap,
    private val servo1Name: String,
    private val servo2Name: String,
    private val reversed: Boolean = true,
) : HardwareComponent {
    private lateinit var servo1: Servo
    private lateinit var servo2: Servo

    // Assuming the servos have the same range and speed
    var position: Double
        get() = servo1.position

        set(value) {
            servo1.position = value
            servo2.position = value * if (reversed) -1 else 1
        }

    override fun init() {
        servo1 = hardwareMap.get(Servo::class.java, servo1Name)
        servo2 = hardwareMap.get(Servo::class.java, servo2Name)
    }
}

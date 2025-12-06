package pioneer.localization.localizers

import com.qualcomm.robotcore.hardware.DcMotor.RunMode
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import kotlin.math.PI

/**
 * Represents an Odometry system for a single encoder.
 */
class Odometry(
    hardwareMap: HardwareMap,
    private val name: String,
    private val ticksPerRev: Double,
    private val wheelDiameterCM: Double,
) {
    private val odometer: DcMotorEx = hardwareMap.get(DcMotorEx::class.java, name)

    init {
        this.reset()
    }

    /**
     * Resets the odometry hardware and internal state.
     */
    fun reset() {
        odometer.mode = RunMode.STOP_AND_RESET_ENCODER
    }

    /**
     * Gets the current position of the encoder in ticks.
     */
    val currentTicks: Int
        get() = odometer.currentPosition

    /**
     * Calculates the distance traveled based on ticks.
     * @return The distance traveled in centimeters.
     */
    fun toCentimeters(): Double {
        val circumference = wheelDiameterCM * PI
        return (currentTicks / ticksPerRev) * circumference
    }
}

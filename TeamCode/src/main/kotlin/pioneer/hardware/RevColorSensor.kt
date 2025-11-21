package pioneer.hardware

import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.NormalizedColorSensor
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import com.qualcomm.robotcore.hardware.DistanceSensor

data class NormalizedColors(
    val red: Float,
    val green: Float,
    val blue: Float,
    val alpha: Float,
)

class RevColorSensor(
    private val hardwareMap: HardwareMap,
    private val sensorName: String,
    private val distanceUnit: DistanceUnit = DistanceUnit.CM
) : HardwareComponent {
    lateinit var sensor: NormalizedColorSensor

    var gain: Float
        get() = sensor.gain
        set(value) { sensor.gain = value }

    val normalizedRGBA: NormalizedColors
        get() {
            val colors = sensor.normalizedColors
            return NormalizedColors(
                red   = colors.red * 255,
                green = colors.green * 255,
                blue  = colors.blue * 255,
                alpha = colors.alpha * 255
            )
        }

    // Convenience accessors
    val r get() = normalizedRGBA.red
    val g get() = normalizedRGBA.green
    val b get() = normalizedRGBA.blue
    val a get() = normalizedRGBA.alpha

    val distance: Double
        get() {
            return (sensor as DistanceSensor).getDistance(distanceUnit)
        }

    override fun init() {
        sensor = hardwareMap.get(NormalizedColorSensor::class.java, sensorName)
    }

    override fun toString(): String {
        val (r, g, b, a) = normalizedRGBA
        val d = distance
        return "R: %.1f G: %.1f B: %.1f A: %.1f D: %.1f cm".format(r, g, b, a, d)
    }
}

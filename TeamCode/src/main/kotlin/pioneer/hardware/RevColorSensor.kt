package pioneer.hardware

import android.graphics.Color
import com.qualcomm.robotcore.hardware.DistanceSensor
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.NormalizedColorSensor
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit

data class NormalizedRGBA(
    val red: Float,
    val green: Float,
    val blue: Float,
    val alpha: Float,
)

data class NormalizedHSV(
    val hue: Float,
    val saturation: Float,
    val value: Float,
)

class RevColorSensor(
    private val hardwareMap: HardwareMap,
    private val sensorName: String,
    private val distanceUnit: DistanceUnit = DistanceUnit.CM,
) : HardwareComponent {
    lateinit var sensor: NormalizedColorSensor

    var gain: Float
        get() = sensor.gain
        set(value) {
            sensor.gain = value
        }

    val normalizedRGBA: NormalizedRGBA
        get() {
            val colors = sensor.normalizedColors
            return NormalizedRGBA(
                red = colors.red * 255,
                green = colors.green * 255,
                blue = colors.blue * 255,
                alpha = colors.alpha * 255,
            )
        }

    val normalizedHSV: NormalizedHSV
        get() {
            val (red, green, blue) = normalizedRGBA
            val hsv = FloatArray(3) // To be modified in-place
            Color.RGBToHSV(red.toInt(), green.toInt(), blue.toInt(), hsv)
            return NormalizedHSV(hsv[0], hsv[1], hsv[2])
        }

    val distance: Double
        get() {
            return (sensor as DistanceSensor).getDistance(distanceUnit)
        }

    // Convenience accessors
    val r get() = normalizedRGBA.red
    val g get() = normalizedRGBA.green
    val b get() = normalizedRGBA.blue
    val a get() = normalizedRGBA.alpha
    val hue get() = normalizedHSV.hue
    val saturation get() = normalizedHSV.saturation
    val value get() = normalizedHSV.value

    override fun init() {
        sensor = hardwareMap.get(NormalizedColorSensor::class.java, sensorName)
    }

    override fun toString(): String {
        val (r, g, b) = normalizedRGBA
        val (h, s, v) = normalizedHSV
        val d = distance
        return "R: %.1f G: %.1f B: %.1f H: %.1f S: %.1f V: %.1f D: %.1f cm".format(
            r,
            g,
            b,
            h,
            s,
            v,
            d,
        )
    }
}

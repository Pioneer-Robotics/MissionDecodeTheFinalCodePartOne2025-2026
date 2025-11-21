package pioneer.hardware

import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.NormalizedColorSensor
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import com.qualcomm.robotcore.hardware.DistanceSensor
import android.graphics.Color

class RevColorSensor(
    private val hardwareMap: HardwareMap,
    private val sensorName: String,
    private val distanceUnit: DistanceUnit = DistanceUnit.CM
) : HardwareComponent {
    lateinit var sensor: NormalizedColorSensor

    override fun init() {
        sensor = hardwareMap.get(NormalizedColorSensor::class.java, sensorName)
    }

    var gain: Float
        get() = sensor.gain
        set(value) {
            sensor.gain = value
        }
    
    fun getNormalizedRGB(): Triple<Float, Float, Float> {
        val colors = sensor.normalizedColors
        return Triple(colors.red * 255, colors.green * 255, colors.blue * 255)
    }

    fun getNormalizedHSV(): FloatArray {
        val (red, green, blue) = getNormalizedRGB()
        val hsv = FloatArray(3) // To be modified in-place
        Color.RGBToHSV(red.toInt(), green.toInt(), blue.toInt(), hsv)
        return hsv
    }

    fun getDistance(): Double {
        return (sensor as DistanceSensor).getDistance(distanceUnit)
    }

    override fun toString(): String {
        val (r, g, b) = getNormalizedRGB()
        val hsv = getNormalizedHSV()
        val d = getDistance()
        return "R: %.1f G: %.1f B: %.1f H: %.1f S: %.1f V: %.1f D: %.1f cm".format(r, g, b, hsv[0], hsv[1], hsv[2], d)
    }
}
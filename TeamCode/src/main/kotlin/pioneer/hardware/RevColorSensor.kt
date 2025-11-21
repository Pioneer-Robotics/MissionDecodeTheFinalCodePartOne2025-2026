package pioneer.hardware

import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.NormalizedColorSensor
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import com.qualcomm.robotcore.hardware.DistanceSensor

class RevColorSensor(
    private val hardwareMap: HardwareMap,
    private val sensorName: String,
    private val distanceUnit: DistanceUnit = DistanceUnit.CM
) : HardwareComponent {
    lateinit var sensor: NormalizedColorSensor

    override fun init() {
        sensor = hardwareMap.get(NormalizedColorSensor::class.java, sensorName)
    }
    
    fun getNormalizedRGB(): Triple<Float, Float, Float> {
        val colors = sensor.normalizedColors
        return Triple(colors.red * 255, colors.green * 255, colors.blue * 255)
    }

    fun getDistance(): Double {
        return (sensor as DistanceSensor).getDistance(distanceUnit)
    }

    override fun toString(): String {
        val (r, g, b) = getNormalizedRGB()
        val d = getDistance()
        return "R: %.1f G: %.1f B: %.1f D: %.1f cm".format(r, g, b, d)
    }
}
package pioneer.opmodes.calibration

import android.icu.text.DecimalFormat
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DistanceSensor
import com.qualcomm.robotcore.hardware.NormalizedColorSensor
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import pioneer.decode.Artifact

@TeleOp(name = "Color Sensor Gain Calibration")
class ColorSensorGain : OpMode() {
    private lateinit var sensor: NormalizedColorSensor
    private val formatter = DecimalFormat("###.##")
    private var gain: Float = 3.0f

    override fun init() {
        sensor = hardwareMap.get(NormalizedColorSensor::class.java, "intakeSensor")
    }

    override fun loop() {
        val (red, green, blue, alpha) = listOf(
            sensor.normalizedColors.red * 255,
            sensor.normalizedColors.green * 255,
            sensor.normalizedColors.blue * 255,
            sensor.normalizedColors.alpha * 255,
        )

        val distance = (sensor as DistanceSensor).getDistance(DistanceUnit.CM)

        val artifact: Artifact? = when {
            distance > 6.0 || alpha > 200 -> null
            red > 40 && blue > 50 && green < blue -> Artifact.PURPLE
            green > 50 -> Artifact.GREEN
            else -> null
        }

        if (gamepad1.right_trigger > 0.05) gain += gamepad1.right_trigger
        if (gamepad1.left_trigger > 0.05) gain -= gamepad1.left_trigger
        sensor.gain = gain

        telemetry.addData("Gain", gain)
        telemetry.addData("Detected Artifact", artifact)
        telemetry.addData("RGBA",
            "r: ${formatter.format(red)}, " +
                    "g: ${formatter.format(green)}, " +
                    "b: ${formatter.format(blue)}, " +
                    "a: ${formatter.format(alpha)}"
        )
        telemetry.addData("Distance", distance)
        telemetry.update()
    }
}

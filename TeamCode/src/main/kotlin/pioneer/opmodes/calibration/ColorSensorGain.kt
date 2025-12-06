package pioneer.opmodes.calibration

import android.icu.text.DecimalFormat
import com.qualcomm.robotcore.eventloop.opmode.Disabled
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.decode.Artifact
import pioneer.hardware.RevColorSensor

@Disabled
@TeleOp(name = "Color Sensor Gain Calibration")
class ColorSensorGain : OpMode() {
    private lateinit var sensor: RevColorSensor
    private val formatter = DecimalFormat("###.##")
    private var gain: Float = 3.0f

    override fun init() {
        sensor = RevColorSensor(hardwareMap, "intakeSensor").apply { init() }
    }

    override fun loop() {
        val artifact: Artifact? =
            when {
                // Purple 165-240
                // Green 150-163
                sensor.distance > 15.0 -> null
                sensor.hue < 175 && sensor.hue > 150 -> Artifact.GREEN
                sensor.hue < 240 && sensor.hue > 175 -> Artifact.PURPLE
                else -> null
            }

        if (gamepad1.right_trigger > 0.05) gain += gamepad1.right_trigger
        if (gamepad1.left_trigger > 0.05) gain -= gamepad1.left_trigger
        sensor.gain = gain

        telemetry.addData("Gain", gain)
        telemetry.addData("Detected Artifact", artifact)
        telemetry.addData("Sensor Data", sensor.toString())
        telemetry.update()
    }
}

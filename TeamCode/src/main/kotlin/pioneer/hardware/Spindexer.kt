package pioneer.hardware

import com.qualcomm.hardware.rev.RevColorSensorV3
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import pioneer.decode.Artifact
import kotlin.enums.enumEntries
import kotlin.math.PI
import kotlin.math.abs

class Spindexer(
    private val hardwareMap: HardwareMap,
    private val motorName: String,
    private val intakeSensorName: String,
    private val outakeSensorName: String
) : HardwareComponent{
    enum class MotorPosition(val radians: Double, val index: Int) {
        INTAKE_1(0 * PI / 3, 1),
        OUTAKE_1(1 * PI / 3, 1),
        INTAKE_2(2 * PI / 3, 2),
        OUTAKE_2(3 * PI / 3, 2),
        INTAKE_3(4 * PI / 3, 3),
        OUTAKE_3(5 * PI / 3, 3);
    }

    override val name = "Spindexer"

    lateinit var motor: DcMotorEx // MAKE PRIVATE
    private lateinit var intakeSensor: RevColorSensorV3
    private lateinit var outakeSensor: RevColorSensorV3

    private var artifacts: List<Artifact?> = listOf(null, null, null)
    var motorState: MotorPosition = MotorPosition.INTAKE_1

    val isFull: Boolean
        get() = !artifacts.contains(null)

    override fun init() {
        motor = hardwareMap.get(DcMotorEx::class.java, motorName)
        intakeSensor = hardwareMap.get(RevColorSensorV3::class.java, intakeSensorName)
        outakeSensor = hardwareMap.get(RevColorSensorV3::class.java, outakeSensorName)

        motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
    }

    fun update() {
        val targetPositionTicks = (28 * 5 * 4 * motorState.radians / (2 * PI)).toInt()
        if (abs(targetPositionTicks - motor.currentPosition) > 1) {
            motor.targetPosition = targetPositionTicks
            motor.mode = DcMotor.RunMode.RUN_TO_POSITION
            motor.power = 0.2
        } else {
            motor.power = 0.0
            motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
        }
    }
}

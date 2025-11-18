package pioneer.hardware

import com.qualcomm.hardware.rev.RevColorSensorV3
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import pioneer.decode.Artifact
import pioneer.decode.Motif
import kotlin.math.PI
import kotlin.math.abs



/*
Positive ROT: CW due to under-mounted motor

INTAKE (INITIALIZED SPINDEXER POSITION):


    \       /
     \  1  /
      \   /
        *
    2   |   3
        |
        |

OUTAKE:

        |
        |
   2    *    1
      /   \
     /  3  \
    /       \

*/


class Spindexer(
    private val hardwareMap: HardwareMap,
    private val motorName: String,
    private val intakeSensorName: String,
    private val outakeSensorName: String,
    private var artifacts: MutableList<Artifact?> = MutableList(3) { null },
    private var motif: List<Artifact> = Motif(21).getPattern(),

) : HardwareComponent {

    private val ticksPerRadian = (28 * 5 * 4 / (2 * PI)).toInt()

    enum class MotorPosition(val radians: Double) {
        INTAKE_1(0 * PI / 3),
        OUTAKE_1(1 * PI / 3),
        INTAKE_2(2 * PI / 3),
        OUTAKE_2(3 * PI / 3),
        INTAKE_3(4 * PI / 3),
        OUTAKE_3(5 * PI / 3);
    }

    private val intakePositions =
        listOf(MotorPosition.INTAKE_1, MotorPosition.INTAKE_2, MotorPosition.INTAKE_3)
    private val outakePositions =
        listOf(MotorPosition.OUTAKE_1, MotorPosition.OUTAKE_2, MotorPosition.OUTAKE_3)

    private lateinit var motor: DcMotorEx
    private lateinit var intakeSensor: RevColorSensorV3
    private lateinit var outakeSensor: RevColorSensorV3

    var motorState: MotorPosition = MotorPosition.INTAKE_1

    val isFull: Boolean
        get() = !artifacts.contains(null)

    val motorCurrentTicks: Int // Avoid confusing with MotorPosition enum (not called motorPosition)
        get() = motor.currentPosition

    val motorTargetTicks: Int
        get() = motor.targetPosition

    override fun init() {
        motor = hardwareMap.get(DcMotorEx::class.java, motorName)
        intakeSensor = hardwareMap.get(RevColorSensorV3::class.java, intakeSensorName)
        outakeSensor = hardwareMap.get(RevColorSensorV3::class.java, outakeSensorName)

        motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
    }

    private fun getPositionIndex(): Int? {
        return when {
            motorState in intakePositions -> intakePositions.indexOf(motorState)
            motorState in outakePositions -> outakePositions.indexOf(motorState)
            else -> null // Safety check
        }
    }

    private fun withSensor(sensor: RevColorSensorV3, enableLight: Boolean, action: (RevColorSensorV3) -> Artifact?): Artifact? {
        sensor.enableLed(enableLight)
        return action(sensor).also { sensor.enableLed(false) }
    }

    private fun matchesPattern(startIndex: Int, offset: Int): Boolean {
        for (i in motif.indices) {
            val artifactIndex = (startIndex + i) % artifacts.size
            val motifIndex = (i + offset) % motif.size
            if (artifacts[artifactIndex] != motif[motifIndex]) {
                return false
            }
        }
        return true
    }

    // Artifact Handling
    private fun storeArtifact(artifact: Artifact?) {
        val index = getPositionIndex() ?: return
        artifacts[index] = artifact
    }

    private fun scanArtifact(enableLight: Boolean = false): Artifact? {
        return withSensor(currentSensor(), enableLight) { detectArtifact(it) }
    }

    private fun detectArtifact(sensor: RevColorSensorV3): Artifact? {
        val (red, blue, green, _) = listOf(sensor.red(), sensor.blue(), sensor.green(), sensor.alpha())
        val distance = sensor.getDistance(DistanceUnit.CM)

        return when {
            distance > 7.0 -> null
            red > 200 && blue > 200 -> Artifact.PURPLE
            green > 200 -> Artifact.GREEN
            else -> null
        }
    }

    private fun currentSensor(): RevColorSensorV3 =
        if (motorState in intakePositions) intakeSensor else outakeSensor

    fun scanAndStoreArtifact(enableLight: Boolean = false) {
        storeArtifact(scanArtifact(enableLight))
    }

    private fun findStartingPatternIndex(offset: Int = 0): Int {
        if (motif.isEmpty()) {
            return artifacts.indexOfFirst { it != null }.takeIf { it != -1 } ?: 0
        }

        for (startIndex in artifacts.indices) {
            if (matchesPattern(startIndex, offset)) {
                return startIndex
            }
        }
        return artifacts.indexOfFirst { it != null }.takeIf { it != -1 } ?: 0
    }

    fun switchMode() {
        val index = getPositionIndex() ?: return

        motorState = if (motorState in intakePositions) {
            outakePositions[index]
        } else {
            intakePositions[index]
        }
    }

    fun update() {
        val targetPositionTicks = (motorState.radians * ticksPerRadian).toInt()
        if (abs(targetPositionTicks - motor.currentPosition) > 1) {
            motor.targetPosition = targetPositionTicks
            motor.mode = DcMotor.RunMode.RUN_TO_POSITION
            motor.power = 0.2
        } else {
            motor.power = 0.0
            motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
        }
    }

    fun moveToNextOpenIntake(): Boolean {
        artifacts.indexOfFirst { it == null }.takeIf { it != -1 }?.let {
            motorState = intakePositions[it]
            return true
        }
        return false
    }
}
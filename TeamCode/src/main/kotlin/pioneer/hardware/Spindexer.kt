package pioneer.hardware

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DistanceSensor
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.NormalizedColorSensor
import org.firstinspires.ftc.robotcore.external.Telemetry
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
    private val _artifacts: Array<Artifact?> = Array(3) { null },
    var motif: List<Artifact> = Motif(21).getPattern(),
    val telemetry: Telemetry
) : HardwareComponent {
    // Indirect reference to internal artifacts array to prevent modification
    val artifacts: Array<Artifact?>
        get() = _artifacts

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
    private lateinit var intakeSensor: NormalizedColorSensor
    private lateinit var outakeSensor: NormalizedColorSensor

    var motorState: MotorPosition = MotorPosition.INTAKE_1

    val isFull: Boolean
        get() = !artifacts.contains(null)

    val motorCurrentTicks: Int
        get() = motor.currentPosition

    val motorTargetTicks: Int
        get() = motor.targetPosition

    /**
     * Returns the current sensor based on the motor position.
     */
    private val currentSensor: NormalizedColorSensor
        get() = if (motorState in intakePositions) intakeSensor else outakeSensor

    /**
     * Returns the index of the current motor position in the intake/outake lists.
     */
    private val positionIndex: Int?
        get() {
            return when (motorState) {
                in intakePositions -> intakePositions.indexOf(motorState)
                in outakePositions -> outakePositions.indexOf(motorState)
                else -> null // Safety check
            }
        }

    override fun init() {
        motor = hardwareMap.get(DcMotorEx::class.java, motorName)
        intakeSensor = hardwareMap.get(NormalizedColorSensor::class.java, intakeSensorName)
        outakeSensor = hardwareMap.get(NormalizedColorSensor::class.java, outakeSensorName)

        intakeSensor.gain = 1.0f
        outakeSensor.gain = 1.0f

        motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
    }

    /**
     * Checks if the artifacts match the motif starting from a given index with an offset.
     */
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
    /**
     * Stores the detected artifact at the current motor position index.
     */
    private fun storeArtifact(artifact: Artifact?) {
        val index = positionIndex ?: return
        artifacts[index] = artifact
    }

    /**
     * Scans the artifact using the current sensor.
     */
    private fun scanArtifact(): Artifact? {
        return detectArtifact(currentSensor)
    }

    /**
     * Detects the artifact based on color and distance readings from the sensor.
     */
    private fun detectArtifact(sensor: NormalizedColorSensor): Artifact? {
        val (red, blue, green) = listOf(
            sensor.normalizedColors.red * 255,
            sensor.normalizedColors.blue * 255,
            sensor.normalizedColors.green * 255,
        )
        val distance = (sensor as DistanceSensor).getDistance(DistanceUnit.CM)

        telemetry.addData("Distance", distance)
        telemetry.addData("RGB", "$red, $blue, $green")

        return when {
            distance > 7.0 -> null
            red > 200 && blue > 200 -> Artifact.PURPLE
            green > 200 -> Artifact.GREEN
            else -> null
        }
    }

    /**
     * Scans and stores the artifact at the current motor position.
     */
    private fun scanAndStoreArtifact() : Boolean {
        storeArtifact(scanArtifact() ?: return false)
        return true
    }

    /**
     * Finds the starting index in artifacts that matches the motif pattern with an optional offset.
     */
    private fun findStartingPatternIndex(offset: Int = 0): Int? {
        if (motif.isEmpty()) {
            return artifacts.indexOfFirst { it != null }.takeIf { it != -1 }
        }

        for (startIndex in artifacts.indices) {
            if (matchesPattern(startIndex, offset)) {
                return startIndex
            }
        }
        return artifacts.indexOfFirst { it != null }.takeIf { it != -1 } ?: 0
    }

    /**
     * Aligns the spindexer to the starting position of the motif pattern.
     */
    fun switchMode() {
        val index = positionIndex ?: return

        motorState = if (motorState in intakePositions) {
            outakePositions[index]
        } else {
            intakePositions[index]
        }
    }

    /**
     * Updates the motor position to match the desired motor state.
     * Checks for new artifacts if in an intake position.
     */
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
        if (motorState in intakePositions) {
            telemetry.addLine("Checking for artifact")
            if (scanAndStoreArtifact()) {
                // Artifact detected and stored, switch mode to outake
                switchMode()
            }
        }
    }

    /**
     * Moves the motor to the next open intake position if available.
     * @return true if moved to the next open intake, false otherwise.
     */
    fun moveToNextOpenIntake(): Boolean {
        artifacts.indexOfFirst { it == null }.takeIf { it != -1 }?.let {
            motorState = intakePositions[it]
            return true
        }
        return false
    }

    /**
     * Moves to the next outake position based on the stored artifacts and motif.
     * @return true if moved to the next outake, false otherwise.
     */
    fun moveToOutakeStart(): Boolean {
        val startIndex: Int = findStartingPatternIndex() ?: return false
        motorState = outakePositions[startIndex]
        return true
    }

    fun moveToOutakeNext(): Boolean {
        val index = positionIndex ?: return false
        val nextIndex = (index + 1) % outakePositions.size
        motorState = outakePositions[nextIndex]
        return true
    }
}

package pioneer.hardware

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.Telemetry
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
    private lateinit var intakeSensor: RevColorSensor
    private lateinit var outakeSensor: RevColorSensor

    var motorState: MotorPosition = MotorPosition.INTAKE_1

    val isFull: Boolean
        get() = !artifacts.contains(null)

    val isEmpty: Boolean
        get() = artifacts.all { it == null }

    val numStoredArtifacts: Int
        get() = artifacts.count { it != null }

    val motorCurrentTicks: Int
        get() = motor.currentPosition

    val motorTargetTicks: Int
        get() = motor.targetPosition

    /**
     * Returns the current sensor based on the motor position.
     */
    private val currentSensor: RevColorSensor
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
        intakeSensor = RevColorSensor(hardwareMap, intakeSensorName).apply { init() }
        outakeSensor = RevColorSensor(hardwareMap, outakeSensorName).apply { init() }

        intakeSensor.gain = 1.0f
        outakeSensor.gain = 1.0f

        motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
    }

    /**
     * Determines if the artifacts in the spindexer match the motif pattern, starting from a given index.
     * The comparison accounts for a circular pattern and allows an optional offset in the motif.
     *
     * @param startIndex The index in the artifacts array to start the comparison.
     * @param offset An optional offset to apply to the motif pattern during comparison.
     * @return True if the artifacts match the motif pattern, false otherwise.
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
    private fun detectArtifact(sensor: RevColorSensor): Artifact? {
        val (red, green, blue) = sensor.getNormalizedRGB()
        val distance = sensor.getDistance()

        telemetry.addData("Sensor: ", currentSensor.toString())

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
     * Finds the starting index in the artifacts array that matches the motif pattern, considering an optional offset.
     * If no match is found, returns the index of the first non-null artifact or 0 if all are null.
     *
     * @param offset An optional offset to apply to the motif pattern during comparison.
     * @return The starting index of the matching pattern, or the first non-null artifact index, or 0.
     */
    private fun findStartingPatternIndex(offset: Int = 0): Int? {
        // If motif is empty (No discovered pattern), return first non-null artifact index
        if (motif.isEmpty()) {
            return artifacts.indexOfFirst { it != null }.takeIf { it != -1 }
        }

        // Check each index for a matching pattern, considering the circular nature            
        artifacts.indices.forEach { startIndex ->
            if (matchesPattern(startIndex, offset)) return startIndex
        }

        return artifacts.indexOfFirst { it != null }.takeIf { it != -1 } ?: 0
    }

    /**
     * Switches the motor state between intake and outake mode at the current position index.
     */
    fun switchMode() {
        val index = positionIndex ?: return

        motorState = if (motorState in intakePositions) {
            outakePositions[index]
        } else {
            intakePositions[index]
        }
    }

    // If in intake position, go to next null intake pos
    // If in outake position, go to next non-null outake pos
    // return true if moved, false otherwise
    fun moveToNext(): Boolean {
        val index = positionIndex ?: return false

        val positions = if (motorState in intakePositions) intakePositions else outakePositions
        val condition: (Int) -> Boolean = if (motorState in intakePositions) {
            { artifacts[it] == null }
        } else {
            { artifacts[it] != null }
        }

        val nextIndex = (index + 1 until positions.size).firstOrNull(condition)
            ?: (0 until index).firstOrNull(condition)
            ?: return false

        motorState = positions[nextIndex]
        return true
    }

    private fun runMotorToState(power: Double = 0.2, toleranceTicks: Int = 1) {
        val targetTicks = (motorState.radians * ticksPerRadian).toInt()
        val currentTicks = motor.currentPosition
        val tickDifference = abs(targetTicks - currentTicks)

        motor.apply {
            if (tickDifference > toleranceTicks) {
                targetPosition = targetTicks
                mode = DcMotor.RunMode.RUN_TO_POSITION
                this.power = power
            } else {
                this.power = 0.0
                mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
            }
        }
    }

    /**
     * Updates the motor position to match the desired motor state.
     * Checks for new artifacts if in an intake position.
     */
    fun update() {
        runMotorToState()
        if (motorState in intakePositions) {
            telemetry.addLine("Checking for artifact")
            if (scanAndStoreArtifact()) {
                // Artifact detected and stored, switch mode to outake
                switchMode()
            }
        }
    }

    /**
     * Moves to the next outake position based on the stored artifacts and motif.
     * @return true if moved to the next outake, false otherwise.
     */
    fun moveToOutakeStart(): Boolean {
        findStartingPatternIndex()?.let {
            motorState = outakePositions[it]
            return true
        }
        return false
    }
}

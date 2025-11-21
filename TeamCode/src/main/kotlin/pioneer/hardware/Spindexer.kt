package pioneer.hardware

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import pioneer.Constants
import pioneer.decode.Artifact
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
) : HardwareComponent {

    // Motor positions in radians
    enum class MotorPosition(val radians: Double) {
        INTAKE_1(0 * PI / 3),
        OUTAKE_1(1 * PI / 3),
        INTAKE_2(2 * PI / 3),
        OUTAKE_2(3 * PI / 3),
        INTAKE_3(4 * PI / 3),
        OUTAKE_3(5 * PI / 3);
    }

    // Indirect reference to internal artifacts array to prevent modification
    val artifacts: Array<Artifact?>
        get() = _artifacts

    // Current motor state
    var motorState: MotorPosition = MotorPosition.INTAKE_1
        private set

    // Getter to check if motor has reached target position
    val reachedTarget: Boolean
        get() = abs(motor.currentPosition - motor.targetPosition) <= 5

    // Getters for artifact storage status
    val isFull: Boolean
        get() = !artifacts.contains(null)

    val isEmpty: Boolean
        get() = artifacts.all { it == null }

    val numStoredArtifacts: Int
        get() = artifacts.count { it != null }

    // Motor position accessors
    val motorCurrentTicks: Int
        get() = motor.currentPosition

    val motorTargetTicks: Int
        get() = motor.targetPosition


    private val ticksPerRadian = (28 * 5 * 4 / (2 * PI)).toInt()

    private var intakeConfirmCount = 0

    private val intakePositions =
        listOf(MotorPosition.INTAKE_1, MotorPosition.INTAKE_2, MotorPosition.INTAKE_3)
    private val outakePositions =
        listOf(MotorPosition.OUTAKE_1, MotorPosition.OUTAKE_2, MotorPosition.OUTAKE_3)

    private lateinit var motor: DcMotorEx
    private lateinit var intakeSensor: RevColorSensor
    private lateinit var outakeSensor: RevColorSensor

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
            // Only return index if at target position
            if (!reachedTarget) return null
            // Find index based on current motor state
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

        intakeSensor.gain = 3.5f
        outakeSensor.gain = 3.5f

        motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
    }

    /**
     * Updates the motor position to match the desired motor state.
     * Checks for new artifacts if in an intake position.
     */
    fun update() {
        runMotorToState()
        checkForArtifact()
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
     * Moves the motor to the next outake position in sequence.
     * @param artifact Optional artifact to prioritize moving to its outake position.
     */
    fun moveToNextOutake(artifact: Artifact? = null): Boolean {
        // No artifacts to outake
        if (isEmpty) return false

        // Already at desired outake position
        if (motorState in outakePositions) {
            val currentIndex = outakePositions.indexOf(motorState)
            if (artifact != null && artifacts[currentIndex] == artifact) return true
        }

        // If no artifact specified, move to next outake in sequence
        val nextIndex = findOutakeIndex(artifact)

        motorState = outakePositions[nextIndex]
        return true
    }

    /**
     * Consumes (removes and returns) the artifact at the current outake position.
     * @return the consumed Artifact, or null if none present or not in outake position
     */
    fun consumeCurrentArtifact(): Artifact? {
        // Only allow consumption from outake positions
        if (motorState !in outakePositions) return null
        // Get and remove artifact at current position
        val index = positionIndex ?: return null
        val artifact = _artifacts[index]
        _artifacts[index] = null
        // Automatically move to intake if empty
        if (isEmpty) moveToNextOpenIntake()
        return artifact
    }

    private fun checkForArtifact() {
        // Check for artifact if in intake position and reached target
        if (motorState in intakePositions)
        {
            if (scanAndStoreArtifact()) {
                // Artifact detected and stored, move to next open intake or switch mode if full
                if (!moveToNextOpenIntake()) switchMode()
            }
        }
    }

    private fun runMotorToState(power: Double = 0.25, toleranceTicks: Int = 1) {
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

    private fun findOutakeIndex(target: Artifact?): Int {
        return target?.let {
            artifacts.indexOfFirst { it == target }
                .takeIf { it != -1 }
        } ?: artifacts.indexOfFirst { it != null }
    }

    /**
     * Switches the motor state between intake and outake for the current position.
     */
    private fun switchMode() {
        val index = positionIndex ?: return

        motorState = if (motorState in intakePositions) {
            outakePositions[index]
        } else {
            intakePositions[index]
        }
    }

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
        if (!reachedTarget) return null
        return detectArtifact(currentSensor)
    }

    /**
     * Detects the artifact based on color and distance readings from the sensor.
     */
    private fun detectArtifact(sensor: RevColorSensor): Artifact? {
        val (red, blue, green, alpha) = listOf(sensor.r, sensor.b, sensor.g, sensor.a)
        val distance = sensor.distance

        // Determine artifact based on color thresholds
        return when {
            distance > 6.0 || alpha > 200 -> null
            red > 40 && blue > 50 && green < blue -> Artifact.PURPLE
            green > 50 -> Artifact.GREEN
            else -> null
        }
    }

    /**
     * Scans and stores the artifact at the current motor position.
     */
    private fun scanAndStoreArtifact(): Boolean {
        val artifact = scanArtifact()

        if (artifact != null) {
            intakeConfirmCount++

            if (intakeConfirmCount >= Constants.Other.REQUIRED_CONFIRM_FRAMES_SPINDEXER) {
                storeArtifact(artifact)
                intakeConfirmCount = 0
                return true
            }
        } else {
            intakeConfirmCount = 0
        }

        return false
    }
}

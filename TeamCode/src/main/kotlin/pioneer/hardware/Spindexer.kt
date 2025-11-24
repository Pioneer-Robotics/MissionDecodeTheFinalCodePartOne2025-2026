package pioneer.hardware

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.PIDFCoefficients
import pioneer.Constants
import pioneer.decode.Artifact
import kotlin.math.PI
import kotlin.math.abs
import com.qualcomm.robotcore.util.ElapsedTime
import pioneer.helpers.FileLogger


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

OUTTAKE:

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
    private val outtakeSensorName: String,
    private val _artifacts: Array<Artifact?> = Array(3) { null },
) : HardwareComponent {

    // Motor positions in radians
    enum class MotorPosition(val radians: Double) {
        INTAKE_1(0 * PI / 3),
        OUTTAKE_1(3 * PI / 3), // Shift down (+2)
        INTAKE_2(2 * PI / 3),
        OUTTAKE_2(5 * PI / 3), // Shift down (+2)
        INTAKE_3(4 * PI / 3),
        OUTTAKE_3(1 * PI / 3); // Shift down (+2) (wrapped)
    }

    // Indirect reference to internal artifacts array to prevent modification
    val artifacts: Array<Artifact?>
        get() = _artifacts

    // Current motor state
    var motorState: MotorPosition = MotorPosition.INTAKE_1

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

    val currentScannedArtifact: Artifact?
        get() = scanArtifact()

    private val ticksPerRadian = (28 * 5 * 4 / (2 * PI)).toInt()

    private val artifactVisibleTimer = ElapsedTime()
    private val artifactLostTimer = ElapsedTime()

    private var artifactSeen = false
    private var artifactWasSeenRecently = false

    private val intakePositions =
        listOf(MotorPosition.INTAKE_1, MotorPosition.INTAKE_2, MotorPosition.INTAKE_3)
    private val outtakePositions =
        listOf(MotorPosition.OUTTAKE_1, MotorPosition.OUTTAKE_2, MotorPosition.OUTTAKE_3)

    private lateinit var motor: DcMotorEx
    private lateinit var intakeSensor: RevColorSensor
    private lateinit var outtakeSensor: RevColorSensor

    /**
     * Returns the current sensor based on the motor position.
     */
    private val currentSensor: RevColorSensor
        get() = if (motorState in intakePositions) intakeSensor else outtakeSensor

    /**
     * Returns the index of the current motor position in the intake/outtake lists.
     */
    private val positionIndex: Int?
        get() {
            // Only return index if at target position
            if (!reachedTarget) return null
            // Find index based on current motor state
            return when (motorState) {
                in intakePositions -> intakePositions.indexOf(motorState)
                in outtakePositions -> outtakePositions.indexOf(motorState)
                else -> null // Safety check
            }
        }

    override fun init() {
        motor = hardwareMap.get(DcMotorEx::class.java, motorName)
        intakeSensor = RevColorSensor(hardwareMap, intakeSensorName).apply { init() }
        outtakeSensor = RevColorSensor(hardwareMap, outtakeSensorName).apply { init() }

        intakeSensor.gain = 20.0f
        outtakeSensor.gain = 20.0f

        motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        motor.mode = DcMotor.RunMode.RUN_USING_ENCODER
        FileLogger.info("Spindexer", motor.getPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER).toString())
        motor.setPIDFCoefficients(
            DcMotor.RunMode.RUN_USING_ENCODER,
                PIDFCoefficients(
                    10.0,
                    0.0,
                    1.0,
                    0.0
                )
            )
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
     * Moves the motor to the next outtake position in sequence.
     * @param artifact Optional artifact to prioritize moving to its outtake position.
     */
    fun moveToNextOuttake(artifact: Artifact? = null): Boolean {
        // No artifacts to outtake
        if (isEmpty) return false

        // Already at desired outtake position
        if (motorState in outtakePositions) {
            val currentIndex = outtakePositions.indexOf(motorState)
            if (artifact != null && artifacts[currentIndex] == artifact) return true
        }

        // If no artifact specified, move to next outtake in sequence
        val nextIndex = findOuttakeIndex(artifact)

        motorState = outtakePositions[nextIndex]
        return true
    }

    /**
     * Consumes (removes and returns) the artifact at the current outtake position.
     * @return the consumed Artifact, or null if none present or not in outtake position
     */
    fun consumeCurrentArtifact(): Artifact? {
        // Only allow consumption from outtake positions
        if (motorState !in outtakePositions) return null
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

    private fun findOuttakeIndex(target: Artifact?): Int {
        return target?.let {
            artifacts.indexOfFirst { it == target }
                .takeIf { it != -1 }
        } ?: artifacts.indexOfFirst { it != null }
    }

    /**
     * Switches the motor state between intake and outtake for the current position.
     */
    private fun switchMode() {
        val index = positionIndex ?: return

        motorState = if (motorState in intakePositions) {
            outtakePositions[index]
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
        // Determine artifact based on hue thresholds
        return when {
            sensor.distance > 15.0 -> null
            sensor.hue < 170 && sensor.hue > 150 -> Artifact.GREEN
            sensor.hue < 240 && sensor.hue > 170 -> Artifact.PURPLE
            else -> null
        }
    }

    /**
     * Scans and stores the artifact at the current motor position.
     */
    private fun scanAndStoreArtifact(): Boolean {
        val artifact = scanArtifact()

        if (artifact != null) {

            // If this is the first time we see it, start the visible timer
            if (!artifactSeen) {
                artifactVisibleTimer.reset()
                artifactSeen = true
            }

            // Reset the lost timer because we see it
            artifactLostTimer.reset()
            artifactWasSeenRecently = true

            // If visible long enough, confirm intake
            if (artifactVisibleTimer.milliseconds() >= Constants.Spindexer.CONFIRM_INTAKE_MS) {
                storeArtifact(artifact)

                // Reset state
                artifactSeen = false
                artifactWasSeenRecently = false

                return true
            }

        } else {
            // Artifact disappeared

            if (artifactWasSeenRecently) {
                // Start loss timer if not already running
                if (artifactLostTimer.milliseconds() == 0.0) {
                    artifactLostTimer.reset()
                }

                // Only reset if it's gone too long
                if (artifactLostTimer.milliseconds() > Constants.Spindexer.CONFIRM_LOSS_MS) {
                    artifactSeen = false
                    artifactWasSeenRecently = false
                }
            }
        }

        return false
    }
}

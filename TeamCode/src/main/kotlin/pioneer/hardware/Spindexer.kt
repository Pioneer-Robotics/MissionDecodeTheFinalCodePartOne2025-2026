package pioneer.hardware

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import pioneer.Constants
import pioneer.decode.Artifact
import pioneer.helpers.Chrono
import pioneer.helpers.PIDController
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sign
import kotlin.time.DurationUnit

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
   3    *    2
      /   \
     /  1  \
    /       \

*/

class Spindexer(
    private val hardwareMap: HardwareMap,
    private val motorName: String = Constants.HardwareNames.SPINDEXER_MOTOR,
    private val intakeSensorName: String = Constants.HardwareNames.INTAKE_SENSOR,
    private val _artifacts: Array<Artifact?> = Array(3) { null },
    private val overCurrentThreshold: Double = 5000.0, // TODO: Find appropriate threshold
    private val currentUnit: CurrentUnit = CurrentUnit.MILLIAMPS,
) : HardwareComponent {

    private lateinit var motor: DcMotorEx
    private lateinit var intakeSensor: RevColorSensor

    // Motor positions in radians
    enum class MotorPosition(
        val radians: Double,
    ) {
        INTAKE_1(0 * PI / 6),
        OUTTAKE_1(3 * PI / 3), // Shift down (+2)
        INTAKE_2(2 * PI / 3),
        OUTTAKE_2(5 * PI / 3), // Shift down (+2)
        INTAKE_3(4 * PI / 3),
        OUTTAKE_3(1 * PI / 3), // Shift down (+2) (wrapped)
    }

    // Indirect reference to internal artifacts array to prevent modification
    val artifacts: Array<Artifact?>
        get() = _artifacts.copyOf()

    // Current motor state
    var motorState: MotorPosition = MotorPosition.INTAKE_1

    val isOuttakePosition: Boolean
        get() =
            motorState in
                listOf(
                    MotorPosition.OUTTAKE_1,
                    MotorPosition.OUTTAKE_2,
                    MotorPosition.OUTTAKE_3,
                )

    val isIntakePosition: Boolean
        get() =
            motorState in
                listOf(
                    MotorPosition.INTAKE_1,
                    MotorPosition.INTAKE_2,
                    MotorPosition.INTAKE_3,
                )

    // Getter to check if motor has reached target position
    val reachedTarget: Boolean
        get() {
            // Compute circular error
            val error = wrapTicks(targetMotorPosition - currentMotorPosition)
            return abs(error) < Constants.Spindexer.POSITION_TOLERANCE_TICKS
        }

    // Getters for artifact storage status
    val isFull: Boolean
        get() = !_artifacts.contains(null)

    val isEmpty: Boolean
        get() = _artifacts.all { it == null }

    val numStoredArtifacts: Int
        get() = _artifacts.count { it != null }

    // Motor position accessors
    val currentMotorPosition: Int
        get() = motor.currentPosition - offsetTicks

    val targetMotorPosition: Int
        get() = (motorState.radians * ticksPerRadian).toInt()

    val currentScannedArtifact: Artifact?
        get() = scanArtifact()

    val current: Double
        get() = motor.getCurrent(currentUnit)

    // TODO: Implement in motion commands to detect jammed artifact
    val isOverCurrent: Boolean
        get() = motor.isOverCurrent()

    var checkingForNewArtifacts = true

    // Private variables
    private var offsetTicks = 0
    private var lastPower = 0.0
    private val chrono = Chrono(autoUpdate = false, units = DurationUnit.MILLISECONDS)
    private val ticksPerRadian: Int = (Constants.Spindexer.TICKS_PER_REV / (2 * PI)).toInt()
    private val motorPID =
        PIDController(
            Constants.Spindexer.KP,
            Constants.Spindexer.KI,
            Constants.Spindexer.KD,
        )
    private val artifactVisibleTimer = ElapsedTime()
    private val artifactLostTimer = ElapsedTime()
    private var artifactSeen = false
    private var artifactWasSeenRecently = false
    private var lastStoredIndex = 0
    private var manualMove = false
    private val intakePositions =
        listOf(MotorPosition.INTAKE_1, MotorPosition.INTAKE_2, MotorPosition.INTAKE_3)
    private val outtakePositions =
        listOf(MotorPosition.OUTTAKE_1, MotorPosition.OUTTAKE_2, MotorPosition.OUTTAKE_3)

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
        checkingForNewArtifacts = true
        motor =
            hardwareMap.get(DcMotorEx::class.java, motorName).apply {
                mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
                mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
                setCurrentAlert(overCurrentThreshold, currentUnit)
            }

        intakeSensor =
            RevColorSensor(hardwareMap, intakeSensorName).apply {
                init()
                gain = 20.0f
            }
    }

    /**
     * Updates the motor position to match the desired motor state.
     * Checks for new artifacts if in an intake position.
     */
    override fun update() {
        chrono.update()
        runMotorToState()
        if (checkingForNewArtifacts) checkForArtifact()
    }

    /**
     * Moves the motor to the next open intake position if available.
     * @return true if moved to the next open intake, false otherwise.
     */
    fun moveToNextOpenIntake(): Boolean {
        manualMove = false
        _artifacts.indexOfFirst { it == null }.takeIf { it != -1 }?.let {
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
        manualMove = false
        // No artifacts to outtake
        if (isEmpty) return false

        // Already at desired outtake position
        if (motorState in outtakePositions) {
            val currentIndex = outtakePositions.indexOf(motorState)
            if (artifact != null && _artifacts[currentIndex] == artifact) return true
        }

        // If no artifact specified, move to next outtake in sequence
        val nextIndex = findOuttakeIndex(artifact)

        motorState = outtakePositions[nextIndex]
        return true
    }

    /**
     * Pops (removes and returns) the artifact at the current outtake position.
     * @return the popped Artifact, or null if none present or not in outtake position
     */
    fun popCurrentArtifact(): Artifact? {
        // Only allow pop from outtake positions
        if (motorState !in outtakePositions) return null
        // Get and remove artifact at current position
        val index = positionIndex ?: return null
        val artifact = _artifacts[index]
        _artifacts[index] = null
        // Automatically move to intake if empty
        if (isEmpty) moveToNextOpenIntake()
        return artifact
    }

    fun cancelLastIntake() {
        artifacts[lastStoredIndex] = null
    }

    fun reset() {
        // Clear all stored artifacts
        for (i in _artifacts.indices) {
            _artifacts[i] = null
        }
    }

    fun moveManual(power: Double) {
        motor.power = power
        manualMove = true
    }

    fun setArtifacts(vararg artifacts: Artifact?) {
        require(artifacts.size == 3) { "Exactly 3 artifacts must be provided." }
        artifacts.forEachIndexed { index, artifact ->
            _artifacts[index] = artifact
        }
    }

    fun resetMotorPosition(resetTicks: Int = 0) {
        motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        offsetTicks = resetTicks
        motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
    }

    private fun wrapTicks(
        error: Int,
        ticksPerRev: Int = Constants.Spindexer.TICKS_PER_REV,
    ): Int {
        var e = error % ticksPerRev
        if (e > ticksPerRev / 2) e -= ticksPerRev
        if (e < -ticksPerRev / 2) e += ticksPerRev
        return e
    }

    private fun rampPower(
        desired: Double,
        dt: Double,
    ): Double {
        val maxDelta = Constants.Spindexer.MAX_POWER_RATE * dt / 1000
        val delta = desired - lastPower
        val clipped = delta.coerceIn(-maxDelta, maxDelta)
        lastPower += clipped
        return lastPower
    }

    private fun runMotorToState() {
        if (manualMove) return

        val rawError = targetMotorPosition - currentMotorPosition
        val error = wrapTicks(rawError)

        // PID -> -1..1 output
        var power = motorPID.update(error.toDouble(), chrono.dt)

        // Ramp power to prevent sudden acceleration
        power = rampPower(power, chrono.dt)

        // Static constant
        val adjustedKS = Constants.Spindexer.KS_START + Constants.Spindexer.KS_STEP * numStoredArtifacts
        power += if (abs(error) > 100) adjustedKS * sign(error.toDouble()) else 0.0

        // Apply power (inverted due to motor orientation)
        val maxPower = 0.75
        motor.power = -power.coerceIn(-maxPower, maxPower)
    }

    private fun checkForArtifact() {
        // Check for artifact if in intake position and reached target
        if (motorState in intakePositions) {
            if (scanAndStoreArtifact()) {
                // Artifact detected and stored, move to next open intake or switch mode if full
                if (!moveToNextOpenIntake()) switchMode()
            }
        }
    }

    private fun findOuttakeIndex(target: Artifact?): Int =
        target?.let {
            _artifacts
                .indexOfFirst { it == target }
                .takeIf { it != -1 }
        } ?: _artifacts.indexOfFirst { it != null }

    /**
     * Switches the motor state between intake and outtake for the current position.
     */
    private fun switchMode() {
        val index = positionIndex ?: return

        // Switch motor state to nearest opposite position
        motorState =
            if (motorState in intakePositions) {
                outtakePositions[(index - 1).mod(outtakePositions.size)]
            } else {
                intakePositions[(index + 1).mod(intakePositions.size)]
            }
    }

    /**
     * Stores the detected artifact at the current motor position index.
     */
    private fun storeArtifact(artifact: Artifact?) {
        val index = positionIndex ?: return
        lastStoredIndex = index
        _artifacts[index] = artifact
    }

    /**
     * Scans the artifact using the current sensor.
     */
    private fun scanArtifact(): Artifact? {
        if (!reachedTarget) return null
        return detectArtifact(intakeSensor)
    }

    /**
     * Detects the artifact based on color and distance readings from the sensor.
     */
    private fun detectArtifact(sensor: RevColorSensor): Artifact? {
        // Determine artifact based on hue thresholds
        return when {
            sensor.distance > 15.0 -> null
            sensor.hue < 170 && sensor.hue > 140 -> Artifact.GREEN
            sensor.hue < 250 && sensor.hue > 170 -> Artifact.PURPLE
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

    // To be used in tandem with reset(). Only to be called when something bad happens :(
    fun rescanAllArtifacts() {
        intakePositions.forEachIndexed { i, position ->
            motorState = position
            // Check if reached target and rescan artifact
            if (reachedTarget) {
                _artifacts[i] = scanArtifact()
            }
        }
    }
}

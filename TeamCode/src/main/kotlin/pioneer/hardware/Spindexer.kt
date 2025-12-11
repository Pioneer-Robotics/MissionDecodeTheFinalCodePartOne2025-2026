package pioneer.hardware

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
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

    private enum class VisibilityState {
        IDLE,
        VISIBLE,
        LOSING,
    }

    // Indirect reference to internal artifacts array to prevent modification
    var artifacts: Array<Artifact?>
        get() = _artifacts.copyOf()
        set(value: Array<Artifact?>) {
            require(value.size == 3) { "Exactly 3 artifacts must be provided." }
            value.forEachIndexed { index, artifact ->
                _artifacts[index] = artifact
            }
    }

    // Current motor state
    var motorState: MotorPosition = MotorPosition.INTAKE_1

    private val intakePositions =
        listOf(
            MotorPosition.INTAKE_1,
            MotorPosition.INTAKE_2,
            MotorPosition.INTAKE_3,
        )

    private val outtakePositions =
        listOf(
            MotorPosition.OUTTAKE_1,
            MotorPosition.OUTTAKE_2,
            MotorPosition.OUTTAKE_3,
        )

    private val positions: List<MotorPosition>
        get() = if (isIntakePosition) intakePositions else outtakePositions

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

    val isIntakePosition: Boolean
        get() = motorState in intakePositions

    val isOuttakePosition: Boolean
        get() = motorState in outtakePositions

    val hasOpenIntake: Boolean
        get() = _artifacts.any { it == null }

    val remainingIntakeSlots: Int
        get() = _artifacts.count { it == null }

    val nextOuttakeIndex: Int?
        get() = _artifacts.indexOfFirst { it != null }.takeIf { it != -1 }

    val nextIntakeIndex: Int?
        get() = _artifacts.indexOfFirst { it == null }.takeIf { it != -1 }

    fun containsArtifact(artifact: Artifact): Boolean = _artifacts.any { it == artifact }

    private fun outtakeIndexFor(artifact: Artifact?): Int? =
        artifact?.let { desired ->
            _artifacts.indexOfFirst { it == desired }.takeIf { it != -1 }
        } ?: nextOuttakeIndex

    var checkingForNewArtifacts = true

    // Private variables
    private var offsetTicks = 0
    private var lastPower = 0.0
    private val pidTimer = Chrono(autoUpdate = false, units = DurationUnit.MILLISECONDS)
    private val ticksPerRadian: Int = (Constants.Spindexer.TICKS_PER_REV / (2 * PI)).toInt()
    private val motorPID =
        PIDController(
            Constants.Spindexer.KP,
            Constants.Spindexer.KI,
            Constants.Spindexer.KD,
        )
    private val artifactVisibleTimer = Chrono(autoUpdate = false, units = DurationUnit.MILLISECONDS)
    private val artifactLostTimer = Chrono(autoUpdate = false, units = DurationUnit.MILLISECONDS)
    private var visibilityState = VisibilityState.IDLE
    private var lastStoredIndex = 0
    private var manualMove = false

    /**
     * Returns the index of the current motor position in the intake/outtake lists.
     */
    private val positionIndex: Int?
        get() {
            // Only return index if at target position
            if (!reachedTarget) return null
            // Find index based on current motor state
            return positions.indexOf(motorState).takeIf { it >= 0 }
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
        pidTimer.update()
        runMotorToState()
        if (checkingForNewArtifacts) checkForArtifact()
    }

    /**
     * Moves the motor to the next open intake position if available.
     * @return true if moved to the next open intake, false otherwise.
     */
    fun moveToNextOpenIntake(): Boolean {
        manualMove = false
        return moveToNext(switchMode = !isIntakePosition)
    }

    /**
     * Moves the motor to the next outtake position in sequence.
     * @param artifact Optional artifact to prioritize moving to its outtake position.
     */
    fun moveToNextOuttake(artifact: Artifact? = null): Boolean {
        manualMove = false
        return moveToNext(switchMode = !isOuttakePosition, artifact = artifact)
    }

    /**
     * Pops (removes and returns) the artifact at the current position based on the mode.
     * If in outtake mode, removes the artifact at the current outtake position.
     * If in intake mode, cancels the last intake.
     * @return the popped Artifact, or null if none present or invalid state
     */
    fun popCurrentArtifact(): Artifact? {
        return if (isOuttakePosition) {
            // Handle outtake mode
            val index = positionIndex ?: return null
            val artifact = _artifacts[index]
            _artifacts[index] = null
            // Automatically move to intake if empty
            if (isEmpty) moveToNextOpenIntake()
            artifact
        } else if (isIntakePosition) {
            // Handle intake mode (cancel last intake)
            val artifact = _artifacts[lastStoredIndex]
            _artifacts[lastStoredIndex] = null
            artifact
        } else {
            null
        }
    }

    fun moveManual(power: Double) {
        motor.power = power
        manualMove = true
    }

    fun resumeAutoControl() {
        manualMove = false
    }

    fun resetArtifacts() {
        _artifacts.fill(null)
    }

    fun resetMotorPosition(resetTicks: Int = 0) {
        motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        offsetTicks = resetTicks
        motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
    }

    fun resetAll() {
        resetArtifacts()
        resetMotorPosition()
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
        var power = motorPID.update(error.toDouble(), pidTimer.dt)

        // Ramp power to prevent sudden acceleration
        power = rampPower(power, pidTimer.dt)

        // Static constant
        val adjustedKS = Constants.Spindexer.KS_START + Constants.Spindexer.KS_STEP * numStoredArtifacts
        power += if (abs(error) > 100) adjustedKS * sign(error.toDouble()) else 0.0

        // Apply power (inverted due to motor orientation)
        val maxPower = 0.75
        motor.power = -power.coerceIn(-maxPower, maxPower)
    }

    private fun checkForArtifact() {
        // Check for artifact if in intake position and reached target

        if (!isIntakePosition || !reachedTarget) return
        // Artifact detected and stored, move to next open intake or switch mode if full

        if (scanAndStoreArtifact() && !moveToNextOpenIntake()) switchMode()
    }

    /**
     * Switches the motor state between intake and outtake for the current position.
     */
    private fun switchMode() {
        positionIndex?.let { index ->
            motorState =
                if (isIntakePosition) {
                    outtakePositions[(index - 1).mod(outtakePositions.size)]
                } else {
                    intakePositions[(index + 1).mod(intakePositions.size)]
                }
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
            sensor.hue.toDouble() in 140.0..170.0 -> Artifact.GREEN
            sensor.hue.toDouble() in 170.0..250.0 -> Artifact.PURPLE
            else -> null
        }
    }

    /**
     * Scans and stores the artifact at the current motor position.
     */
    private fun scanAndStoreArtifact(): Boolean {
        return scanArtifact()?.let { artifact ->
            onArtifactSeen()
            if (artifactVisibleTimer.peek() >= Constants.Spindexer.CONFIRM_INTAKE_MS) {
                storeArtifact(artifact)
                resetArtifactVisibility()
                return true
            }
            false
        } ?: handleArtifactLoss()
    }

    private fun onArtifactSeen() {
        if (visibilityState != VisibilityState.VISIBLE) {
            artifactVisibleTimer.reset()
        }
        visibilityState = VisibilityState.VISIBLE
        artifactLostTimer.reset()
    }

    private fun resetArtifactVisibility() {
        visibilityState = VisibilityState.IDLE
        artifactVisibleTimer.reset()
        artifactLostTimer.reset()
    }

    private fun handleArtifactLoss(): Boolean {
        when (visibilityState) {
            VisibilityState.IDLE -> return false
            VisibilityState.VISIBLE -> {
                visibilityState = VisibilityState.LOSING
                artifactLostTimer.reset()
            }
            VisibilityState.LOSING -> {
                if (artifactLostTimer.peek() >= Constants.Spindexer.CONFIRM_LOSS_MS) {
                    resetArtifactVisibility()
                }
            }
        }
        return false
    }

    /**
     * Steps the spindexer to the next slot, optionally swapping intake/outtake first.
     *
     * Intake: moves to the next empty slot (returns false if full and not switching).
     * Outtake: moves to a slot with the requested artifact if provided, otherwise the first non-null.
     * When `switchMode` is true, the intake/outtake side is toggled before picking a target.
     */
    private fun moveToNext(
        switchMode: Boolean = false,
        artifact: Artifact? = null,
    ): Boolean {
        if (switchMode) switchMode() // Switch between intake and outtake before selecting
        if (isIntakePosition && !hasOpenIntake) return false // No open intake available

        val targetIndex =
            if (isIntakePosition) {
                nextIntakeIndex
            } else {
                outtakeIndexFor(artifact)
            } ?: return false

        val currentIndex = positions.indexOf(motorState)
        if (currentIndex == targetIndex && currentIndex != -1) return true

        motorState = positions[targetIndex]
        return true
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

package pioneer.hardware.spindexer

import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.ElapsedTime
import pioneer.Constants
import pioneer.decode.Artifact
import pioneer.hardware.HardwareComponent
import pioneer.hardware.RevColorSensor
import pioneer.helpers.FileLogger

/**
 * PASSIVE SPINDEXER - Simplified Version
 *
 * Keeps ALL existing functionality and button mappings!
 * Just adapts the underlying motion to work with passive ramp system.
 *
 * MECHANICAL BEHAVIOR:
 * - INTAKE direction: Balls collected, stay in spindexer
 * - SHOOTING direction: Passive ramp engages, balls shoot
 *
 * The key difference from active system:
 * - Multi-shot now uses continuous rotation in SHOOTING direction
 * - Much faster than position-based active kicker!
 */
class Spindexer(
    private val hardwareMap: HardwareMap,
    private val motorName: String = Constants.HardwareNames.SPINDEXER_MOTOR,
    private val intakeSensorName: String = Constants.HardwareNames.INTAKE_SENSOR,
) : HardwareComponent {
    // --- Subsystems --- //
    private lateinit var motion: SpindexerMotionController
    private lateinit var detector: ArtifactDetector
    private val indexer = ArtifactIndexer()

    // --- NEW: Multi-shot state for passive rapid fire --- //
    private var rapidFireActive = false
    private val rapidFireTimer = ElapsedTime()

    // --- Positions --- //
    private val intakePositions =
        listOf(
            SpindexerMotionController.MotorPosition.INTAKE_1,
            SpindexerMotionController.MotorPosition.INTAKE_2,
            SpindexerMotionController.MotorPosition.INTAKE_3
        )

    private val outtakePositions =
        listOf(
            SpindexerMotionController.MotorPosition.OUTTAKE_1,
            SpindexerMotionController.MotorPosition.OUTTAKE_2,
            SpindexerMotionController.MotorPosition.OUTTAKE_3
        )

    var manualOverride = false

    // --- Artifact Data --- //
    val artifacts: Array<Artifact?> get() = indexer.snapshot()

    val isFull: Boolean get() = indexer.isFull
    val isEmpty: Boolean get() = indexer.isEmpty
    val numStoredArtifacts: Int get() = indexer.count

    // --- Motor Getters --- //
    val motorState: SpindexerMotionController.MotorPosition get() = motion.target
    val reachedTarget: Boolean get() = motion.reachedTarget
    val withinDetectionTolerance: Boolean get() = motion.withinDetectionTolerance
    val currentMotorTicks: Int get() = motion.currentTicks
    val targetMotorTicks: Int get() = motion.targetTicks
    val currentMotorVelocity: Double get() = motion.velocity
    val closestMotorPosition get() = motion.closestPosition
    val isOuttakePosition get() = motion.target in outtakePositions
    val isIntakePosition get() = motion.target in intakePositions

    // --- NEW: Multi-shot status --- //
    val isRapidFiring: Boolean get() = rapidFireActive

    // --- Initialization --- //
    override fun init() {
        val motor = hardwareMap.get(DcMotorEx::class.java, motorName)
        motion = SpindexerMotionController(motor)
        motion.init()

        val sensor = RevColorSensor(hardwareMap, intakeSensorName).apply {
            init()
            gain = 20.0f
        }
        detector = ArtifactDetector(sensor)
    }

    // --- Update Loop --- //
    override fun update() {
        // Handle rapid fire (multi-shot) if active
        if (rapidFireActive) {
            updateRapidFire()
            return  // Don't do normal updates during rapid fire
        }

        // Normal position-based control
        motion.update()
        motion.manualOverride = this.manualOverride
        checkForArtifact()
    }

    // --- NEW: Rapid Fire for Multi-Shot --- //

    /**
     * Start rapid fire (replaces old multi-shot state machine)
     * Just spins in SHOOTING direction continuously
     * Much faster than position-based shooting!
     */
    fun startRapidFire() {
        if (isEmpty) {
            FileLogger.warn("Spindexer", "No balls to rapid fire!")
            return
        }

        FileLogger.info("Spindexer", "Starting RAPID FIRE - ${numStoredArtifacts} balls")
        rapidFireActive = true
        rapidFireTimer.reset()

        // Start continuous rotation in SHOOTING direction
        motion.startContinuousRotation(
            SpindexerMotionController.SpindexerDirection.SHOOTING,
            Constants.Spindexer.RAPID_FIRE_SPEED
        )
    }

    /**
     * Stop rapid fire
     */
    fun stopRapidFire() {
        if (!rapidFireActive) return

        FileLogger.info("Spindexer", "Stopping RAPID FIRE")
        rapidFireActive = false
        motion.stopContinuousRotation()

        // Clear all balls (assume they all shot)
        indexer.resetAll()

        // Move to next intake position
        moveToNextOpenIntake()
    }

    private fun updateRapidFire() {
        // Auto-stop after duration
        if (rapidFireTimer.milliseconds() > Constants.Spindexer.RAPID_FIRE_DURATION_MS) {
            FileLogger.info("Spindexer", "RAPID FIRE complete (timeout)")
            stopRapidFire()
        }

        // Keep the motor spinning
        motion.update()
    }

    // --- Public Commands (UNCHANGED from original) --- //
    fun moveToNextOpenIntake(): Boolean {
        manualOverride = false
        rapidFireActive = false  // NEW: Stop rapid fire if active
        val index = indexer.nextOpenIntakeIndex() ?: return false
        motion.target = intakePositions[index]
        return true
    }

    fun moveToNextOuttake(artifact: Artifact? = null): Boolean {
        manualOverride = false
        rapidFireActive = false  // NEW: Stop rapid fire if active
        val index = indexer.findOuttakeIndex(artifact) ?: return false
        motion.target = outtakePositions[index]
        return true
    }

    fun moveToPosition(position: SpindexerMotionController.MotorPosition) {
        manualOverride = false
        rapidFireActive = false  // NEW: Stop rapid fire if active
        motion.target = position
    }

    fun popCurrentArtifact(autoSwitchToIntake: Boolean = true): Artifact? {
        if (motion.target !in outtakePositions) return null
        val index = outtakePositions.indexOf(motion.target)
        val artifact = indexer.pop(index)
        if (autoSwitchToIntake){
            if (indexer.isEmpty) moveToNextOpenIntake()
        }
        return artifact
    }

    fun popArtifact(index: Int): Artifact? {
        val artifact = indexer.pop(index)
        if (indexer.isEmpty) moveToNextOpenIntake()
        return artifact
    }

    fun cancelLastIntake() {
        indexer.popLastIntake()
    }

    fun reset() {
        indexer.resetAll()
        rapidFireActive = false  // NEW: Stop rapid fire
        motion.stopContinuousRotation()  // NEW: Stop motor
    }

    fun moveManual(power: Double) {
        manualOverride = true
        rapidFireActive = false  // NEW: Stop rapid fire
        motion.moveManual(power)
    }

    fun resetMotorPosition(resetTicks: Int = 0) {
        motion.calibrateEncoder(resetTicks)
    }

    fun setArtifacts(vararg artifacts: Artifact?) {
        indexer.setAll(*artifacts)
    }

    // --- Private Helpers (UNCHANGED) --- //
    private fun checkForArtifact() {
        if (motion.target !in intakePositions) return
        if (!motion.withinDetectionTolerance) return
        if (!motion.withinVelocityTolerance) return

        val index = intakePositions.indexOf(motion.target)
        val detected = detector.detect()

        if (indexer.processIntake(index, detected)) {
            if (!moveToNextOpenIntake()) switchMode()
        }
    }

    private fun switchMode() {
        val index = intakePositions.indexOf(motion.target)
        if (motion.target in intakePositions) {
            motion.target = outtakePositions[(index - 1).mod(outtakePositions.size)]
        } else {
            motion.target = intakePositions[(index + 1).mod(intakePositions.size)]
        }
    }
}
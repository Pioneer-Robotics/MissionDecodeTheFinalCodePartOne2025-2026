package pioneer.hardware.spindexer

import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import pioneer.Constants
import pioneer.decode.Artifact
import pioneer.decode.Motif
import pioneer.hardware.HardwareComponent
import pioneer.hardware.RevColorSensor
import pioneer.helpers.next
import kotlin.math.roundToInt

class Spindexer(
    private val hardwareMap: HardwareMap,
    private val motorName: String = Constants.HardwareNames.SPINDEXER_MOTOR,
    private val intakeSensorName: String = Constants.HardwareNames.INTAKE_SENSOR,
) : HardwareComponent {
    // --- Spindexer State --- //
    private enum class State {
        READY,
        INTAKE,
    }

    private var state = State.INTAKE

    // --- Subsystems --- //
    private lateinit var detector: ArtifactDetector
    private lateinit var motion: SpindexerMotionController
    private val indexer = ArtifactIndexer()

    // --- Positions --- //
    var manualOverride = false
    var isSorting = false

    private val ticksPerArtifact: Int
        get() = (Constants.Spindexer.TICKS_PER_REV / 3.0).roundToInt()

    // --- Artifact Data --- //
    val artifacts: Array<Artifact?> get() = indexer.snapshot()

    val isFull: Boolean get() = indexer.isFull
    val isEmpty: Boolean get() = indexer.isEmpty
    val numStoredArtifacts: Int get() = indexer.count

    // --- Motor Getters --- //
    val currentMotorTicks: Int get() = motion.currentTicks
    val targetMotorTicks: Int get() = motion.targetTicks
    val reachedTarget: Boolean get() = motion.reachedTarget
    val motorState: SpindexerMotionController.MotorPosition get() = motion.target

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
        motion.update()
        when (state) {
            State.INTAKE -> intakeState()
            State.READY -> readyState()
        }
    }

    // --- Public Commands --- //
    fun moveManual(power: Double) {
        motion.moveManual(power)
    }

    fun moveToPosition(position: SpindexerMotionController.MotorPosition) {
        motion.target = position
    }

    fun setArtifacts(vararg values: Artifact?) {
        indexer.setAll(*values)
    }

    fun moveToNextOpenIntake(): Boolean {
        state = State.INTAKE
        manualOverride = false
        motion.stopShooting()
        val index = indexer.nextOpenIntakeIndex() ?: return false
        motion.target = motion.intakePositions[index]
        return true
    }

    fun readyOuttake(targetMotif: Motif? = null) {
        state = State.READY
        motion.stopShooting()
        // Return if there aren't any artifacts
        val startIndex = indexer.motifStartIndex(targetMotif) ?: return
        motion.target = motion.outtakePositions[startIndex]
    }

    fun shootNext() {
        if (state == State.INTAKE) return
        if (motion.isShooting) return
        // Rotate 120 degrees at constant speed (no PID) then hold next outtake.
        motion.target = motion.target.offset(2)
        motion.startShooting(ticksPerArtifact, Constants.Spindexer.SHOOT_POWER)
    }

    fun shootAll() {
        if (state == State.INTAKE) return
        if (motion.isShooting) return
        // Rotate a full revolution at constant speed (no PID) to shoot all 3.
        motion.startShooting(Constants.Spindexer.TICKS_PER_REV.roundToInt(), Constants.Spindexer.SHOOT_POWER)
    }

    fun resetMotorPosition(resetTicks: Int) {
        motion.calibrateEncoder(resetTicks)
    }

    fun reset() {
        indexer.resetAll()
    }

    // --- Private Helpers --- //
    private fun intakeState() {
        // Make sure the spindexer is at it's target position
        if (!motion.withinDetectionTolerance) return
        if (!motion.withinVelocityTolerance) return

        // Detect artifact
        val detected = detector.detect()
        val index = motion.intakePositions.indexOf(motion.target)
        if (index == -1) return // Sanity check: make sure we aren't in an outtake
        if (indexer.processIntake(index, detected)) {
            // If there aren't any more open intake positions
            if (!moveToNextOpenIntake()) readyOuttake()
        }
    }

    private fun readyState() {
        // TODO: check if an artifact has been shot (then pop it)
    }
}

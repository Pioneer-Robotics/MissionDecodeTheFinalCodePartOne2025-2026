package pioneer.hardware.spindexer

import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import pioneer.Constants
import pioneer.decode.Artifact
import pioneer.decode.Motif
import pioneer.hardware.HardwareComponent
import pioneer.hardware.RevColorSensor
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
    val isShooting: Boolean get() = motion.isShooting

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

    fun moveToPosition(positionIndex: Int) {
        motion.positionIndex = positionIndex
    }

    fun setArtifacts(vararg values: Artifact?) {
        indexer.setAll(*values)
    }

    fun moveToNextOpenIntake(): Boolean {
        state = State.INTAKE
        manualOverride = false
        motion.stopShooting()
        val index = indexer.nextOpenIntakeIndex(motion.positionIndex) ?: return false
        motion.positionIndex = index
        return true
    }

    fun readyOuttake(targetMotif: Motif? = null) {
        state = State.READY
        motion.stopShooting()
        // Return if there aren't any artifacts
        val startIndex = indexer.motifStartIndex(targetMotif, motion.positionIndex) ?: return
        motion.positionIndex = startIndex
    }

    fun shootNext(shootPower: Double = Constants.Spindexer.SHOOT_POWER_CLOSE) {
        if (state == State.INTAKE) return
        if (motion.isShooting) return
        indexer.pop(motion.positionIndex)
        // Rotate 120 degrees at constant speed (no PID) then hold next outtake.
        motion.positionIndex += 1
        motion.startShooting(ticksPerArtifact, shootPower)
    }

    fun shootAll(shootPower: Double = Constants.Spindexer.SHOOT_POWER_CLOSE) {
        if (state == State.INTAKE) return
        if (motion.isShooting) return
        // Rotate a full revolution plus one at constant speed (no PID) to shoot all 3.
        motion.startShooting(Constants.Spindexer.TICKS_PER_REV.roundToInt() + ticksPerArtifact, shootPower)
        indexer.resetAll()
    }

//    fun popCurrentArtifact(autoSwitchToIntake: Boolean = true): Artifact? {
//        if (motion.target !in outtakePositions) return null
//        val index = outtakePositions.indexOf(motion.target) //Needs to be current ticks,
//        val artifact = indexer.pop(index)
//        if (autoSwitchToIntake){
//            if (indexer.isEmpty) moveToNextOpenIntake()
//        }
//        return artifact
//    }

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
        val index = motion.positionIndex
        if (indexer.processIntake(index, detected)) {
            // If there aren't any more open intake positions
            if (!moveToNextOpenIntake()) readyOuttake()
        }
    }

    private fun readyState() {
        // TODO: check if an artifact has been shot (then pop it)
    }
}

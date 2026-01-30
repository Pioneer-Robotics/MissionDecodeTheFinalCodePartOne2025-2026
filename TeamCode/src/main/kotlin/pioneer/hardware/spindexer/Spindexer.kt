package pioneer.hardware.spindexer

import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import pioneer.Constants
import pioneer.decode.Artifact
import pioneer.hardware.HardwareComponent
import pioneer.hardware.RevColorSensor

class Spindexer(
    private val hardwareMap: HardwareMap,
    private val motorName: String = Constants.HardwareNames.SPINDEXER_MOTOR,
    private val intakeSensorName: String = Constants.HardwareNames.INTAKE_SENSOR,
) : HardwareComponent {
    // --- Subsystems --- //
    private lateinit var motion: SpindexerMotionController
    private lateinit var detector: ArtifactDetector
    private val indexer = ArtifactIndexer()

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
        motion.manualOverride = this.manualOverride
        checkForArtifact()
    }

    // --- Public Commands --- //
    fun moveToNextOpenIntake(): Boolean {
        manualOverride = false
        val index = indexer.nextOpenIntakeIndex() ?: return false
        motion.target = intakePositions[index]
        return true
    }

    fun moveToNextOuttake(artifact: Artifact? = null): Boolean {
        manualOverride = false
        val index = indexer.findOuttakeIndex(artifact) ?: return false
        motion.target = outtakePositions[index]
        return true
    }

    fun moveToPosition(position: SpindexerMotionController.MotorPosition) {
        manualOverride = false
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
    }

    fun moveManual(power: Double) {
        manualOverride = true
        motion.moveManual(power)
    }

    fun resetMotorPosition(resetTicks: Int = 0) {
        motion.calibrateEncoder(resetTicks)
    }

    fun setArtifacts(vararg artifacts: Artifact?) {
        indexer.setAll(*artifacts)
    }

    // --- Private Helpers --- //
    private fun checkForArtifact() {
        if (motion.target !in intakePositions) return
        if (!motion.withinDetectionTolerance) return

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

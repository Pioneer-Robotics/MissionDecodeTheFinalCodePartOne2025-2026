package pioneer.hardware.spindexer

import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.ElapsedTime
import pioneer.Constants
import pioneer.decode.Artifact
import pioneer.hardware.HardwareComponent
import pioneer.hardware.RevColorSensor
import pioneer.helpers.FileLogger

class Spindexer(
    private val hardwareMap: HardwareMap,
    private val motorName: String = Constants.HardwareNames.SPINDEXER_MOTOR,
    private val intakeSensorName: String = Constants.HardwareNames.INTAKE_SENSOR,
) : HardwareComponent {
    private lateinit var motion: SpindexerMotionController
    private lateinit var detector: ArtifactDetector
    private val indexer = ArtifactIndexer()

    private var rapidFireActive = false
    private val rapidFireTimer = ElapsedTime()

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

    val artifacts: Array<Artifact?> get() = indexer.snapshot()

    val isFull: Boolean get() = indexer.isFull
    val isEmpty: Boolean get() = indexer.isEmpty
    val numStoredArtifacts: Int get() = indexer.count

    val motorState: SpindexerMotionController.MotorPosition get() = motion.target
    val reachedTarget: Boolean get() = motion.reachedTarget
    val withinDetectionTolerance: Boolean get() = motion.withinDetectionTolerance
    val currentMotorTicks: Int get() = motion.currentTicks
    val targetMotorTicks: Int get() = motion.targetTicks
    val currentMotorVelocity: Double get() = motion.velocity
    val closestMotorPosition get() = motion.closestPosition
    val isOuttakePosition get() = motion.target in outtakePositions
    val isIntakePosition get() = motion.target in intakePositions

    val isReadyToShoot: Boolean get() = motion.isReadyToShoot
    val isShooting: Boolean get() = motion.isShooting

    val isRapidFiring: Boolean get() = rapidFireActive

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

    override fun update() {
        if (rapidFireActive) {
            updateRapidFire()
            return
        }

        motion.update()
        motion.manualOverride = this.manualOverride
        checkForArtifact()
    }

    fun triggerSingleShot() {
        motion.triggerSingleShot()
    }

    fun startRapidFire() {
        if (isEmpty) {
            FileLogger.warn("Spindexer", "No balls to rapid fire!")
            return
        }

        FileLogger.info("Spindexer", "Starting RAPID FIRE - ${numStoredArtifacts} balls")
        rapidFireActive = true
        rapidFireTimer.reset()

        motion.startContinuousRotation(
            SpindexerMotionController.SpindexerDirection.SHOOTING,
            Constants.Spindexer.RAPID_FIRE_SPEED
        )
    }

    fun stopRapidFire() {
        if (!rapidFireActive) return

        FileLogger.info("Spindexer", "Stopping RAPID FIRE")
        rapidFireActive = false
        motion.stopContinuousRotation()

        indexer.resetAll()

        moveToNextOpenIntake()
    }

    private fun updateRapidFire() {
        if (rapidFireTimer.milliseconds() > Constants.Spindexer.RAPID_FIRE_DURATION_MS) {
            FileLogger.info("Spindexer", "RAPID FIRE complete (timeout)")
            stopRapidFire()
        }

        motion.update()
    }

    fun moveToNextOpenIntake(): Boolean {
        manualOverride = false
        rapidFireActive = false
        val index = indexer.nextOpenIntakeIndex() ?: return false
        motion.target = intakePositions[index]
        return true
    }

    fun moveToNextOuttake(artifact: Artifact? = null): Boolean {
        manualOverride = false
        rapidFireActive = false

        val index = indexer.findOuttakeIndex(artifact)
        if (index == null) {
            FileLogger.warn("Spindexer", "No ${artifact ?: "any"} ball found!")
            return false
        }

        FileLogger.info("Spindexer", "Moving to ${artifact ?: "next"} ball at position $index")
        motion.target = outtakePositions[index]
        return true
    }

    fun moveToPosition(position: SpindexerMotionController.MotorPosition) {
        manualOverride = false
        rapidFireActive = false
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
        rapidFireActive = false
        motion.stopContinuousRotation()
    }

    fun moveManual(power: Double) {
        manualOverride = true
        rapidFireActive = false
        motion.moveManual(power)
    }

    fun resetMotorPosition(resetTicks: Int = 0) {
        motion.calibrateEncoder(resetTicks)
    }

    fun setArtifacts(vararg artifacts: Artifact?) {
        indexer.setAll(*artifacts)
    }

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
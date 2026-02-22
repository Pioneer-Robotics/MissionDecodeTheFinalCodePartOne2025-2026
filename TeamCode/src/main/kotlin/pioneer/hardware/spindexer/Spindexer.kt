package pioneer.hardware.spindexer

import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.ElapsedTime
import pioneer.Constants
import pioneer.decode.Artifact
import pioneer.decode.Motif
import pioneer.hardware.HardwareComponent
import pioneer.hardware.RevColorSensor
import pioneer.helpers.FileLogger
import kotlin.math.E

class Spindexer(
    private val hardwareMap: HardwareMap,
    private val motorName: String = Constants.HardwareNames.SPINDEXER_MOTOR,
    private val intakeSensorName: String = Constants.HardwareNames.INTAKE_SENSOR,
) : HardwareComponent {
    private lateinit var detector: ArtifactDetector
    private lateinit var motion: SpindexerMotionController
    private val _artifacts = arrayOfNulls<Artifact?>(3)
    private val intakeDir = if (Constants.Spindexer.OUTTAKE_IS_POSITIVE) -1 else 1
    private val outtakeDir = -intakeDir
    private val detectionTimer = ElapsedTime()
    private var isIntake = true

    // --- Public State --- //
    val artifacts: Array<Artifact?> get() = _artifacts
    val isFull: Boolean
        get() = artifacts.none { it == null }
    val isEmpty: Boolean
        get() = artifacts.all { it == null }
    val count: Int
        get() = artifacts.count { it != null }
    val errorTicks: Int
        get() = motion.errorTicks


    // --- Motor Getters --- //
    val currentMotorTicks: Int get() = motion.currentTicks
    val targetMotorTicks: Int get() = motion.targetTicks
    val reachedTarget: Boolean get() = motion.reachedTarget
    val isManualOverride: Boolean get() = motion.manualOverride

    // --- Initialization --- //
    override fun init() {
        val motor = hardwareMap.get(DcMotorEx::class.java, motorName)
        motion = SpindexerMotionController(motor)

        val sensor = RevColorSensor(hardwareMap, intakeSensorName).apply {
            init()
            gain = 20.0f
        }
        detector = ArtifactDetector(sensor)
        moveToNextOpenIntake()
    }

    override fun update() {
        motion.update()
        if (!isIntake) return
        // Update artifact states based on the detector
        val detected = detector.detect()
        if (detected == null || !motion.reachedTarget || artifacts[motion.positionIndex] != null) {
            // Reset the timer if there isn't an artifact to be indexed
            detectionTimer.reset()
        }
        if (detectionTimer.milliseconds() > Constants.Spindexer.CONFIRM_INTAKE_MS) {
            // If the artifact has been detected for long enough, confirm the intake
            _artifacts[motion.positionIndex] = detected
            moveToNextOpenIntake()
        }
    }

    // --- Public Commands --- //
    fun reset() {
        motion.manualOverride = false
        motion.setTarget(0, intakeDir)
        for (i in 0..2) {
            _artifacts[i] = null
        }
    }

    fun moveToIndex(index: Int, direction: Int = 0) {
        motion.setTarget(index, direction)
    }

    fun resetMotorPosition(ticks: Int) {
        motion.calibrateEncoder(ticks)
    }

    fun moveManual(power: Double) {
        motion.moveManual(power)
    }

    fun setArtifacts(vararg values: Artifact?) {
        for (i in 0..2) {
            _artifacts[i] = values.getOrNull(i)
        }
    }

    fun shootNext() {
        isIntake = false
        // Remove artifact
        _artifacts[Math.floorMod(motion.positionIndex + intakeDir, 3)] = null
        // Command the motion controller to shoot by moving in the outtake direction
        val newTarget = Math.floorMod(motion.positionIndex + outtakeDir, 3)
        motion.setTarget(newTarget, outtakeDir)
    }

    fun moveToNextOpenIntake(): Boolean {
        isIntake = true
        for (i in 0..2) {
            // Check current position first, then in the direction of intake
            val checkPosition = Math.floorMod(motion.positionIndex + i * intakeDir, 3)
            if (_artifacts[checkPosition] == null) {
                // Move to the position of the open intake
                motion.setTarget(checkPosition, intakeDir)
                return true
            }
        }
        return false
    }

    fun readyOuttake(motif: Motif?) {
        // Move to the position of the next artifact to shoot, if not already there
        // TODO: Doing this explicitly for now; there be a better way
        isIntake = false
        var targetIndex = 0
        if (isEmpty) return // No artifacts, so no need to move
        if (motif == null) {
            // Don't care about the color
            when (count) {
                1 -> {
                    // Move to the position of the one artifact
                    targetIndex = _artifacts.indexOfFirst { it != null }
                }
                2 -> {
                    // Move to the position of the first consecutive artifact
                    targetIndex = when {
                        _artifacts[0] != null && _artifacts[1] != null -> 0
                        _artifacts[1] != null && _artifacts[2] != null -> 1
                        else -> 2
                    }
                }
                3 -> {
                    // All three _artifacts; don't move
                    return
                }
            }
        } else {
            // There is a target motif; move to the position that best matches it
            when (count) {
                1 -> {
                    // Move to the position of the one artifact
                    targetIndex = _artifacts.indexOfFirst { it != null }
                }
                2 -> {
                    // Move to the position of the first consecutive artifact
                    // FIXME: Doesn't look for motif match; just picks the first consecutive pair.
                    // Spindexer shooting would get messed up if there was a gap between artifacts
                    targetIndex = when {
                        _artifacts[0] != null && _artifacts[1] != null -> 0
                        _artifacts[1] != null && _artifacts[2] != null -> 1
                        else -> 2
                    }
                }
                3 -> {
                    // Find the best match for the motif among the three possible rotations
                    // Look starting at the current position and moving in the direction of intake
                    var bestScore = -1
                    var bestIndex = 0
                    val pattern = motif.getPattern()
                    for (i in 0..2) {
                        val checkIndex = Math.floorMod(motion.positionIndex + i * intakeDir, 3)
                        // Score is number of matches with the motif pattern
                        val score = (0..2).count { j ->
                            val artifact = _artifacts[Math.floorMod(checkIndex + j, 3)]
                            val expected = pattern[j]
                            artifact == expected
                        }
                        if (score > bestScore) {
                            bestScore = score
                            bestIndex = checkIndex
                        }
                    }
                    targetIndex = bestIndex
                }
            }
        }
        // Shift the targetIndex in the direction of outtake
        // because MotionController expects the index of the intake slot to move to
        targetIndex += outtakeDir
        targetIndex = Math.floorMod(targetIndex, 3)
        // Still move in intakeDir to not shoot
        motion.setTarget(targetIndex, intakeDir)
    }
}
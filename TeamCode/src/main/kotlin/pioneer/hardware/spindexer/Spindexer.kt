package pioneer.hardware.spindexer

import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.ElapsedTime
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
    val finishedShot: Boolean get() = motion.justStoppedShooting
    val delayTimer = ElapsedTime()
    var readyForNextShot = true
    var shotCounter = 0
    var shootAllCommanded = false
        private set
    var requestedShootPower = Constants.Spindexer.SHOOT_POWER
    var launchConditionsMetGlobal = false
    val ticksPerArtifact: Int
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
        // Only run shootAll when commanded and we're in READY
        if (shootAllCommanded && state == State.READY) {
            handleShootAll(requestedShootPower)
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

        /**
         * Attempt to start the next shot. Returns true if a shot was actually started.
         */
    fun shootNext(shootPower: Double = Constants.Spindexer.SHOOT_POWER): Boolean {
        if (state == State.INTAKE) return false
        if (motion.isShooting) return false

        // Pop the artifact at current outtake position (if any)
        indexer.pop(motion.positionIndex)

        // Rotate 120 degrees at constant speed (no PID) then hold next outtake.
        motion.positionIndex = (motion.positionIndex + 1) % 3
        motion.startShooting(ticksPerArtifact, shootPower)
        return true
    }

    fun handleShootAll(shootPower: Double = Constants.Spindexer.SHOOT_POWER ){
        // Guard: must be in READY
        if (state != State.READY) return

        //    // If we've already issued 3 shots, wait for the last shot to finish before clearing
        if (shotCounter>=3) {
            if (!motion.isShooting) {
                // All shots finished
                shotCounter = 0
                shootAllCommanded = false
                readyForNextShot = true
            }
            return
        }
        if (readyForNextShot && !motion.isShooting){
            val started = shootNext(shootPower)
            if (started){
                shotCounter += 1
                readyForNextShot = false
            }
        }

        if (isShooting){
            delayTimer.reset()
        }

        if (delayTimer.seconds() > Constants.Spindexer.SHOOT_ALL_DELAY && launchConditionsMetGlobal){
            readyForNextShot = true
        }
    }

    fun requestShootAll(launchConditionsMet: Boolean){
        launchConditionsMetGlobal = launchConditionsMet
        shootAllCommanded = true
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
    fun shootAll(shootPower: Double = Constants.Spindexer.SHOOT_POWER) {
        // Ensure we are in READY when running; caller should call readyOuttake first.
        if (state != State.READY) return
        // Initialize shoot-all state
        shootAllCommanded = true
        shotCounter = 0
        readyForNextShot = true
        delayTimer.reset()
        requestedShootPower = shootPower
    }

    fun updateLaunchConditions(launchConditionsMet: Boolean){
        launchConditionsMetGlobal = launchConditionsMet
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

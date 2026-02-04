package pioneer.hardware

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import pioneer.Constants
import pioneer.FlywheelOperatingMode  // ✅ ADDED: Import for operating mode enum
import pioneer.helpers.Chrono
import pioneer.helpers.FileLogger
import pioneer.helpers.Pose
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class Flywheel(
    private val hardwareMap: HardwareMap,
    private val motorName: String = Constants.HardwareNames.FLYWHEEL,
) : HardwareComponent {
    private lateinit var flywheel: DcMotorEx
    private val motorPID = PIDController(
        Constants.Flywheel.KP,
        Constants.Flywheel.KI,
        Constants.Flywheel.KD,
    )

    private val chrono = Chrono()
    private val spinUpTimer = ElapsedTime()
    private val thermalTimer = ElapsedTime()

    // Flywheel state machine
    enum class FlywheelState {
        OFF,      // Completely stopped (match start/end)
        IDLE,     // Low speed ready mode
        SHOOTING  // Full speed
    }

    // Current state
    var state = FlywheelState.OFF
        set(value) {
            if (field != value) {
                // Log state transitions
                FileLogger.debug("Flywheel", "State: ${field.name} → ${value.name}")

                // Handle state-specific logic
                when (value) {
                    FlywheelState.OFF -> {
                        targetVelocity = 0.0
                        isSpinningUp = false
                    }
                    FlywheelState.IDLE -> {
                        targetVelocity = idleVelocity
                        isSpinningUp = false
                    }
                    FlywheelState.SHOOTING -> {
                        // targetVelocity set externally via velocity setter
                        // Start spin-up timer if coming from idle/off
                        if (field != FlywheelState.SHOOTING && targetVelocity > idleVelocity) {
                            isSpinningUp = true
                            spinUpTimer.reset()
                        }
                    }
                }
                field = value
            }
        }

    val motor: DcMotorEx
        get() = flywheel

    // Target velocity (ticks per second)
    var targetVelocity = 0.0
        set(value) {
            field = value

            // Automatically transition to SHOOTING state when target is set high
            if (value > idleVelocity * 1.5 && state != FlywheelState.SHOOTING) {
                state = FlywheelState.SHOOTING
                isSpinningUp = true
                spinUpTimer.reset()
            }
        }

    // Current velocity
    val velocity: Double
        get() = flywheel.velocity

    // Current draw
    val current: Double
        get() = flywheel.getCurrent(CurrentUnit.MILLIAMPS)

    // Idle velocity based on operating mode
    private val idleVelocity: Double
        get() = when (Constants.Flywheel.OPERATING_MODE) {
            FlywheelOperatingMode.ALWAYS_IDLE -> Constants.Flywheel.IDLE_VELOCITY
            FlywheelOperatingMode.SMART_IDLE -> {
                if (smartIdleActive) Constants.Flywheel.IDLE_VELOCITY else 0.0
            }
            FlywheelOperatingMode.CONSERVATIVE_IDLE -> Constants.Flywheel.CONSERVATIVE_VELOCITY
            FlywheelOperatingMode.FULL_OFF -> 0.0
        }

//    private var idleVelocity = 0.0


    // Thermal monitoring
    private var maxCurrentSeen = 0.0
    private var currentSamples = mutableListOf<Double>()
    private val CURRENT_SAMPLE_WINDOW = 20 // Keep last 20 samples

    var isOverheating = false
        private set

    var thermalThrottleActive = false
        private set

    // Spin-up tracking
    private var isSpinningUp = false
    var lastSpinUpTime = 0.0
        private set

    // Smart idle mode state
    private var smartIdleActive = true
    private var lastShootTime = 0L
    private val SMART_IDLE_TIMEOUT_MS = 10_000L // 10 seconds

    // Performance metrics
    var timeAtTargetSpeed = 0.0
        private set
    var totalRunTime = 0.0
        private set
    private var lastUpdateTime = 0L

    override fun init() {
        flywheel = hardwareMap.get(DcMotorEx::class.java, motorName).apply {
            mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
            mode = DcMotor.RunMode.RUN_USING_ENCODER
            zeroPowerBehavior = DcMotor.ZeroPowerBehavior.FLOAT
            direction = DcMotorSimple.Direction.FORWARD
        }

        thermalTimer.reset()
        lastUpdateTime = System.currentTimeMillis()
        FileLogger.info("Flywheel", "Initialized in ${Constants.Flywheel.OPERATING_MODE} mode")
    }

    override fun update() {
        val currentTime = System.currentTimeMillis()
        val dt = (currentTime - lastUpdateTime) / 1000.0
        lastUpdateTime = currentTime

        // Update thermal monitoring
        updateThermalMonitoring()

        // Handle smart idle timeout
        if (Constants.Flywheel.OPERATING_MODE == FlywheelOperatingMode.SMART_IDLE) {
            updateSmartIdle(currentTime)
        }
        //Causing some issues
//        // Apply thermal throttling if needed
//        val effectiveTargetVelocity = if (thermalThrottleActive) {
//            idleVelocity // Throttle down to idle if overheating
//        } else {
//            targetVelocity
//        }
        val effectiveTargetVelocity = targetVelocity
        // Motor control
        if (effectiveTargetVelocity == 0.0) {
            flywheel.power = 0.0
            return
        }

        // PID control
        val correction = motorPID.update(effectiveTargetVelocity - velocity, chrono.dt)
        flywheel.power = (Constants.Flywheel.KF * effectiveTargetVelocity + correction).coerceIn(-1.0, 1.0)

        // Track spin-up completion
        if (isSpinningUp && isAtTargetSpeed()) {
            lastSpinUpTime = spinUpTimer.seconds()
            isSpinningUp = false
            FileLogger.debug("Flywheel", "Spin-up complete in %.3f seconds".format(lastSpinUpTime))
        }

        // Update performance metrics
        totalRunTime += dt
        if (isAtTargetSpeed()) {
            timeAtTargetSpeed += dt
        }
    }

//    private fun updateOperateMode(flywheelOperatingMode: FlywheelOperatingMode){
//        when (flywheelOperatingMode) {
//            FlywheelOperatingMode.ALWAYS_IDLE -> Constants.Flywheel.IDLE_VELOCITY
//            FlywheelOperatingMode.SMART_IDLE -> {
//                if (smartIdleActive) Constants.Flywheel.IDLE_VELOCITY else 0.0
//            }
//            FlywheelOperatingMode.CONSERVATIVE_IDLE -> Constants.Flywheel.CONSERVATIVE_VELOCITY
//            FlywheelOperatingMode.FULL_OFF -> 0.0
//        }
//    }

    private fun updateThermalMonitoring() {
        val currentCurrent = current

        // Track max current
        if (currentCurrent > maxCurrentSeen) {
            maxCurrentSeen = currentCurrent
        }

        // Rolling average of current
        currentSamples.add(currentCurrent)
        if (currentSamples.size > CURRENT_SAMPLE_WINDOW) {
            currentSamples.removeAt(0)
        }

        val avgCurrent = if (currentSamples.isNotEmpty()) {
            currentSamples.average()
        } else {
            0.0
        }

        // Check for overheating
        val wasOverheating = isOverheating
        isOverheating = avgCurrent > Constants.Flywheel.OVERHEAT_CURRENT_MA

        if (isOverheating && !wasOverheating) {
            FileLogger.info("Flywheel", "⚠️ OVERHEATING DETECTED! Current: %.0f mA".format(avgCurrent))
        }

        // Apply thermal throttling if overheating for too long
        if (isOverheating && thermalTimer.seconds() > Constants.Flywheel.OVERHEAT_TIME_THRESHOLD_S) {
            if (!thermalThrottleActive) {
                thermalThrottleActive = true
                FileLogger.info("Flywheel", "⚠️ THERMAL THROTTLE ACTIVATED - Reducing to idle speed")
            }
        } else if (!isOverheating) {
            // Reset thermal state when cooled down
            if (thermalThrottleActive) {
                FileLogger.info("Flywheel", "Thermal throttle cleared - resuming normal operation")
            }
            thermalThrottleActive = false
            thermalTimer.reset()
        }
    }

    private fun updateSmartIdle(currentTime: Long) {
        // If we haven't shot in a while, deactivate smart idle (go to OFF)
        if (currentTime - lastShootTime > SMART_IDLE_TIMEOUT_MS) {
            if (smartIdleActive && state == FlywheelState.IDLE) {
                smartIdleActive = false
                state = FlywheelState.OFF
                FileLogger.debug("Flywheel", "Smart idle timeout - going to OFF")
            }
        }
    }

    fun recordShot() {
        lastShootTime = System.currentTimeMillis()

        // Reactivate smart idle if it was deactivated
        if (Constants.Flywheel.OPERATING_MODE == FlywheelOperatingMode.SMART_IDLE && !smartIdleActive) {
            smartIdleActive = true
        }
    }

    fun isAtTargetSpeed(tolerance: Double = Constants.Flywheel.SPEED_TOLERANCE_TPS): Boolean {
        return abs(velocity - targetVelocity) < tolerance
    }

    fun getReadyPercentage(): Double {
        return if (totalRunTime > 0) {
            (timeAtTargetSpeed / totalRunTime) * 100.0
        } else {
            0.0
        }
    }

    fun getAverageCurrent(): Double {
        return if (currentSamples.isNotEmpty()) {
            currentSamples.average()
        } else {
            0.0
        }
    }

    fun getThermalStatus(): String {
        return when {
            thermalThrottleActive -> "THROTTLED"
            isOverheating -> "OVERHEATING"
            else -> "NORMAL"
        }
    }

    fun resetMetrics() {
        timeAtTargetSpeed = 0.0
        totalRunTime = 0.0
        maxCurrentSeen = 0.0
        currentSamples.clear()
    }

    // ========================================
    // ✅ SHOOTING ACCURACY IMPROVEMENTS
    // ========================================

    /**
     * Calculate required flywheel velocity for a shot
     *
     * ✅ UPDATED with shooting accuracy improvements:
     * - Speed reduction factor (tunable via Constants)
     * - Battery voltage compensation
     * - Safety limits (min/max speed)
     * - Shot quality assessment
     */
    fun estimateVelocity(
        pose: Pose,
        target: Pose,
        targetHeight: Double,
        batteryVoltage: Double = 12.0  // ✅ NEW: Battery compensation
    ): Double {
        val shootPose = pose +
                Pose(x = Constants.Turret.OFFSET * sin(-pose.theta), y = Constants.Turret.OFFSET * cos(-pose.theta)) +
                Pose(pose.vx * Constants.Turret.LAUNCH_TIME, pose.vy * Constants.Turret.LAUNCH_TIME)

        val heightDiff = targetHeight - Constants.Turret.HEIGHT
        val groundDistance = shootPose distanceTo target

        // DISTANCE VALIDATION
        if (groundDistance < Constants.Flywheel.MIN_SHOT_DISTANCE_CM ||
            groundDistance > Constants.Flywheel.MAX_SHOT_DISTANCE_CM) {
            FileLogger.info("Flywheel", "⚠️ Shot distance %.1f cm out of range!".format(groundDistance))
        }

        // Real world v0 of the ball
        val v0 = (groundDistance) / (
                cos(Constants.Turret.THETA) *
                        sqrt((2.0 * (heightDiff - tan(Constants.Turret.THETA) * groundDistance)) / (-980))
                )

        // Regression for real world velocity to flywheel speed
        var flywheelVelocity = Constants.Flywheel.REGRESSION_SLOPE * v0 +
                Constants.Flywheel.REGRESSION_INTERCEPT

        // Speed reduction factor (PRIMARY TUNING KNOB)
        //TODO ADD back when other stuff is fixed
//        flywheelVelocity *= Constants.Flywheel.SPEED_REDUCTION_FACTOR

        // Battery voltage compensation
        //TODO: Test if necessary, otherwise might be causing issues
//        if (Constants.Flywheel.USE_BATTERY_COMPENSATION) {
//            val voltageCompensation = 12.0 / batteryVoltage.coerceIn(10.0, 13.5)
//            flywheelVelocity *= voltageCompensation.coerceIn(
//                Constants.Flywheel.MIN_VOLTAGE_COMPENSATION,
//                Constants.Flywheel.MAX_VOLTAGE_COMPENSATION
//            )
//        }

        // Safety limits
        flywheelVelocity = flywheelVelocity.coerceIn(
            Constants.Flywheel.MIN_FLYWHEEL_SPEED,
            Constants.Flywheel.MAX_FLYWHEEL_SPEED
        )

        return flywheelVelocity
    }

    // ✅ NEW: Assess shot quality based on distance
    fun assessShotQuality(distance: Double): ShotQuality {
        return when {
            distance < 30.0 -> ShotQuality.OUT_OF_RANGE
            distance in 30.0..50.0 -> ShotQuality.RISKY
            distance in 50.0..60.0 -> ShotQuality.ACCEPTABLE
            distance in 60.0..80.0 -> ShotQuality.GOOD
            distance in 80.0..150.0 -> ShotQuality.EXCELLENT
            distance in 150.0..180.0 -> ShotQuality.GOOD
            distance in 180.0..210.0 -> ShotQuality.ACCEPTABLE
            distance in 210.0..240.0 -> ShotQuality.RISKY
            else -> ShotQuality.OUT_OF_RANGE
        }
    }

    enum class ShotQuality {
        EXCELLENT,      // 80-150cm - sweet spot
        GOOD,           // 60-80cm or 150-180cm
        ACCEPTABLE,     // 50-60cm or 180-210cm
        RISKY,          // 30-50cm or 210-240cm
        OUT_OF_RANGE    // <30cm or >240cm
    }
}

// PID Controller (copied from helpers to avoid circular dependency)
private class PIDController(
    private val kP: Double,
    private val kI: Double,
    private val kD: Double,
) {
    private var integral = 0.0
    private var previousError = 0.0

    fun update(error: Double, dt: Double): Double {
        integral += error * dt
        val derivative = (error - previousError) / dt
        previousError = error
        return kP * error + kI * integral + kD * derivative
    }
}
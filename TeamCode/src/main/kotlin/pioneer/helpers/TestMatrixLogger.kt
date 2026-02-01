package pioneer.helpers

import com.qualcomm.robotcore.util.ElapsedTime
import pioneer.Bot
import pioneer.Constants
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Automated Test Matrix Logger
 * 
 * Captures performance data during OpMode execution and exports to CSV
 * for automatic population of FTC Requirements Test Matrix spreadsheet.
 * 
 * Usage in OpMode:
 * ```kotlin
 * private lateinit var testLogger: TestMatrixLogger
 * 
 * override fun onInit() {
 *     testLogger = TestMatrixLogger(bot, "TEST_AUTO_001", "Autonomous Shooting Test")
 * }
 * 
 * override fun onStart() {
 *     testLogger.start()
 * }
 * 
 * override fun onLoop() {
 *     testLogger.logMetric("shot_distance", currentDistance)
 *     testLogger.logEvent("shot_fired", "target_id" to "GOAL_BLUE")
 * }
 * 
 * override fun onStop() {
 *     testLogger.stop()
 *     super.onStop()
 * }
 * ```
 */
class TestMatrixLogger(
    private val bot: Bot,
    private val testId: String,
    private val testDescription: String,
    private val testType: TestType = TestType.FUNCTIONAL
) {
    enum class TestType {
        INSPECTION,
        FUNCTIONAL,
        CHARACTERIZATION,
        PERFORMANCE,
        RELIABILITY
    }
    
    enum class TestResult {
        PASS,
        FAIL,
        MARGINAL,
        NOT_TESTED
    }
    
    data class TestMetric(
        val timestamp: Double,
        val metricName: String,
        val metricValue: Double,
        val unit: String = "",
        val system: String = "GENERAL"
    )
    
    data class TestEvent(
        val timestamp: Double,
        val eventType: String,
        val parameters: Map<String, String> = emptyMap()
    )
    
    // Test session data
    private val sessionStartTime = System.currentTimeMillis()
    private val elapsedTimer = ElapsedTime()
    private val metrics = mutableListOf<TestMetric>()
    private val events = mutableListOf<TestEvent>()
    
    // Test conditions
    private var initialBatteryVoltage = 0.0
    private var finalBatteryVoltage = 0.0
    private var initialPose = Pose()
    private var testResult = TestResult.NOT_TESTED
    private var testNotes = ""
    
    // Automatic metric tracking
    private var autoTrackingEnabled = true
    private var lastLogTime = 0L
    private val LOG_INTERVAL_MS = 100L  // Log every 100ms
    
    // File output
    private lateinit var csvFile: File
    private lateinit var csvWriter: FileWriter
    
    /**
     * Start test logging session
     */
    fun start() {
        elapsedTimer.reset()
        
        // Capture initial conditions
        initialBatteryVoltage = bot.batteryMonitor?.voltage ?: 0.0
        initialPose = bot.pinpoint?.pose ?: Pose()
        
        // Create CSV file
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
            .format(Date(sessionStartTime))
        val filename = "test_${testId}_${timestamp}.csv"
        csvFile = File("/sdcard/FIRST/test_logs/$filename")
        csvFile.parentFile?.mkdirs()
        
        csvWriter = FileWriter(csvFile, false)
        
        // Write CSV header
        csvWriter.append("timestamp,elapsed_s,metric_name,metric_value,unit,system,event_type,event_params\n")
        csvWriter.flush()
        
        FileLogger.info("TestMatrixLogger", "Started test: $testId - $testDescription")
        FileLogger.info("TestMatrixLogger", "Initial battery: %.2fV".format(initialBatteryVoltage))
        FileLogger.info("TestMatrixLogger", "Logging to: ${csvFile.absolutePath}")
        
        // Log test start event
        logEvent("TEST_START", mapOf(
            "test_id" to testId,
            "test_type" to testType.name,
            "description" to testDescription,
            "battery_v" to "%.2f".format(initialBatteryVoltage)
        ))
    }
    
    /**
     * Log a single metric value
     */
    fun logMetric(
        name: String,
        value: Double,
        unit: String = "",
        system: String = "GENERAL"
    ) {
        val metric = TestMetric(
            timestamp = elapsedTimer.seconds(),
            metricName = name,
            metricValue = value,
            unit = unit,
            system = system
        )
        
        metrics.add(metric)
        
        // Write to CSV immediately for crash recovery
        csvWriter.append("${metric.timestamp},${metric.timestamp},$name,$value,$unit,$system,,\n")
    }
    
    /**
     * Log an event with parameters
     */
    fun logEvent(
        eventType: String,
        parameters: Map<String, String> = emptyMap()
    ) {
        val event = TestEvent(
            timestamp = elapsedTimer.seconds(),
            eventType = eventType,
            parameters = parameters
        )
        
        events.add(event)
        
        // Format parameters as JSON-ish string
        val paramStr = parameters.entries.joinToString(";") { "${it.key}=${it.value}" }
        
        csvWriter.append("${event.timestamp},${event.timestamp},,,,,$eventType,\"$paramStr\"\n")
    }
    
    /**
     * Automatic telemetry logging (call in onLoop)
     */
    fun logAutomaticTelemetry() {
        if (!autoTrackingEnabled) return
        
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastLogTime < LOG_INTERVAL_MS) return
        lastLogTime = currentTime
        
        val elapsed = elapsedTimer.seconds()
        
        // Motion Control System (MCS)
        bot.pinpoint?.pose?.let { pose ->
            logMetric("mcs_pos_x", pose.x, "cm", "MCS")
            logMetric("mcs_pos_y", pose.y, "cm", "MCS")
            logMetric("mcs_heading", pose.theta, "rad", "MCS")
            logMetric("mcs_vel_x", pose.vx, "cm/s", "MCS")
            logMetric("mcs_vel_y", pose.vy, "cm/s", "MCS")
            logMetric("mcs_omega", pose.omega, "rad/s", "MCS")
        }
        
        // Projectile Launch Control System (PLCS)
        bot.flywheel?.let { flywheel ->
            logMetric("plcs_flywheel_velocity", flywheel.velocity, "tps", "PLCS")
            logMetric("plcs_flywheel_target", flywheel.targetVelocity, "tps", "PLCS")
            logMetric("plcs_flywheel_current", flywheel.current, "mA", "PLCS")
            logMetric("plcs_flywheel_power", flywheel.motor.power, "ratio", "PLCS")
        }
        
        bot.turret?.let { turret ->
            logMetric("plcs_turret_angle", turret.currentAngle, "rad", "PLCS")
            logMetric("plcs_turret_target", turret.targetAngle, "rad", "PLCS")
            logMetric("plcs_turret_error", turret.currentAngle - turret.targetAngle, "rad", "PLCS")
        }
        
        // Artifact Management System (AMS)
        bot.spindexer?.let { spindexer ->
            logMetric("ams_position", spindexer.currentMotorTicks.toDouble(), "ticks", "AMS")
            logMetric("ams_target", spindexer.targetMotorTicks.toDouble(), "ticks", "AMS")
            logMetric("ams_velocity", spindexer.currentMotorVelocity, "tps", "AMS")
            logMetric("ams_count", spindexer.numStoredArtifacts.toDouble(), "artifacts", "AMS")
        }
        
        // Power System
        bot.batteryMonitor?.voltage?.let { voltage ->
            logMetric("power_battery_voltage", voltage, "V", "POWER")
        }
    }
    
    /**
     * Log shot attempt and outcome
     */
    fun logShot(
        shotNumber: Int,
        distance: Double,
        targetX: Double,
        targetY: Double,
        hit: Boolean
    ) {
        logEvent("SHOT_FIRED", mapOf(
            "shot_number" to shotNumber.toString(),
            "distance" to "%.1f".format(distance),
            "target_x" to "%.1f".format(targetX),
            "target_y" to "%.1f".format(targetY),
            "result" to if (hit) "HIT" else "MISS"
        ))
    }
    
    /**
     * Log collection attempt and outcome
     */
    fun logCollection(
        artifactType: String,
        position: String,
        success: Boolean,
        timeMs: Long
    ) {
        logEvent("COLLECTION_ATTEMPT", mapOf(
            "artifact" to artifactType,
            "position" to position,
            "result" to if (success) "SUCCESS" else "FAIL",
            "time_ms" to timeMs.toString()
        ))
    }
    
    /**
     * Set final test result and notes
     */
    fun setResult(result: TestResult, notes: String = "") {
        testResult = result
        testNotes = notes
        
        FileLogger.info("TestMatrixLogger", "Test result: $result")
        if (notes.isNotEmpty()) {
            FileLogger.info("TestMatrixLogger", "Notes: $notes")
        }
    }
    
    /**
     * Stop logging and finalize file
     */
    fun stop() {
        // Capture final conditions
        finalBatteryVoltage = bot.batteryMonitor?.voltage ?: 0.0
        
        // Log test end event
        logEvent("TEST_END", mapOf(
            "test_id" to testId,
            "result" to testResult.name,
            "duration_s" to "%.2f".format(elapsedTimer.seconds()),
            "battery_start_v" to "%.2f".format(initialBatteryVoltage),
            "battery_end_v" to "%.2f".format(finalBatteryVoltage),
            "battery_drop_v" to "%.2f".format(initialBatteryVoltage - finalBatteryVoltage),
            "notes" to testNotes
        ))
        
        // Write summary
        csvWriter.append("\n# TEST SUMMARY\n")
        csvWriter.append("# Test ID: $testId\n")
        csvWriter.append("# Description: $testDescription\n")
        csvWriter.append("# Type: ${testType.name}\n")
        csvWriter.append("# Result: ${testResult.name}\n")
        csvWriter.append("# Duration: %.2f seconds\n".format(elapsedTimer.seconds()))
        csvWriter.append("# Battery: %.2fV → %.2fV (drop: %.2fV)\n".format(
            initialBatteryVoltage, finalBatteryVoltage, initialBatteryVoltage - finalBatteryVoltage
        ))
        csvWriter.append("# Metrics Logged: ${metrics.size}\n")
        csvWriter.append("# Events Logged: ${events.size}\n")
        if (testNotes.isNotEmpty()) {
            csvWriter.append("# Notes: $testNotes\n")
        }
        
        // Close file
        csvWriter.flush()
        csvWriter.close()
        
        FileLogger.info("TestMatrixLogger", "Test complete - logged ${metrics.size} metrics, ${events.size} events")
        FileLogger.info("TestMatrixLogger", "Data saved to: ${csvFile.absolutePath}")
        
        // Log retrieval instructions
        FileLogger.info("TestMatrixLogger", "To retrieve: adb pull ${csvFile.absolutePath}")
    }
    
    /**
     * Calculate statistics for a specific metric
     */
    fun getStatistics(metricName: String): MetricStatistics? {
        val values = metrics
            .filter { it.metricName == metricName }
            .map { it.metricValue }
        
        if (values.isEmpty()) return null
        
        val mean = values.average()
        val stdDev = if (values.size > 1) {
            kotlin.math.sqrt(
                values.map { (it - mean) * (it - mean) }.average()
            )
        } else {
            0.0
        }
        
        return MetricStatistics(
            metricName = metricName,
            count = values.size,
            mean = mean,
            stdDev = stdDev,
            min = values.minOrNull() ?: 0.0,
            max = values.maxOrNull() ?: 0.0
        )
    }
    
    data class MetricStatistics(
        val metricName: String,
        val count: Int,
        val mean: Double,
        val stdDev: Double,
        val min: Double,
        val max: Double
    ) {
        override fun toString(): String {
            return "$metricName: μ=%.2f σ=%.2f [%.2f, %.2f] (n=$count)".format(
                mean, stdDev, min, max
            )
        }
    }
}

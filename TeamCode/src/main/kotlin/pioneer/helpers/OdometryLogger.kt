package pioneer.helpers

import pioneer.Bot
import pioneer.pathing.follower.Follower
import java.text.SimpleDateFormat
import java.util.*

/**
 * Dedicated logger for odometry drift analysis.
 * Logs comprehensive pose, velocity, encoder, and control data to CSV files.
 * 
 * Usage in OpMode:
 *   val logger = OdometryLogger(bot, "GoalSideAuto")
 *   logger.start()
 *   // In loop: logger.log(currentState, targetPose)
 *   logger.stop()
 */
class OdometryLogger(
    private val bot: Bot,
    private val opModeName: String
) {
    private val entries = mutableListOf<String>()
    private val startTime = System.currentTimeMillis()
    private var lastLogTime = startTime
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
    
    // Configuration
    private val logDir = "/sdcard/FIRST/drift_logs"
    private val minLogInterval = 20 // ms between logs (50 Hz max)
    
    // State tracking
    private var isLogging = false
    private var totalLogs = 0
    
    // CSV header
    private val header = listOf(
        // Time
        "timestamp_ms", "elapsed_s",
        
        // Estimated pose
        "est_x", "est_y", "est_theta",
        
        // Velocities
        "vx", "vy", "omega",
        
        // Accelerations
        "ax", "ay", "alpha",
        
        // Raw encoder data
        "enc_x_ticks", "enc_y_ticks",
        
        // Target pose (if following path)
        "target_x", "target_y", "target_theta",
        
        // Follower state (if available)
        "follower_done", "path_length", "target_velocity",
        
        // Position errors (field frame)
        "error_x", "error_y", "error_theta",
        
        // State machine info
        "state", "sub_state",
        
        // Battery voltage
        "voltage"
    ).joinToString(",")
    
    init {
        entries.add(header)
    }
    
    /**
     * Start logging - call this in onStart()
     */
    fun start() {
        isLogging = true
        lastLogTime = System.currentTimeMillis()
        FileLogger.info("OdometryLogger", "Started logging for $opModeName")
    }
    
    /**
     * Stop logging and save file - call this in onStop()
     */
    fun stop() {
        isLogging = false
        save()
        FileLogger.info("OdometryLogger", "Stopped logging. Total entries: $totalLogs")
    }
    
    /**
     * Log current state - call this every loop iteration
     * 
     * @param state Current state machine state (e.g., "GOTO_SHOOT")
     * @param subState Optional sub-state (e.g., "MOVING_TO_POSITION")
     * @param targetPose Target pose the robot is trying to reach (null if not following path)
     */
    fun log(
        state: String = "UNKNOWN",
        subState: String = "",
        targetPose: Pose? = null
    ) {
        if (!isLogging) return
        
        // Rate limiting
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastLogTime < minLogInterval) return
        lastLogTime = currentTime
        
        val pose = bot.pinpoint?.pose ?: return
        val elapsed = (currentTime - startTime) / 1000.0
        
        // Get follower state if available
        val follower = try { bot.follower } catch (e: Exception) { null }
        val followerDone = follower?.done ?: true
        val pathLength = follower?.currentPath?.getLength() ?: 0.0
        val targetVelocity = follower?.targetState?.v ?: 0.0
        
        // Get target pose from follower if not provided
        val target = targetPose ?: run {
            val pathT = follower?.targetState?.x?.let { s ->
                follower.currentPath?.getTFromLength(s)
            }
            pathT?.let { follower.currentPath?.getPose(it) }
        }
        
        // Calculate errors (field frame)
        val errorX = (target?.x ?: pose.x) - pose.x
        val errorY = (target?.y ?: pose.y) - pose.y
        val errorTheta = MathUtils.normalizeRadians((target?.theta ?: pose.theta) - pose.theta)
        
        // Get encoder data
        val encX = bot.pinpoint?.encoderXTicks ?: 0
        val encY = bot.pinpoint?.encoderYTicks ?: 0
        
        // Get voltage
        val voltage = bot.batteryMonitor?.voltage ?: 0.0
        
        // Build CSV row
        val row = listOf(
            currentTime,
            "%.3f".format(elapsed),
            
            "%.3f".format(pose.x),
            "%.3f".format(pose.y),
            "%.6f".format(pose.theta),
            
            "%.3f".format(pose.vx),
            "%.3f".format(pose.vy),
            "%.6f".format(pose.omega),
            
            "%.3f".format(pose.ax),
            "%.3f".format(pose.ay),
            "%.6f".format(pose.alpha),
            
            encX,
            encY,
            
            "%.3f".format(target?.x ?: 0.0),
            "%.3f".format(target?.y ?: 0.0),
            "%.6f".format(target?.theta ?: 0.0),
            
            followerDone,
            "%.3f".format(pathLength),
            "%.3f".format(targetVelocity),
            
            "%.3f".format(errorX),
            "%.3f".format(errorY),
            "%.6f".format(errorTheta),
            
            state,
            subState,
            
            "%.2f".format(voltage)
        ).joinToString(",")
        
        entries.add(row)
        totalLogs++
    }
    
    /**
     * Log a critical checkpoint (e.g., start of path, end of path, state transition)
     * These are always logged regardless of rate limiting
     */
    fun logCheckpoint(
        checkpoint: String,
        targetPose: Pose? = null
    ) {
        val savedMinInterval = minLogInterval
        // Temporarily disable rate limiting
        lastLogTime = 0
        log(state = checkpoint, targetPose = targetPose)
        FileLogger.info("OdometryLogger", "Checkpoint: $checkpoint")
    }
    
    /**
     * Save the log file to disk
     */
    private fun save() {
        try {
            val timestamp = dateFormatter.format(Date(startTime))
            val filename = "${opModeName}_${timestamp}.csv"
            val file = java.io.File(logDir, filename)
            
            // Create directory if it doesn't exist
            file.parentFile?.mkdirs()
            
            // Write all entries
            file.writeText(entries.joinToString("\n"))
            
            FileLogger.info("OdometryLogger", "Saved ${entries.size} entries to $filename")
        } catch (e: Exception) {
            FileLogger.error("OdometryLogger", "Failed to save log: ${e.message}")
        }
    }
    
    /**
     * Get current statistics about the logging session
     */
    fun getStats(): String {
        val elapsed = (System.currentTimeMillis() - startTime) / 1000.0
        val rate = if (elapsed > 0) totalLogs / elapsed else 0.0
        return "Logs: $totalLogs, Rate: ${"%.1f".format(rate)} Hz"
    }
}

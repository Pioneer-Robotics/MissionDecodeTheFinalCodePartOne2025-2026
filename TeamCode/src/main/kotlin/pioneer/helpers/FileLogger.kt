package pioneer.helpers

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A file-based logger with automatic file rotation and buffering.
 * Logs are written to /sdcard/logs/ with automatic rotation when files exceed size limits.
 */
object FileLogger {
    // Configuration constants
    private const val BASE_FILE_NAME = "/sdcard/logs/log"
    private const val MAX_FILE_SIZE = 1024 * 1024 * 5 // 5 MB
    private const val NUM_FILES = 3
    private const val MAX_QUEUE_SIZE = 1024 * 1024 // 1 MB
    private const val DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss:SSS"

    private val dateFormatter = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.US)
    private val logQueue = mutableListOf<String>()
    private var queueSize = 0
    private var currentFile: File = initializeFile()

    enum class LogLevel(
        val displayName: String,
    ) {
        DEBUG("DEBUG"),
        INFO("INFO"),
        WARN("WARN"),
        ERROR("ERROR"),
    }

    private fun initializeFile(): File =
        try {
            val file = File("$BASE_FILE_NAME.txt")
            file.parentFile?.mkdirs()
            if (!file.exists()) {
                file.createNewFile()
            }
            file
        } catch (e: Exception) {
            // Fallback to a temp file if primary location fails
            File.createTempFile("ftc_log", ".txt")
        }

    private fun rotateFiles() {
        try {
            // Rotate existing files (log.2.txt -> log.3.txt, log.1.txt -> log.2.txt, etc.)
            for (i in (NUM_FILES - 2) downTo 0) {
                val sourceFile =
                    if (i == 0) {
                        File("$BASE_FILE_NAME.txt")
                    } else {
                        File("$BASE_FILE_NAME.$i.txt")
                    }
                val destFile = File("$BASE_FILE_NAME.${i + 1}.txt")

                if (sourceFile.exists()) {
                    destFile.delete() // Remove destination if it exists
                    sourceFile.renameTo(destFile)
                }
            }
            currentFile = initializeFile()
        } catch (e: Exception) {
            // If rotation fails, just continue with current file
            currentFile = initializeFile()
        }
    }

    /**
     * Logs a message with the specified level and tag.
     * Messages are buffered and flushed when queue size exceeds limit.
     */
    fun log(
        level: LogLevel,
        tag: String,
        message: String,
    ) {
        try {
            val timestamp = dateFormatter.format(Date())
            val logMessage = "[$timestamp] [$tag] [${level.displayName}] $message"

            synchronized(logQueue) {
                logQueue.add(logMessage)
                queueSize += logMessage.length + 1 // +1 for newline
            }

            // Auto-flush if queue is large or file would exceed size limit
            if (shouldFlush()) {
                flush()
            }
        } catch (e: Exception) {
            // Silently ignore logging errors to prevent crashes
        }
    }

    private fun shouldFlush(): Boolean =
        queueSize > MAX_QUEUE_SIZE ||
            (currentFile.exists() && currentFile.length() + queueSize > MAX_FILE_SIZE)

    // Convenience methods for different log levels
    fun debug(
        tag: String,
        message: String,
    ) = log(LogLevel.DEBUG, tag, message)

    fun info(
        tag: String,
        message: String,
    ) = log(LogLevel.INFO, tag, message)

    fun warn(
        tag: String,
        message: String,
    ) = log(LogLevel.WARN, tag, message)

    fun error(
        tag: String,
        message: String,
    ) = log(LogLevel.ERROR, tag, message)

    /**
     * Flushes the current log queue to file and rotates if necessary.
     */
    fun flush() {
        try {
            synchronized(logQueue) {
                if (logQueue.isEmpty()) return

                val logContent = logQueue.joinToString("\n") + "\n"
                currentFile.appendText(logContent)

                logQueue.clear()
                queueSize = 0
            }

            // Check if file rotation is needed
            if (currentFile.length() > MAX_FILE_SIZE) {
                rotateFiles()
            }
        } catch (e: Exception) {
            // Reset queue on flush failure to prevent memory issues
            synchronized(logQueue) {
                logQueue.clear()
                queueSize = 0
            }
        }
    }

    /**
     * Clears all logs and starts fresh.
     */
    fun clearLogs() {
        try {
            synchronized(logQueue) {
                logQueue.clear()
                queueSize = 0
            }

            // Delete all log files
            repeat(NUM_FILES) { i ->
                val fileName = if (i == 0) "$BASE_FILE_NAME.txt" else "$BASE_FILE_NAME.$i.txt"
                File(fileName).delete()
            }

            currentFile = initializeFile()
        } catch (e: Exception) {
            // If clearing fails, at least reset the queue
            synchronized(logQueue) {
                logQueue.clear()
                queueSize = 0
            }
        }
    }
}

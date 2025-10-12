package pioneer.helpers

import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileLogger {
    companion object {
        // Constants
        private const val BASE_FILE_NAME = "/sdcard/logs/log"
        private const val MAX_FILE_SIZE = 1024 * 1024 * 5 // 5 MB
        private const val NUM_FILES = 3
        private const val MAX_QUEUE_SIZE = 1024 * 1024 // 1 MB
        private const val DATE_FORMAT_PATTERN = "yyyy-MM-dd HH:mm:ss:SSS"

        private val logQueue: MutableList<String> = mutableListOf<String>()

        private var currentFile: File
        private var queueSize = 0

        enum class LogLevel {
            DEBUG, INFO, WARN, ERROR
        }

        init {
            currentFile = initializeFile()
        }

        private fun initializeFile(): File {
            val file = File("$BASE_FILE_NAME.txt")
            file.parentFile?.mkdirs()
            if (!file.exists()) file.createNewFile()
            return file
        }

        private fun rotateFiles() {
            for (i in (NUM_FILES - 2) downTo 0) {
                val source = File(if (i == 0) "$BASE_FILE_NAME.txt" else "$BASE_FILE_NAME.$i.txt")
                val destination = File("$BASE_FILE_NAME.${i + 1}.txt")
                if (source.exists()) {
                    if (destination.exists()) destination.delete()
                    source.renameTo(destination)
                }
            }
            currentFile = initializeFile()
        }

        /**
         * Logs a message with the specified log level and tag.
         * The message is formatted with a timestamp and added to a queue.
         * If the queue exceeds a certain size, it flushes to the file.
         */
        fun log(level: LogLevel, tag: String, message: String) {
            val timestamp = SimpleDateFormat(DATE_FORMAT_PATTERN, Locale.US).format(Date())
            val logMessage = "[$timestamp] [$tag] [${level.name}] $message"

            // Add to queue
            logQueue.add(logMessage)
            queueSize += logMessage.length + 1 // +1 for newline character

            // Flush to file if queue exceeds a certain size or if the current file size exceeds the limit
            if (queueSize > MAX_QUEUE_SIZE || currentFile.length() + queueSize > MAX_FILE_SIZE) {
                flush()
            }
        }

        fun debug(tag: String, message: String) = log(LogLevel.DEBUG, tag, message)
        fun info(tag: String, message: String) = log(LogLevel.INFO, tag, message)
        fun warn(tag: String, message: String) = log(LogLevel.WARN, tag, message)
        fun error(tag: String, message: String) = log(LogLevel.ERROR, tag, message)

        fun flush() {
            if (logQueue.isEmpty()) return

            // Write to file
            currentFile.appendText(logQueue.joinToString("\n") + "\n")
            logQueue.clear()
            queueSize = 0

            // Check file size and rotate if necessary
            if (currentFile.length() > MAX_FILE_SIZE) {
                rotateFiles()
            }
        }

        fun clearLogs() {
            logQueue.clear()
            queueSize = 0

            // Delete all log files
            for (i in 0 until NUM_FILES) {
                val file = if (i == 0) File("$BASE_FILE_NAME.txt") else File("$BASE_FILE_NAME.$i.txt")
                if (file.exists()) file.delete()
            }

            // Initialize a new log file
            currentFile = initializeFile()
        }
    }
}

package pioneer.hardware.spindexer

import com.qualcomm.robotcore.util.ElapsedTime
import pioneer.Constants
import pioneer.decode.Artifact
import pioneer.decode.Motif
import pioneer.helpers.FileLogger

class ArtifactIndexer {
    private val artifacts: Array<Artifact?> = Array(3) { null }

    // Intake confirmation state
    private val artifactVisibleTimer = ElapsedTime()
    private val artifactLostTimer = ElapsedTime()
    private var artifactSeen = false
    private var artifactWasSeenRecently = false

    private var lastStoredIndex = 0

    // --- Public State --- //
    fun snapshot(): Array<Artifact?> = artifacts.copyOf()

    val isFull: Boolean
        get() = artifacts.none { it == null }

    val isEmpty: Boolean
        get() = artifacts.all { it == null }

    val count: Int
        get() = artifacts.count { it != null }

    // --- Index Helpers --- //
    fun nextOpenIntakeIndex(currentIndex: Int): Int? = run {
        for (i in 0..2) {
            // -i for reversed direction
            val index = Math.floorMod(currentIndex - i, 3)
            if (artifacts[index] == null) return index
        }
        null
    }

    fun findOuttakeIndex(target: Artifact? = null): Int? =
        target?.let {
            artifacts.indexOfFirst { a -> a == it }.takeIf { i -> i != -1 }
        } ?: artifacts.indexOfFirst { it != null }.takeIf { it != -1 }

    fun motifStartIndex(motif: Motif?, currentIndex: Int): Int? {
        // Find the best shift to match motif
        if (motif == null || !motif.isValid()) return null

        val pattern = motif.getPattern()

        var bestShift: Int? = null
        var bestScore = -1

        // Try all 3 rotations
        for (offset in 0 until 3) {
            // -offset for reversed direction, -2 to account for shooting vs intake difference
            val shift = Math.floorMod((currentIndex - offset - 2), 3)
            var score = 0

            // Score this rotation based on how many positions match the motif pattern
            for (i in 0 until 3) {
                val rotated = artifacts[(i + shift) % 3]
                if (rotated == pattern[i]) {
                    score++
                }
            }

            if (score > bestScore) {
                bestScore = score
                bestShift = shift
            }
        }

        return bestShift?.minus(2)
    }

    // --- Intake Logic --- //

    /**
     * Call continuously while at an intake position.
     * Returns true when an artifact is CONFIRMED and stored.
     */
    fun processIntake(
        index: Int,
        detected: Artifact?,
    ): Boolean {
        if (detected != null) {
            if (!artifactSeen) {
                artifactVisibleTimer.reset()
                artifactSeen = true
            }

            artifactLostTimer.reset()
            artifactWasSeenRecently = true

            if (artifactVisibleTimer.milliseconds() >= Constants.Spindexer.CONFIRM_INTAKE_MS) {
                store(index, detected)
                resetIntakeState()
                return true
            }
        } else {
            handleLoss()
        }

        return false
    }

    private fun handleLoss() {
        if (!artifactWasSeenRecently) return

        // FIXME: whatttt
        if (artifactLostTimer.milliseconds() == 0.0) {
            artifactLostTimer.reset()
        }

        if (artifactLostTimer.milliseconds() > Constants.Spindexer.CONFIRM_LOSS_MS) {
            resetIntakeState()
        }
    }

    private fun resetIntakeState() {
        artifactSeen = false
        artifactWasSeenRecently = false
    }

    // --- Storage Logic --- //
    fun store(index: Int, artifact: Artifact) {
        artifacts[index] = artifact
        lastStoredIndex = index
        FileLogger.info("ArtifactIndexer", "Stored $artifact at $index")
    }

    fun pop(index: Int): Artifact? {
        val artifact = artifacts[index]
        artifacts[index] = null
        FileLogger.info("ArtifactIndexer", "Removed $artifact from $index")
        return artifact
    }


    fun popLastIntake() {
        artifacts[lastStoredIndex] = null
    }

    fun resetAll() {
        for (i in artifacts.indices) artifacts[i] = null
        resetIntakeState()
    }

    fun setAll(vararg values: Artifact?) {
        require(values.size == 3)
        values.forEachIndexed { i, a -> artifacts[i] = a }
    }
}
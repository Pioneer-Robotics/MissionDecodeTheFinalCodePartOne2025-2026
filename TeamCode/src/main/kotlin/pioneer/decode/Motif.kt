package pioneer.decode

class Motif(
    val aprilTagId: Int,
) {
    private val artifacts: List<Artifact> =
        when (aprilTagId) {
            21 -> listOf(Artifact.GREEN, Artifact.PURPLE, Artifact.PURPLE)
            22 -> listOf(Artifact.PURPLE, Artifact.GREEN, Artifact.PURPLE)
            23 -> listOf(Artifact.PURPLE, Artifact.PURPLE, Artifact.GREEN)
            else -> emptyList()
        }

    private var currentIndex: Int = 0

    // Returns the next artifact in the motif's sequence, cycling back to the start if needed
    fun getNextArtifact(): Artifact? {
        if (artifacts.isEmpty()) return null

        val artifact = artifacts[currentIndex]
        currentIndex = (currentIndex + 1) % artifacts.size
        return artifact
    }

    // Returns the current artifact without advancing the index
    val currentArtifact: Artifact?
        get() {
            if (artifacts.isEmpty()) return null
            return artifacts[currentIndex]
        }

    // Returns the artifact at a specific position in the pattern (0-indexed)
    fun getArtifactAt(index: Int): Artifact? {
        if (index < 0 || index >= artifacts.size) return null
        return artifacts[index]
    }

    // Returns the current position in the pattern (0-indexed)
    fun getCurrentIndex(): Int = currentIndex

    // Returns true if this is a valid motif with artifacts
    fun isValid(): Boolean = artifacts.isNotEmpty()

    // Returns the complete artifact sequence for this motif
    fun getPattern(): List<Artifact> = artifacts

    fun reset() {
        currentIndex = 0
    }

    override fun toString(): String = getPattern().toString()
}

package pioneer.decode

class Motif(
    val aprilTagId: Int,
) {
    private val artifacts: List<Artifact>?
    private var currentIndex: Int = 0

    init {
        artifacts =
            when (aprilTagId) {
                21 -> listOf(GreenArtifact(), PurpleArtifact(), PurpleArtifact())
                22 -> listOf(PurpleArtifact(), GreenArtifact(), PurpleArtifact())
                23 -> listOf(PurpleArtifact(), PurpleArtifact(), GreenArtifact())
                else -> null
            }
    }

    // Returns the next artifact in the motif's sequence, cycling back to the start if needed
    fun getNextArtifact(): Artifact? {
        if (artifacts == null || artifacts.isEmpty()) return null

        val artifact = artifacts[currentIndex]
        currentIndex = (currentIndex + 1) % artifacts.size
        return artifact
    }

    // Returns the current artifact without advancing the index
    fun currentArtifact(): Artifact? {
        if (artifacts == null || artifacts.isEmpty()) return null
        return artifacts[currentIndex]
    }

    // Returns the artifact at a specific position in the pattern (0-indexed)
    fun getArtifactAt(index: Int): Artifact? {
        if (artifacts == null || artifacts.isEmpty() || index < 0 || index >= artifacts.size) return null
        return artifacts[index]
    }

    // Returns the current position in the pattern (0-indexed)
    fun getCurrentIndex(): Int = currentIndex

    // Returns true if this is a valid motif with artifacts
    fun isValid(): Boolean = artifacts != null && artifacts.isNotEmpty()

    // Returns the complete artifact sequence for this motif
    fun getPattern(): List<Artifact>? = artifacts?.toList()

    fun reset() {
        currentIndex = 0
    }
}

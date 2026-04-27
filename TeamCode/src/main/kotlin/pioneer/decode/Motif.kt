package pioneer.decode

class Motif(
    val aprilTagId: Int,
) {
    private val _artifacts: List<Artifact> =
        when (aprilTagId) {
            21 -> listOf(Artifact.GREEN, Artifact.PURPLE, Artifact.PURPLE)
            22 -> listOf(Artifact.PURPLE, Artifact.GREEN, Artifact.PURPLE)
            23 -> listOf(Artifact.PURPLE, Artifact.PURPLE, Artifact.GREEN)
            else -> emptyList()
        }

    val artifacts: List<Artifact>
        get() = _artifacts

    private var currentIndex: Int = 0

    // Returns the next artifact in the motif's sequence, cycling back to the start if needed
    fun getNextArtifact(): Artifact? {
        if (_artifacts.isEmpty()) return null
        val artifact = _artifacts[currentIndex]
        currentIndex = (currentIndex + 1) % _artifacts.size
        return artifact
    }

    // Returns the current artifact without advancing the index
    val currentArtifact: Artifact?
        get() {
            if (_artifacts.isEmpty()) return null
            return _artifacts[currentIndex]
        }

    // Returns the artifact at a specific position in the pattern (0-indexed)
    fun getArtifactAt(index: Int): Artifact? {
        if (index < 0 || index >= _artifacts.size) return null
        return _artifacts[index]
    }

    // Returns the current position in the pattern (0-indexed)
    fun getCurrentIndex(): Int = currentIndex

    // Returns true if this is a valid motif with artifacts
    fun isValid(): Boolean = _artifacts.isNotEmpty()

    // Returns the complete artifact sequence for this motif
    fun getPattern(): List<Artifact> = _artifacts

    fun reset() {
        currentIndex = 0
    }

    fun nextMotif(): Motif? {
        return when (aprilTagId) {
            21 -> Motif(22)
            22 -> Motif(23)
            23 -> Motif(21)
            else -> null
        }
    }

    fun prevMotif(): Motif? {
        return when (aprilTagId) {
            21 -> Motif(23)
            22 -> Motif(21)
            23 -> Motif(22)
            else -> null
        }
    }

    override fun toString(): String = getPattern().toString()
}

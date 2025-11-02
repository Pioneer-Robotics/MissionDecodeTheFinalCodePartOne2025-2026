package pioneer.decode

class Motif(
    val aprilTagId: Int
) {
    private val artifacts: List<Artifact>?
    private var currentIndex: Int = 0

    init {
        artifacts = when (aprilTagId) {
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
}


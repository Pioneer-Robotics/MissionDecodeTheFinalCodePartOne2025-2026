package pioneer.decode

import org.firstinspires.ftc.vision.opencv.ColorRange
import org.firstinspires.ftc.vision.opencv.PredominantColorProcessor

interface Artifact {
    val name: String
    val color: ColorRange
    val swatch: PredominantColorProcessor.Swatch
}

class PurpleArtifact : Artifact {
    override val name: String = "Purple"
    override val color: ColorRange = ColorRange.ARTIFACT_PURPLE
    override val swatch: PredominantColorProcessor.Swatch = PredominantColorProcessor.Swatch.ARTIFACT_PURPLE

    override fun toString(): String = "$name Artifact"
}

class GreenArtifact : Artifact {
    override val name: String = "Green"
    override val color: ColorRange = ColorRange.ARTIFACT_GREEN
    override val swatch: PredominantColorProcessor.Swatch = PredominantColorProcessor.Swatch.ARTIFACT_GREEN

    override fun toString(): String = "$name Artifact"
}

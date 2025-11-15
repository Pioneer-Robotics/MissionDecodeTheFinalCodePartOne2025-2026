package pioneer.decode

import org.firstinspires.ftc.vision.opencv.ColorRange
import org.firstinspires.ftc.vision.opencv.PredominantColorProcessor

enum class Artifact(
    val color: ColorRange,
    val swatch: PredominantColorProcessor.Swatch
) {
    PURPLE(ColorRange.ARTIFACT_PURPLE, PredominantColorProcessor.Swatch.ARTIFACT_PURPLE),
    GREEN(ColorRange.ARTIFACT_GREEN, PredominantColorProcessor.Swatch.ARTIFACT_GREEN);

    override fun toString() = "$name Artifact"
}

package pioneer.decode

import org.firstinspires.ftc.vision.opencv.ColorRange
import org.firstinspires.ftc.vision.opencv.PredominantColorProcessor.Swatch

enum class Artifact(
    val color: ColorRange,
    val swatch: Swatch,
) {
    PURPLE(ColorRange.ARTIFACT_PURPLE, Swatch.ARTIFACT_PURPLE),
    GREEN(ColorRange.ARTIFACT_GREEN, Swatch.ARTIFACT_GREEN),
    ;

    override fun toString() = "$name Artifact"
}

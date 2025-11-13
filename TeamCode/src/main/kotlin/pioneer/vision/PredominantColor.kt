package pioneer.vision

import org.firstinspires.ftc.vision.opencv.ImageRegion
import org.firstinspires.ftc.vision.opencv.PredominantColorProcessor
import org.firstinspires.ftc.vision.opencv.PredominantColorProcessor.Result
import org.firstinspires.ftc.vision.opencv.PredominantColorProcessor.Swatch

/**
 * The PredominantColorProcessor acts like a "Color Sensor", allowing you to define a Region of Interest (ROI)
 * of the camera stream inside of which the dominant color is found. Additionally, the detected color is matched
 * to one of the Swatches specified by the user as a "best guess" at the general shade of the color.
 */
class PredominantColor(
    vararg swatches: Swatch,
) : Processor {
    override val processor: PredominantColorProcessor =
        PredominantColorProcessor
            .Builder()
            .setRoi(ImageRegion.entireFrame())
            .setSwatches(*swatches)
            .build()

    /**
     * Get the full analysis result including closest swatch and color values.
     */
    fun getAnalysis(): Result = processor.getAnalysis()

    /**
     * Get the closest matching swatch to the predominant color.
     */
    fun getClosestSwatch(): Swatch? = getAnalysis().closestSwatch

    /**
     * Get RGB color values [Red 0-255, Green 0-255, Blue 0-255].
     */
    fun getRGB(): IntArray = getAnalysis().RGB

    /**
     * Get HSV color values [Hue 0-180, Saturation 0-255, Value 0-255].
     */
    fun getHSV(): IntArray = getAnalysis().HSV

    /**
     * Get YCrCb color values [Y 0-255, Cr 0-255, Cb 0-255].
     */
    fun getYCrCb(): IntArray = getAnalysis().YCrCb

    /**
     * Check if the detected color matches a specific swatch.
     */
    fun isColor(swatch: Swatch): Boolean = getClosestSwatch() == swatch
}

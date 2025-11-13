package pioneer.vision

import org.firstinspires.ftc.vision.opencv.ColorBlobLocatorProcessor.Blob
import org.firstinspires.ftc.vision.opencv.ColorBlobLocatorProcessor.BlobCriteria
import org.firstinspires.ftc.vision.opencv.ColorRange

/**
 * Manages multiple ColorBlob processors to detect multiple colors simultaneously.
 * Each color gets its own processor for optimal detection.
 */
class MultiColorBlob(
    vararg targetColors: ColorRange,
    draw: Boolean = false,
) {
    private val vps = targetColors.map { ColorBlob(it, draw) }

    val processors: List<ColorBlob>
        get() = vps

    /**
     * Get all detected blobs from all color processors combined.
     */
    fun getAllBlobs(): List<Blob> = vps.flatMap { it.getBlobs() }

    /**
     * Get blobs for a specific color by index (0-based).
     */
    fun getBlobsForColor(colorIndex: Int): List<Blob> = vps.getOrNull(colorIndex)?.getBlobs() ?: emptyList()

    /**
     * Get blobs filtered by criteria across all colors.
     */
    fun getBlobsByCriteria(
        criteria: BlobCriteria,
        minValue: Double,
        maxValue: Double,
    ): List<Blob> = vps.flatMap { it.getBlobsByCriteria(criteria, minValue, maxValue) }

    /**
     * Get the largest blob across all colors.
     */
    fun getLargestBlob(): Blob? = getAllBlobs().maxByOrNull { it.contourArea }

    /**
     * Get the most circular blob across all colors.
     */
    fun getMostCircular(): Blob? = getAllBlobs().maxByOrNull { it.circularity }

    /**
     * Get the densest blob across all colors.
     */
    fun getDensest(): Blob? = getAllBlobs().maxByOrNull { it.density }
}

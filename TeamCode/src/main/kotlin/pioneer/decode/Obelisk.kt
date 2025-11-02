package pioneer.decode

import org.firstinspires.ftc.vision.apriltag.AprilTagDetection
import pioneer.general.AllianceColor

class Obelisk {
    companion object {
        private val validMotifTagIds = setOf(21, 22, 23)

        fun isValidMotifTag(aprilTagId: Int): Boolean {
            return aprilTagId in validMotifTagIds
        }

        /**
         * Filters the AprilTag detections to find the correct motif tag based on alliance color.
         * If Blue alliance, look for right most valid tag (largest x in ftcPose)
         * If Red alliance, look for left most valid tag (smallest x in ftcPose)
         * Will work on close or far starting positions
         */
        fun detectMotif(detections: List<AprilTagDetection>, alliance: AllianceColor): Motif? {
            val validTags = detections.filter { it.ftcPose != null && isValidMotifTag(it.id) }
            val motifTagId = when (alliance) {
                AllianceColor.BLUE -> validTags.maxByOrNull { it.ftcPose.x }?.id
                AllianceColor.RED -> validTags.minByOrNull { it.ftcPose.x }?.id
            }
            return motifTagId?.let { Motif(it) }
        }
    }
}

package pioneer.helpers

import pioneer.localization.Pose
import kotlin.math.pow
import kotlin.math.sqrt

class PathUtils {
    companion object {

        fun poseDist(pose1: Pose, pose2: Pose): Double {
            return sqrt((pose2.x - pose1.x).pow(2.0) +
                           (pose2.y - pose1.y).pow(2.0))
        }

    }
}
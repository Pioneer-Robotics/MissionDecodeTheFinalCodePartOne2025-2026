package pioneer.decode

import pioneer.helpers.Pose

class AprilTagProcess (val aprilTagID: Int){
    val detectedTag: AprilTagMeta?

    init {
        detectedTag =
            when (aprilTagID){
                20 -> BlueGoalTag()
                21 -> ObeliskTag21()
                22 -> ObeliskTag22()
                23 -> ObeliskTag23()
                24 -> RedGoalTag()
                else -> null
            }
    }

    fun getTag(): AprilTagMeta?{
        return detectedTag
    }

}
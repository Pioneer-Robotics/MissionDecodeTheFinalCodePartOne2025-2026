package pioneer.decode

import org.firstinspires.ftc.robotcore.external.matrices.VectorF
import org.firstinspires.ftc.robotcore.external.navigation.Quaternion
import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase
import org.opencv.core.Mat
import pioneer.general.AllianceColor
import pioneer.helpers.MathUtils
import pioneer.helpers.Pose

private val decodeTagLibrary by lazy { AprilTagGameDatabase.getDecodeTagLibrary() }

interface AprilTagMeta {
    val id: Int
    val color: AllianceColor
    val name: String
        get() = decodeTagLibrary.lookupTag(id).name
    val databasePosition: VectorF
    val databaseOrientation: Quaternion
    val pose: Pose
    val zHeight: Double
}

abstract class DecodeAprilTag(
    override val id: Int,
    override val color: AllianceColor
) : AprilTagMeta {
    override val databasePosition: VectorF by lazy {
        decodeTagLibrary.lookupTag(id).fieldPosition
    }
    override val databaseOrientation: Quaternion by lazy {
        decodeTagLibrary.lookupTag(id).fieldOrientation
    }
    override val pose = Pose(MathUtils.inToCM(databasePosition[1].toDouble()), MathUtils.inToCM(-databasePosition[0].toDouble()), 0.0,0.0,0.0,0.0,MathUtils.quarternionToEuler(databaseOrientation).yaw,0.0,0.0)
    override val zHeight = databasePosition[2].toDouble()
}

// Helper extension function to convert VectorF to List<Double>
fun VectorF.toList(): List<Double> = listOf(this[0].toDouble(), this[1].toDouble(), this[2].toDouble())
fun VectorF.x() = this[0]
fun VectorF.y() = this[1]
fun VectorF.z() = this[2]

class BlueGoalTag : DecodeAprilTag(20, AllianceColor.BLUE)

class RedGoalTag : DecodeAprilTag(24, AllianceColor.RED)

class ObeliskTag21: DecodeAprilTag(21, AllianceColor.NEUTRAL)

class ObeliskTag22: DecodeAprilTag(22, AllianceColor.NEUTRAL)

class ObeliskTag23: DecodeAprilTag(23, AllianceColor.NEUTRAL){
    override val databasePosition = VectorF(0f,0f,0f)
    override val databaseOrientation = Quaternion(0f,0f,0f,0f,0)
    override val pose = Pose (0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0,0.0)
}


// class ObeliskTag : AprilTag(0, AllianceColor.NEUTRAL)

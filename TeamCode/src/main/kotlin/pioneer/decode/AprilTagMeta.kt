package pioneer.decode

import org.firstinspires.ftc.robotcore.external.matrices.VectorF
import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase
import pioneer.general.AllianceColor

private val decodeTagLibrary by lazy { AprilTagGameDatabase.getDecodeTagLibrary() }

interface AprilTagMeta {
    val id: Int
    val color: AllianceColor
    val name: String
        get() = decodeTagLibrary.lookupTag(id).name
    val position: VectorF
}

abstract class DecodeAprilTag(
    override val id: Int,
    override val color: AllianceColor
) : AprilTagMeta {
    override val position: VectorF by lazy {
        decodeTagLibrary.lookupTag(id).fieldPosition
    }
}

// Helper extension function to convert VectorF to List<Double>
fun VectorF.toList(): List<Double> = listOf(this[0].toDouble(), this[1].toDouble(), this[2].toDouble())
fun VectorF.x() = this[0]
fun VectorF.y() = this[1]
fun VectorF.z() = this[2]

class BlueGoalTag : DecodeAprilTag(20, AllianceColor.BLUE)

class RedGoalTag : DecodeAprilTag(24, AllianceColor.RED)

// class ObeliskTag : AprilTag(0, AllianceColor.NEUTRAL)

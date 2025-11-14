package pioneer.decode

import org.firstinspires.ftc.robotcore.external.matrices.VectorF
import org.firstinspires.ftc.vision.apriltag.AprilTagGameDatabase

interface AprilTagMeta{
    val name: String
    val id: Int
    val color: Color
    val positionVector: VectorF
    val position: List<Double>

}

enum class Color{
    BLUE,
    RED,
    NEUTRAL
}


class blueGoalTag: AprilTagMeta{
    override val name = "Blue Goal Tag"
    override val id = 20
    override val color = Color.BLUE
    override val positionVector = AprilTagGameDatabase.getDecodeTagLibrary().lookupTag(id).fieldPosition
    override val position = listOf(positionVector[0], positionVector[1], positionVector[2])
}

class redGoalTag: AprilTagMeta{
    override val name = "Red Goal Tag"
    override val id = 24
    override val color = Color.RED
    override val positionVector = AprilTagGameDatabase.getDecodeTagLibrary().lookupTag(this.id).fieldPosition
    override val position = listOf(positionVector[0], positionVector[1], positionVector[2])
}

class obeliskTag: AprilTagMeta{
    override val name = "Obelisk Tag"
    override val id = 24
    override val color = Color.NEUTRAL
    override val positionVector = AprilTagGameDatabase.getDecodeTagLibrary().lookupTag(this.id).fieldPosition
    override val position = listOf(positionVector[0], positionVector[1], positionVector[2])

}
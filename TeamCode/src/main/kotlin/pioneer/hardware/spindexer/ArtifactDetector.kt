package pioneer.hardware.spindexer

import pioneer.decode.Artifact
import pioneer.hardware.RevColorSensor

class ArtifactDetector(val sensor: RevColorSensor) {
    fun detect(): Artifact? {
        if (sensor.distance > 7.0) return null
        return when (sensor.hue) {
            in 145.0..170.0 -> Artifact.GREEN
            in 195.0..230.0 -> Artifact.PURPLE
            else -> null
        }
    }
}

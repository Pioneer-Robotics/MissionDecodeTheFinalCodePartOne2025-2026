package pioneer.hardware.spindexer

import pioneer.decode.Artifact
import pioneer.hardware.RevColorSensor

class ArtifactDetector(val sensor: RevColorSensor) {
    fun detect(): Artifact? {
        if (sensor.distance > 8.0) return null
        return when (sensor.hue) {
            in 130.0..170.0 -> Artifact.GREEN
            in 170.0..250.0 -> Artifact.PURPLE
            else -> null
        }
    }
}

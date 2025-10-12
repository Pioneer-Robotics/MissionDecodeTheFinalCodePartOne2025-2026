package pioneer.helpers

import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import pioneer.localization.Pose
import pioneer.pathing.paths.Path

/**
 * A utility class for plotting data on the FTC Dashboard.
 */
class DashboardPlotter {
    companion object {
        const val MAX_PREVIOUS_POSITIONS = 500
        private val previousPositions = mutableListOf<Pose>()

        fun toDashboardCoordinates(pose: Pose): Pose {
            // Convert from robot coordinates to dashboard coordinates
            val newPose = Pose(pose.y, -pose.x, pose.heading)
            newPose.scale(160.0 / 366.0) // Scale to make field the right size (366 cm wide)
            newPose.add(Pose(-72.5, 72.5)) // Offset to center the field on the dashboard
            return newPose
        }

        fun plotBotPosition(packet: TelemetryPacket, position: Pose, showPathTaken: Boolean = true, color: String = "#000000") {
            val pose = toDashboardCoordinates(position)
            previousPositions.add(pose)
            if (previousPositions.size > MAX_PREVIOUS_POSITIONS) {
                previousPositions.removeAt(0)
            }
            val tl = Pose(-8.0, -8.0) // Top-left corner
            val tr = Pose(8.0, -8.0) // Top-right corner
            val br = Pose(8.0, 8.0) // Bottom-right corner
            val bl = Pose(-8.0, 8.0) // Bottom-left corner
            for (corner in arrayOf(tl, tr, br, bl)) {
                corner.rotate(-pose.heading)
                corner.add(pose)
            }
            packet.fieldOverlay()
                .setStroke(color)
                .strokePolygon(
                    doubleArrayOf(tl.x, tr.x, br.x, bl.x, tl.x),
                    doubleArrayOf(tl.y, tr.y, br.y, bl.y, tl.y)
                )
            if (showPathTaken) {
                packet.fieldOverlay()
                    .strokePolyline(
                        previousPositions.map { it.x }.toDoubleArray(),
                        previousPositions.map { it.y }.toDoubleArray()
                    )
            }
        }

        fun plotPath(packet: TelemetryPacket, path: Path, color: String = "#0000FF") {
            // Plot the path as a polyline with 100 samples
            val xSamples = DoubleArray(100)
            val ySamples = DoubleArray(100)
            for (i in 0 until 100) {
                val t = i / 99.0 // Normalize t to [0, 1]
                val point = toDashboardCoordinates(path.getPoint(t))
                xSamples[i] = point.x
                ySamples[i] = point.y
            }

            packet.fieldOverlay()
                .setStroke(color)
                .strokePolyline(xSamples, ySamples)
        }

        fun plotPoint(packet: TelemetryPacket, point: Pose, color: String = "#FF0000") {
            val dashboardPoint = toDashboardCoordinates(point)
            packet.fieldOverlay()
                .setFill(color)
                .fillCircle(dashboardPoint.x, dashboardPoint.y, 2.0) // Draw a small circle at the lookahead point
        }

        fun plotCircle(packet: TelemetryPacket, center: Pose, radius: Double, color: String = "#FF0000") {
            val dashboardCenter = toDashboardCoordinates(center)
            packet.fieldOverlay()
                .setStroke(color)
                .strokeCircle(dashboardCenter.x, dashboardCenter.y, radius * 160.0 / 366.0) // Scale radius to match field size
        }

        fun plotGrid(packet: TelemetryPacket) {
            packet.fieldOverlay()
                .setStroke("#888888")
                .drawGrid(0.0, 0.0, 144.0, 144.0, 7, 7)
                .setStrokeWidth(2)
                .setStroke("#222222")
                .drawGrid(0.0, 0.0, 144.0, 144.0, 2, 2)
        }

        fun clearPreviousPositions() {
            previousPositions.clear()
        }
    }
}
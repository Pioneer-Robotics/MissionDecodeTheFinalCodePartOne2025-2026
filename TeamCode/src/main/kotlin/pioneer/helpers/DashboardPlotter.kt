package pioneer.helpers

import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import pioneer.pathing.paths.Path
import kotlin.math.cos
import kotlin.math.sin

/**
 * A utility class for plotting data on the FTC Dashboard.
 */
object DashboardPlotter {
    const val MAX_PREVIOUS_POSITIONS = 500
    private val previousPositions = mutableListOf<Pose>()

    var scale = 1.0

    fun toDashboardCoordinates(pose: Pose): Pose {
        // Convert from robot coordinates to dashboard coordinates
        val scale = 160.0 / 366.0 * scale // Scale to make field the right size (366 cm wide)
        return Pose(
            x = (pose.y * scale) - 72.5, // Offset to center the field
            y = (-pose.x * scale) + 72.5,
            theta = pose.theta,
        )
    }

    fun plotBotPosition(
        packet: TelemetryPacket,
        position: Pose,
        showPathTaken: Boolean = true,
        color: String = "#000000",
    ) {
        val pose = toDashboardCoordinates(position)
        previousPositions.add(pose)
        if (previousPositions.size > MAX_PREVIOUS_POSITIONS) {
            previousPositions.removeAt(0)
        }

        // Create robot corners (8cm x 8cm robot)
        val corners =
            arrayOf(
                Pose(-8.0, -8.0), // Top-left
                Pose(8.0, -8.0), // Top-right
                Pose(8.0, 8.0), // Bottom-right
                Pose(-8.0, 8.0), // Bottom-left
            )

        // Rotate corners by robot heading and translate to robot position
        val cosTheta = cos(pose.theta)
        val sinTheta = sin(pose.theta)
        val rotatedCorners =
            corners.map { corner ->
                Pose(
                    x = pose.x + (corner.x * cosTheta - corner.y * sinTheta),
                    y = pose.y + (corner.x * sinTheta + corner.y * cosTheta),
                    theta = 0.0,
                )
            }

        packet
            .fieldOverlay()
            .setStroke(color)
            .strokePolygon(
                rotatedCorners.map { it.x }.toDoubleArray() + rotatedCorners[0].x,
                rotatedCorners.map { it.y }.toDoubleArray() + rotatedCorners[0].y,
            )

        if (showPathTaken) {
            packet
                .fieldOverlay()
                .strokePolyline(
                    previousPositions.map { it.x }.toDoubleArray(),
                    previousPositions.map { it.y }.toDoubleArray(),
                )
        }
    }

    fun plotPath(
        packet: TelemetryPacket,
        path: Path,
        color: String = "#0000FF",
    ) {
        // Plot the path as a polyline with 100 samples
        val points =
            (0 until 100).map { i ->
                val t = i / 99.0 // Normalize t to [0, 1]
                toDashboardCoordinates(path.getPoint(t))
            }

        packet
            .fieldOverlay()
            .setStroke(color)
            .strokePolyline(
                points.map { it.x }.toDoubleArray(),
                points.map { it.y }.toDoubleArray(),
            )
    }

    fun plotPoint(
        packet: TelemetryPacket,
        point: Pose,
        color: String = "#FF0000",
    ) {
        val dashboardPoint = toDashboardCoordinates(point)
        packet
            .fieldOverlay()
            .setFill(color)
            .fillCircle(dashboardPoint.x, dashboardPoint.y, 2.0)
    }

    fun plotCircle(
        packet: TelemetryPacket,
        center: Pose,
        radius: Double,
        color: String = "#FF0000",
    ) {
        val dashboardCenter = toDashboardCoordinates(center)
        val scaledRadius = radius * 160.0 / 366.0 // Scale radius to match field size
        packet
            .fieldOverlay()
            .setStroke(color)
            .strokeCircle(dashboardCenter.x, dashboardCenter.y, scaledRadius)
    }

    fun plotGrid(packet: TelemetryPacket) {
        packet
            .fieldOverlay()
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

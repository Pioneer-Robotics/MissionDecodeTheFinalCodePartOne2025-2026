package pioneer.visualizer

import pioneer.helpers.Pose
import pioneer.pathing.paths.*
import java.awt.*
import java.awt.event.*
import java.awt.geom.*
import java.io.File
import javax.swing.*
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.*

open class EnhancedPioneerPathVisualizer : JFrame("Pioneer Robotics - DECODE 2025-2026 Path Visualizer") {

    private val FIELD_SIZE = 144.0  // FTC field is 144" x 144"
    private val TILE_SIZE = 24.0    // Each tile is 24" x 24"

    private val loadedPaths = mutableMapOf<String, PathData>()
    private var primaryPath: PathData? = null
    private val comparisonPaths = mutableListOf<PathData>()

    private var animationProgress = 0.0
    private var isAnimating = false
    private var animationSpeed = 1.0
    private var showVelocityVectors = true
    private var showWaypoints = true
    private var showFieldElements = true
    private var showTiming = true
    private var showOptimizationHints = true

    private val fieldPanel = FieldPanel()
    private val infoPanel = InfoPanel()
    private val animationTimer = Timer(20) { updateAnimation() }

    // FIXED: Make combo box a field so we can populate it after subclass init
    private lateinit var pathComboBox: JComboBox<String>

    // DECODE 2025-2026 Field Elements (in inches from origin)
    private val fieldElements = FieldElements2025()

    init {
        layout = BorderLayout()

        // Menu bar
        jMenuBar = createMenuBar()

        // Field display
        add(fieldPanel, BorderLayout.CENTER)

        // Control panel
        add(createControlPanel(), BorderLayout.SOUTH)

        // Info panel
        add(infoPanel, BorderLayout.EAST)

        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(1400, 900)
        setLocationRelativeTo(null)
        isVisible = true

        // FIXED: Populate combo box AFTER subclass is fully initialized
        SwingUtilities.invokeLater {
            populatePathComboBox()
        }
    }

    // FIXED: New method to populate the combo box after initialization
    private fun populatePathComboBox() {
        pathComboBox.removeAllItems()
        for (path in getAvailablePaths()) {
            pathComboBox.addItem(path)
        }
    }

    data class PathData(
        val name: String,
        val path: Path,
        val color: Color,
        val maxVelocity: Double = 40.0,  // inches/sec
        val maxAcceleration: Double = 30.0,  // inches/sec^2
        var visible: Boolean = true
    ) {
        fun getLength(): Double {
            // Use the path's built-in length method
            return path.getLength()
        }

        fun getTimeAtT(t: Double): Double {
            // Simplified timing calculation
            val distance = getLength() * t
            // Use average velocity for now (could be enhanced with motion profile)
            return distance / (maxVelocity * 0.7)  // Assume 70% of max velocity on average
        }

        fun getTotalTime(): Double = getLength() / (maxVelocity * 0.7)
    }

    inner class FieldPanel : JPanel() {
        var scale = 1.0
        var offsetX = 0.0
        var offsetY = 0.0

        init {
            preferredSize = Dimension(900, 900)
            background = Color(30, 30, 30)

            // Mouse wheel zoom
            addMouseWheelListener { e ->
                val zoomFactor = if (e.wheelRotation < 0) 1.1 else 0.9
                scale *= zoomFactor
                scale = scale.coerceIn(0.5, 8.0)
                repaint()
            }

            // Mouse drag to pan
            var lastPoint: Point? = null
            addMouseListener(object : MouseAdapter() {
                override fun mousePressed(e: MouseEvent) {
                    lastPoint = e.point
                }
                override fun mouseReleased(e: MouseEvent) {
                    lastPoint = null
                }
            })

            addMouseMotionListener(object : MouseMotionAdapter() {
                override fun mouseDragged(e: MouseEvent) {
                    lastPoint?.let { last ->
                        offsetX += e.x - last.x
                        offsetY += e.y - last.y
                        lastPoint = e.point
                        repaint()
                    }
                }
            })
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2d = g as Graphics2D
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)

            // Calculate default scale
            if (offsetX == 0.0 && offsetY == 0.0) {
                scale = minOf(width, height) / (FIELD_SIZE * 1.1)
                offsetX = (width - FIELD_SIZE * scale) / 2
                offsetY = (height - FIELD_SIZE * scale) / 2
            }

            // Draw field
            drawField(g2d)

            // Draw field elements
            if (showFieldElements) {
                fieldElements.draw(g2d, this@FieldPanel)
            }

            // Draw comparison paths
            for (pathData in comparisonPaths) {
                if (pathData.visible) {
                    drawPath(g2d, pathData, alpha = 100)
                }
            }

            // Draw primary path
            primaryPath?.let { pathData ->
                drawPath(g2d, pathData)

                // Draw robot at current position
                if (isAnimating || animationProgress > 0) {
                    drawRobotAtPosition(g2d, pathData, animationProgress)
                }

                // Draw timing markers
                if (showTiming) {
                    drawTimingMarkers(g2d, pathData)
                }

                // Draw optimization hints
                if (showOptimizationHints) {
                    drawOptimizationHints(g2d, pathData)
                }
            }

            // Draw legend
            drawLegend(g2d)
        }

        private fun drawField(g2d: Graphics2D) {
            // Field background (gray mat)
            g2d.color = Color(50, 50, 50)
            g2d.fillRect(
                offsetX.toInt(),
                offsetY.toInt(),
                (FIELD_SIZE * scale).toInt(),
                (FIELD_SIZE * scale).toInt()
            )

            // Grid lines (24" tiles)
            g2d.color = Color(70, 70, 70)
            g2d.stroke = BasicStroke(1f)
            for (i in 0..6) {
                val pos = i * TILE_SIZE
                val x = toScreenX(pos).toInt()
                val y = toScreenY(pos).toInt()

                g2d.drawLine(x, toScreenY(0.0).toInt(), x, toScreenY(FIELD_SIZE).toInt())
                g2d.drawLine(toScreenX(0.0).toInt(), y, toScreenX(FIELD_SIZE).toInt(), y)
            }

            // Tile labels
            g2d.color = Color(100, 100, 100)
            g2d.font = Font("Arial", Font.PLAIN, 10)
            val tileLabels = listOf("A", "B", "C", "D", "E", "F")
            for (i in 0..5) {
                for (j in 0..5) {
                    val x = i * TILE_SIZE + TILE_SIZE / 2
                    val y = j * TILE_SIZE + TILE_SIZE / 2
                    val pos = toScreenX(x) to toScreenY(y)
                    val label = "${tileLabels[i]}${j + 1}"
                    g2d.drawString(label, pos.first.toFloat() - 8, pos.second.toFloat() + 4)
                }
            }

            // Field perimeter
            g2d.color = Color.WHITE
            g2d.stroke = BasicStroke(3f)
            g2d.drawRect(
                offsetX.toInt(),
                offsetY.toInt(),
                (FIELD_SIZE * scale).toInt(),
                (FIELD_SIZE * scale).toInt()
            )
        }

        private fun drawPath(g2d: Graphics2D, pathData: PathData, alpha: Int = 255) {
            val resolution = 200

            // FEATURE 1: Draw robot footprint trail FIRST (so path draws on top)
            if (pathData == primaryPath && showFieldElements) {
                drawRobotFootprint(g2d, pathData)
            }

            // Draw path with velocity color-coding
            val baseColor = pathData.color
            g2d.stroke = BasicStroke(3f)

            val pathShape = Path2D.Double()
            var isFirst = true

            for (i in 0..resolution) {
                val t = i.toDouble() / resolution
                val pose = pathData.path.getPose(t)

                // FEATURE 2: Velocity-based color coding
                val velocity = sqrt(pose.vx * pose.vx + pose.vy * pose.vy)
                val speedColor = getVelocityColor(velocity, pathData.maxVelocity)
                g2d.color = Color(speedColor.red, speedColor.green, speedColor.blue, alpha)

                val x = toScreenX(pose.x)
                val y = toScreenY(pose.y)

                if (isFirst) {
                    pathShape.moveTo(x, y)
                    isFirst = false
                } else {
                    pathShape.lineTo(x, y)
                    // Draw small segment with velocity color
                    if (i > 0 && i % 5 == 0) {  // Every 5 points to show color changes
                        g2d.draw(pathShape)
                        pathShape.reset()
                        pathShape.moveTo(x, y)
                    }
                }
            }
            g2d.draw(pathShape)

            // Draw start/end
            drawStartEnd(g2d, pathData.path, baseColor, alpha)

            if (pathData == primaryPath && showVelocityVectors) {
                drawVelocityVectors(g2d, pathData.path, resolution)
            }
        }

        // FEATURE 1: Robot footprint trail
        private fun drawRobotFootprint(g2d: Graphics2D, pathData: PathData) {
            g2d.color = Color(0, 255, 0, 30)  // Semi-transparent green
            g2d.stroke = BasicStroke(1.5f)

            val numFootprints = 15  // Show 15 robot positions along path
            for (i in 0..numFootprints) {
                val t = i.toDouble() / numFootprints
                val pose = pathData.path.getPoint(t)

                val robotSize = 18.0  // 18" square robot

                // Draw rotated robot square
                val x = toScreenX(pose.x)
                val y = toScreenY(pose.y)

                val halfSize = robotSize / 2 * scale

                // Create rectangle corners
                val corners = listOf(
                    Point2D.Double(-halfSize, -halfSize),
                    Point2D.Double(halfSize, -halfSize),
                    Point2D.Double(halfSize, halfSize),
                    Point2D.Double(-halfSize, halfSize)
                )

                // FIXED: Rotate corners by theta, accounting for theta=0 being +Y direction
                // Apply same rotation correction as in drawRobotAtPosition
                val rotatedCorners = corners.map { corner ->
                    val adjustedTheta = -pose.theta - PI/2  // Same correction as drawRobotAtPosition
                    val cos = cos(adjustedTheta)
                    val sin = sin(adjustedTheta)
                    Point2D.Double(
                        x + corner.x * cos - corner.y * sin,
                        y + corner.x * sin + corner.y * cos
                    )
                }

                // Draw robot footprint
                val footprint = Path2D.Double()
                footprint.moveTo(rotatedCorners[0].x, rotatedCorners[0].y)
                for (j in 1..3) {
                    footprint.lineTo(rotatedCorners[j].x, rotatedCorners[j].y)
                }
                footprint.closePath()
                g2d.draw(footprint)
            }
        }

        // FEATURE 2: Velocity color mapping
        private fun getVelocityColor(velocity: Double, maxVelocity: Double): Color {
            val ratio = (velocity / maxVelocity).coerceIn(0.0, 1.2)
            return when {
                ratio > 1.0 -> Color.RED       // Exceeds max velocity!
                ratio > 0.8 -> Color.ORANGE    // High speed (80-100%)
                ratio > 0.5 -> Color.YELLOW    // Medium speed (50-80%)
                ratio > 0.2 -> Color.GREEN     // Low speed (20-50%)
                else -> Color.CYAN             // Very slow (0-20%)
            }
        }

        private fun drawStartEnd(g2d: Graphics2D, path: Path, color: Color, alpha: Int = 255) {
            val startPose = path.getPoint(0.0)
            val endPose = path.getPoint(1.0)

            val startX = toScreenX(startPose.x).toInt()
            val startY = toScreenY(startPose.y).toInt()
            val endX = toScreenX(endPose.x).toInt()
            val endY = toScreenY(endPose.y).toInt()

            g2d.color = Color(0, 255, 0, alpha)
            g2d.fillOval(startX - 8, startY - 8, 16, 16)
            g2d.color = Color.BLACK
            g2d.font = Font("Arial", Font.BOLD, 10)
            g2d.drawString("START", startX + 12, startY + 4)

            g2d.color = Color(255, 0, 0, alpha)
            g2d.fillOval(endX - 8, endY - 8, 16, 16)
            g2d.color = Color.BLACK
            g2d.drawString("END", endX + 12, endY + 4)
        }

        private fun drawVelocityVectors(g2d: Graphics2D, path: Path, resolution: Int) {
            g2d.color = Color(255, 255, 0, 150)
            g2d.stroke = BasicStroke(2f)

            for (i in 0..resolution step 10) {
                val t = i.toDouble() / resolution
                val pose = path.getPoint(t)

                val x = toScreenX(pose.x).toInt()
                val y = toScreenY(pose.y).toInt()

                val velMag = sqrt(pose.vx * pose.vx + pose.vy * pose.vy)
                if (velMag > 0.001) {
                    val vectorScale = 10.0
                    val endX = (x + pose.vx * vectorScale).toInt()
                    val endY = (y - pose.vy * vectorScale).toInt()

                    g2d.drawLine(x, y, endX, endY)

                    val angle = atan2(-pose.vy, pose.vx)
                    drawArrowhead(g2d, endX, endY, angle, 6)
                }
            }
        }

        private fun drawTimingMarkers(g2d: Graphics2D, pathData: PathData) {
            g2d.color = Color(255, 165, 0, 200)  // Orange
            g2d.font = Font("Arial", Font.PLAIN, 10)
            g2d.stroke = BasicStroke(2f)

            val totalTime = pathData.getTotalTime()
            val numMarkers = min(10, totalTime.toInt() + 1)

            for (i in 1 until numMarkers) {
                val t = i.toDouble() / numMarkers
                val pose = pathData.path.getPoint(t)
                val x = toScreenX(pose.x).toInt()
                val y = toScreenY(pose.y).toInt()

                g2d.drawOval(x - 3, y - 3, 6, 6)
                g2d.drawString("${String.format("%.1f", pathData.getTimeAtT(t))}s", x + 5, y - 5)
            }
        }

        private fun drawOptimizationHints(g2d: Graphics2D, pathData: PathData) {
            val hints = analyzePathOptimization(pathData)

            g2d.font = Font("Arial", Font.BOLD, 11)

            for ((index, hint) in hints.withIndex()) {
                val x = toScreenX(hint.position.x).toInt()
                val y = toScreenY(hint.position.y).toInt()

                // Draw warning icon
                g2d.color = hint.severity.color
                g2d.fillOval(x - 6, y - 6, 12, 12)
                g2d.color = Color.WHITE
                g2d.drawString("!", x - 3, y + 4)

                // Draw hint text (show first 3 hints)
                if (index < 3) {
                    g2d.color = Color(0, 0, 0, 220)
                    g2d.fillRect(x + 10, y - 20, 200, 35)
                    g2d.color = Color.WHITE
                    g2d.font = Font("Arial", Font.PLAIN, 9)
                    drawWrappedString(g2d, hint.message, x + 15, y - 15, 190)
                }
            }
        }

        private fun drawRobotAtPosition(g2d: Graphics2D, pathData: PathData, t: Double) {
            val pose = pathData.path.getPoint(t)

            val x = toScreenX(pose.x).toInt()
            val y = toScreenY(pose.y).toInt()

            val robotSize = (18.0 * scale).toInt()

            // CRITICAL FIX: Robot coordinate system has theta=0 pointing in +Y direction (forward/toward goal)
            // The visualizer needs to:
            // 1. Add PI/2 to rotate from +Y reference to +X reference (standard graphics rotation)
            // 2. Negate to account for screen Y-axis being flipped (increases downward)
            // So: screen_rotation = -(theta + PI/2) = -theta - PI/2
            val heading = pose.theta

            // Save graphics state for rotation
            val oldTransform = g2d.transform

            // Translate to robot center, rotate, then draw
            g2d.translate(x, y)
            // FIXED: Account for theta=0 being +Y direction in robot frame
            // Add -PI/2 to make theta=0 point upward (which is +Y in field coords, -Y in screen coords)
            g2d.rotate(-heading - PI/2)

            // Draw robot body (centered at origin after translation)
            g2d.color = Color(0, 100, 255, 200)
            val rect = Rectangle(
                -robotSize / 2,
                -robotSize / 2,
                robotSize,
                robotSize
            )
            g2d.fill(rect)

            g2d.color = Color.WHITE
            g2d.stroke = BasicStroke(2f)
            g2d.draw(rect)

            // Draw heading indicator (front arrow) - points in +X direction of rotated frame
            g2d.color = Color.RED
            g2d.stroke = BasicStroke(3f)
            val frontLength = robotSize / 2
            g2d.drawLine(0, 0, frontLength, 0)

            // Arrowhead at front
            val arrowSize = 8
            g2d.drawLine(frontLength, 0, frontLength - arrowSize, -arrowSize/2)
            g2d.drawLine(frontLength, 0, frontLength - arrowSize, arrowSize/2)

            // Draw side markers to show rotation clearly
            g2d.color = Color.YELLOW
            g2d.stroke = BasicStroke(2f)
            // Left side marker
            g2d.drawLine(-robotSize/2, 0, -robotSize/2 + 6, 0)
            // Right side marker
            g2d.drawLine(robotSize/2, 0, robotSize/2 - 6, 0)

            // Restore graphics state
            g2d.transform = oldTransform

            // Show position, time, and rotation angle
            g2d.color = Color.WHITE
            g2d.font = Font("Arial", Font.PLAIN, 10)
            val timeAtPos = pathData.getTimeAtT(t)
            val thetaDegrees = Math.toDegrees(pose.theta)
            g2d.drawString(
                String.format("t=%.2fs (%.1f, %.1f) θ=%.0f°", timeAtPos, pose.x, pose.y, thetaDegrees),
                x + robotSize / 2 + 5,
                y
            )
        }

        private fun drawArrowhead(g2d: Graphics2D, x: Int, y: Int, angle: Double, size: Int) {
            val angle1 = angle + Math.toRadians(150.0)
            val angle2 = angle - Math.toRadians(150.0)

            val x1 = (x + size * cos(angle1)).toInt()
            val y1 = (y - size * sin(angle1)).toInt()
            val x2 = (x + size * cos(angle2)).toInt()
            val y2 = (y - size * sin(angle2)).toInt()

            g2d.drawLine(x, y, x1, y1)
            g2d.drawLine(x, y, x2, y2)
        }

        private fun drawWrappedString(g2d: Graphics2D, text: String, x: Int, y: Int, width: Int) {
            val words = text.split(" ")
            var line = ""
            var yOffset = y

            for (word in words) {
                val testLine = if (line.isEmpty()) word else "$line $word"
                val metrics = g2d.fontMetrics
                if (metrics.stringWidth(testLine) > width) {
                    g2d.drawString(line, x, yOffset)
                    line = word
                    yOffset += 12
                } else {
                    line = testLine
                }
            }
            g2d.drawString(line, x, yOffset)
        }

        private fun drawLegend(g2d: Graphics2D) {
            g2d.color = Color(0, 0, 0, 200)
            g2d.fillRect(10, 10, 280, if (comparisonPaths.isNotEmpty()) 240 else 200)

            g2d.color = Color.WHITE
            g2d.font = Font("Arial", Font.BOLD, 12)
            var y = 30

            g2d.drawString("Controls:", 20, y)
            g2d.font = Font("Arial", Font.PLAIN, 11)
            y += 18
            g2d.drawString("Space: Play/Pause", 20, y)
            y += 15
            g2d.drawString("R: Reset Animation", 20, y)
            y += 15
            g2d.drawString("V: Toggle Velocity Vectors", 20, y)
            y += 15
            g2d.drawString("F: Toggle Field Elements", 20, y)
            y += 15
            g2d.drawString("T: Toggle Timing", 20, y)
            y += 15
            g2d.drawString("O: Toggle Optimization", 20, y)
            y += 15
            g2d.drawString("Mouse Wheel: Zoom", 20, y)
            y += 15
            g2d.drawString("Click+Drag: Pan", 20, y)

            // Velocity color legend
            y += 20
            g2d.font = Font("Arial", Font.BOLD, 12)
            g2d.drawString("Path Speed:", 20, y)
            g2d.font = Font("Arial", Font.PLAIN, 10)
            y += 15

            val speedColors = listOf(
                Triple(Color.CYAN, "Very Slow", "0-20%"),
                Triple(Color.GREEN, "Low", "20-50%"),
                Triple(Color.YELLOW, "Medium", "50-80%"),
                Triple(Color.ORANGE, "High", "80-100%"),
                Triple(Color.RED, "OVER MAX!", ">100%")
            )

            for ((color, label, range) in speedColors) {
                g2d.color = color
                g2d.fillRect(20, y - 8, 15, 10)
                g2d.color = Color.WHITE
                g2d.drawString("$label ($range)", 40, y)
                y += 12
            }

            if (comparisonPaths.isNotEmpty()) {
                y += 8
                g2d.font = Font("Arial", Font.BOLD, 12)
                g2d.drawString("Comparing Paths:", 20, y)
                y += 5

                primaryPath?.let {
                    y += 15
                    g2d.color = it.color
                    g2d.fillRect(20, y - 8, 15, 10)
                    g2d.color = Color.WHITE
                    g2d.font = Font("Arial", Font.PLAIN, 10)
                    g2d.drawString("${it.name} (primary)", 40, y)
                }

                for (path in comparisonPaths.take(3)) {
                    y += 15
                    g2d.color = path.color
                    g2d.fillRect(20, y - 8, 15, 10)
                    g2d.color = Color.WHITE
                    g2d.font = Font("Arial", Font.PLAIN, 10)
                    g2d.drawString(path.name, 40, y)
                }
            }
        }

        fun toScreenX(fieldX: Double): Double = offsetX + fieldX * scale
        fun toScreenY(fieldY: Double): Double = offsetY + (FIELD_SIZE - fieldY) * scale
    }

    inner class InfoPanel : JPanel() {
        private val pathInfoArea = JTextArea(25, 25)

        init {
            layout = BorderLayout()
            background = Color.DARK_GRAY
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10)
            preferredSize = Dimension(320, 0)

            val scrollPane = JScrollPane(pathInfoArea)
            pathInfoArea.isEditable = false
            pathInfoArea.background = Color(40, 40, 40)
            pathInfoArea.foreground = Color.WHITE
            pathInfoArea.font = Font("Monospaced", Font.PLAIN, 11)

            add(JLabel("Path Analysis").apply {
                foreground = Color.WHITE
                font = Font("Arial", Font.BOLD, 14)
            }, BorderLayout.NORTH)

            add(scrollPane, BorderLayout.CENTER)

            updateInfo()
        }

        fun updateInfo() {
            val sb = StringBuilder()

            primaryPath?.let { pathData ->
                sb.appendLine("=== PRIMARY PATH ===")
                sb.appendLine("Name: ${pathData.name}")
                sb.appendLine()

                // FEATURE 3 & 4: Cycle time and efficiency
                val pathLength = pathData.getLength()
                val totalTime = pathData.getTotalTime()
                val avgSpeed = pathLength / totalTime

                sb.appendLine("--- Performance Metrics ---")
                sb.appendLine(String.format("Length: %.2f in", pathLength))
                sb.appendLine(String.format("Total Time: %.2f sec", totalTime))
                sb.appendLine(String.format("Avg Speed: %.1f in/s", avgSpeed))

                // FEATURE 4: Path efficiency score
                val startPose = pathData.path.getPoint(0.0)
                val endPose = pathData.path.getPoint(1.0)
                val straightLineDistance = sqrt(
                    (endPose.x - startPose.x).pow(2) +
                            (endPose.y - startPose.y).pow(2)
                )
                val efficiency = (straightLineDistance / pathLength * 100).coerceIn(0.0, 100.0)

                sb.appendLine(String.format("Efficiency: %.1f%%", efficiency))
                if (efficiency < 80) {
                    sb.appendLine("  ⚠ Consider more direct route")
                } else if (efficiency > 90) {
                    sb.appendLine("  ✓ Very efficient path!")
                }
                sb.appendLine()

                // FEATURE 3: Cycle time breakdown
                sb.appendLine("--- Cycle Time Breakdown ---")
                // Estimate: 70% driving, 30% actions (shooting, collecting)
                val drivingTime = totalTime * 0.7
                val actionTime = totalTime * 0.3

                sb.appendLine(String.format("Driving: %.2f sec (70%%)", drivingTime))
                sb.appendLine(String.format("Actions: %.2f sec (30%%)", actionTime))
                sb.appendLine(String.format("Total: %.2f sec", totalTime))

                if (totalTime > 30) {
                    sb.appendLine("  ⚠ EXCEEDS 30 sec auto period!")
                } else if (totalTime > 25) {
                    sb.appendLine("  ⚠ Cutting it close to 30 sec")
                } else {
                    sb.appendLine(String.format("  ✓ %.1f sec buffer remaining", 30 - totalTime))
                }
                sb.appendLine()

                sb.appendLine("--- Poses ---")
                sb.appendLine(String.format("Start: (%.1f, %.1f)", startPose.x, startPose.y))
                sb.appendLine(String.format("End: (%.1f, %.1f)", endPose.x, endPose.y))
                sb.appendLine()

                // Optimization analysis
                val hints = analyzePathOptimization(pathData)
                if (hints.isNotEmpty()) {
                    sb.appendLine("--- Optimization Hints ---")
                    for ((index, hint) in hints.withIndex().take(5)) {
                        sb.appendLine("${index + 1}. [${hint.severity}]")
                        sb.appendLine("   ${hint.message}")
                    }
                    sb.appendLine()
                }

                // Comparison
                if (comparisonPaths.isNotEmpty()) {
                    sb.appendLine("=== COMPARISON ===")
                    sb.appendLine()
                    sb.appendLine(String.format("%-15s %8s %8s %8s", "Path", "Length", "Time", "Effic%"))
                    sb.appendLine("-".repeat(45))
                    sb.appendLine(String.format("%-15s %8.1f %8.2f %7.1f%%",
                        pathData.name.take(15),
                        pathLength,
                        totalTime,
                        efficiency))

                    for (compPath in comparisonPaths) {
                        val compLength = compPath.getLength()
                        val compTime = compPath.getTotalTime()
                        val compStart = compPath.path.getPoint(0.0)
                        val compEnd = compPath.path.getPoint(1.0)
                        val compStraight = sqrt(
                            (compEnd.x - compStart.x).pow(2) +
                                    (compEnd.y - compStart.y).pow(2)
                        )
                        val compEfficiency = (compStraight / compLength * 100).coerceIn(0.0, 100.0)

                        sb.appendLine(String.format("%-15s %8.1f %8.2f %7.1f%%",
                            compPath.name.take(15),
                            compLength,
                            compTime,
                            compEfficiency))

                        val lengthDiff = compLength - pathLength
                        val timeDiff = compTime - totalTime
                        sb.appendLine(String.format("  Δ: %+15.1f %+8.2f", lengthDiff, timeDiff))
                    }
                }
            } ?: run {
                sb.appendLine("No path loaded.")
                sb.appendLine()
                sb.appendLine("Use the dropdown menu or")
                sb.appendLine("Paths > Load Path to begin.")
            }

            pathInfoArea.text = sb.toString()
        }
    }

    // DECODE 2025-2026 Field Elements
    inner class FieldElements2025 {
        fun draw(g2d: Graphics2D, panel: FieldPanel) {
            drawGoalsAndRamps(g2d, panel)
            drawBaseZones(g2d, panel)
            drawLoadingZones(g2d, panel)
            drawSecretTunnels(g2d, panel)
            drawLaunchLines(g2d, panel)
            drawSpikeMarks(g2d, panel)
        }

        private fun drawGoalsAndRamps(g2d: Graphics2D, panel: FieldPanel) {
            drawGoalWithRamp(g2d, panel, 0.0, FIELD_SIZE - TILE_SIZE, Color.RED, "RED")
            drawGoalWithRamp(g2d, panel, FIELD_SIZE - TILE_SIZE, FIELD_SIZE - TILE_SIZE, Color.BLUE, "BLUE")
        }

        private fun drawGoalWithRamp(g2d: Graphics2D, panel: FieldPanel, x: Double, y: Double,
                                     allianceColor: Color, label: String) {
            val rampWidth = 6.5
            val rampStartY = FIELD_SIZE
            val rampEndY = TILE_SIZE

            g2d.color = Color(allianceColor.red, allianceColor.green, allianceColor.blue, 100)
            val rampRect = if (x == 0.0) {
                Rectangle2D.Double(
                    panel.toScreenX(0.0),
                    panel.toScreenY(rampStartY),
                    rampWidth * panel.scale,
                    (rampStartY - rampEndY) * panel.scale
                )
            } else {
                Rectangle2D.Double(
                    panel.toScreenX(FIELD_SIZE - rampWidth),
                    panel.toScreenY(rampStartY),
                    rampWidth * panel.scale,
                    (rampStartY - rampEndY) * panel.scale
                )
            }
            g2d.fill(rampRect)
            g2d.color = allianceColor
            g2d.stroke = BasicStroke(2f)
            g2d.draw(rampRect)

            g2d.color = Color(allianceColor.red, allianceColor.green, allianceColor.blue, 150)
            val goalTriangle = Polygon()

            if (x == 0.0) {
                goalTriangle.addPoint(panel.toScreenX(rampWidth).toInt(), panel.toScreenY(FIELD_SIZE).toInt())
                goalTriangle.addPoint(panel.toScreenX(TILE_SIZE).toInt(), panel.toScreenY(FIELD_SIZE).toInt())
                goalTriangle.addPoint(panel.toScreenX(rampWidth).toInt(), panel.toScreenY(FIELD_SIZE - TILE_SIZE).toInt())
            } else {
                goalTriangle.addPoint(panel.toScreenX(FIELD_SIZE - rampWidth).toInt(), panel.toScreenY(FIELD_SIZE).toInt())
                goalTriangle.addPoint(panel.toScreenX(FIELD_SIZE - TILE_SIZE).toInt(), panel.toScreenY(FIELD_SIZE).toInt())
                goalTriangle.addPoint(panel.toScreenX(FIELD_SIZE - rampWidth).toInt(), panel.toScreenY(FIELD_SIZE - TILE_SIZE).toInt())
            }

            g2d.fill(goalTriangle)
            g2d.color = allianceColor
            g2d.stroke = BasicStroke(3f)
            g2d.draw(goalTriangle)

            g2d.color = Color.WHITE
            g2d.font = Font("Arial", Font.BOLD, 9)
            if (x == 0.0) {
                g2d.drawString("$label", panel.toScreenX(rampWidth + 2).toInt(), panel.toScreenY(FIELD_SIZE - 4).toInt())
            } else {
                g2d.drawString("$label", panel.toScreenX(FIELD_SIZE - rampWidth - 28).toInt(), panel.toScreenY(FIELD_SIZE - 4).toInt())
            }
        }

        private fun drawBaseZones(g2d: Graphics2D, panel: FieldPanel) {
            val zoneSize = 18.0

            g2d.color = Color(255, 0, 0, 100)
            var rect = Rectangle2D.Double(
                panel.toScreenX(2 * TILE_SIZE - zoneSize),
                panel.toScreenY(TILE_SIZE + zoneSize),
                zoneSize * panel.scale,
                zoneSize * panel.scale
            )
            g2d.fill(rect)
            g2d.color = Color.RED
            g2d.stroke = BasicStroke(2f)
            g2d.draw(rect)

            g2d.color = Color(0, 0, 255, 100)
            rect = Rectangle2D.Double(
                panel.toScreenX(4 * TILE_SIZE),
                panel.toScreenY(TILE_SIZE + zoneSize),
                zoneSize * panel.scale,
                zoneSize * panel.scale
            )
            g2d.fill(rect)
            g2d.color = Color.BLUE
            g2d.draw(rect)
        }

        private fun drawLoadingZones(g2d: Graphics2D, panel: FieldPanel) {
            g2d.color = Color(255, 255, 255, 80)
            g2d.stroke = BasicStroke(2f)

            var rect = Rectangle2D.Double(
                panel.toScreenX(0.0),
                panel.toScreenY(TILE_SIZE),
                TILE_SIZE * panel.scale,
                TILE_SIZE * panel.scale
            )
            g2d.fill(rect)
            g2d.color = Color.WHITE
            g2d.draw(rect)

            g2d.color = Color(255, 255, 255, 80)
            rect = Rectangle2D.Double(
                panel.toScreenX(5 * TILE_SIZE),
                panel.toScreenY(TILE_SIZE),
                TILE_SIZE * panel.scale,
                TILE_SIZE * panel.scale
            )
            g2d.fill(rect)
            g2d.color = Color.WHITE
            g2d.draw(rect)
        }

        private fun drawSecretTunnels(g2d: Graphics2D, panel: FieldPanel) {
            g2d.stroke = BasicStroke(2f)

            g2d.color = Color.RED
            g2d.drawLine(
                panel.toScreenX(6.5).toInt(),
                panel.toScreenY(TILE_SIZE).toInt(),
                panel.toScreenX(6.5).toInt(),
                panel.toScreenY(3 * TILE_SIZE).toInt()
            )

            g2d.color = Color.BLUE
            g2d.drawLine(
                panel.toScreenX(FIELD_SIZE - 6.5).toInt(),
                panel.toScreenY(TILE_SIZE).toInt(),
                panel.toScreenX(FIELD_SIZE - 6.5).toInt(),
                panel.toScreenY(3 * TILE_SIZE).toInt()
            )
        }

        private fun drawLaunchLines(g2d: Graphics2D, panel: FieldPanel) {
            g2d.color = Color.WHITE
            g2d.stroke = BasicStroke(2f)

            g2d.drawLine(
                panel.toScreenX(0.0).toInt(),
                panel.toScreenY(FIELD_SIZE).toInt(),
                panel.toScreenX(FIELD_SIZE / 2).toInt(),
                panel.toScreenY(FIELD_SIZE / 2).toInt()
            )
            g2d.drawLine(
                panel.toScreenX(FIELD_SIZE).toInt(),
                panel.toScreenY(FIELD_SIZE).toInt(),
                panel.toScreenX(FIELD_SIZE / 2).toInt(),
                panel.toScreenY(FIELD_SIZE / 2).toInt()
            )

            val frontCenterX = FIELD_SIZE / 2
            val frontCenterY = TILE_SIZE

            g2d.drawLine(
                panel.toScreenX(frontCenterX).toInt(),
                panel.toScreenY(frontCenterY).toInt(),
                panel.toScreenX(2 * TILE_SIZE).toInt(),
                panel.toScreenY(0.0).toInt()
            )

            g2d.drawLine(
                panel.toScreenX(frontCenterX).toInt(),
                panel.toScreenY(frontCenterY).toInt(),
                panel.toScreenX(4 * TILE_SIZE).toInt(),
                panel.toScreenY(0.0).toInt()
            )
        }

        private fun drawSpikeMarks(g2d: Graphics2D, panel: FieldPanel) {
            val artifactRadius = 2.5

            val spikeData = listOf(
                Triple(24.0, TILE_SIZE + TILE_SIZE / 2, "PPG"),
                Triple(24.0, 2 * TILE_SIZE + TILE_SIZE / 2, "PGP"),
                Triple(24.0, 3 * TILE_SIZE + TILE_SIZE / 2, "GPP"),
                Triple(120.0, TILE_SIZE + TILE_SIZE / 2, "PPG"),
                Triple(120.0, 2 * TILE_SIZE + TILE_SIZE / 2, "PGP"),
                Triple(120.0, 3 * TILE_SIZE + TILE_SIZE / 2, "GPP")
            )

            for ((seamX, centerY, pattern) in spikeData) {
                val colors = pattern.map {
                    if (it == 'G') Color(50, 200, 50) else Color(150, 50, 200)
                }

                val spacing = artifactRadius * 2.0
                val direction = if (seamX < FIELD_SIZE / 2) -1.0 else 1.0

                for (i in 0..2) {
                    val offset = when(i) {
                        0 -> 0.0
                        1 -> direction * spacing
                        2 -> -direction * spacing
                        else -> 0.0
                    }
                    val artifactX = seamX + offset

                    g2d.color = colors[i]
                    val circle = Ellipse2D.Double(
                        panel.toScreenX(artifactX - artifactRadius),
                        panel.toScreenY(centerY) - artifactRadius * panel.scale,
                        artifactRadius * 2 * panel.scale,
                        artifactRadius * 2 * panel.scale
                    )
                    g2d.fill(circle)
                    g2d.color = Color.BLACK
                    g2d.stroke = BasicStroke(1f)
                    g2d.draw(circle)
                }
            }
        }
    }

    // Optimization Analysis
    data class OptimizationHint(
        val position: Pose,
        val severity: Severity,
        val message: String
    ) {
        enum class Severity(val color: Color) {
            INFO(Color(100, 150, 255)),
            WARNING(Color.ORANGE),
            CRITICAL(Color.RED)
        }
    }

    private fun analyzePathOptimization(pathData: PathData): List<OptimizationHint> {
        val hints = mutableListOf<OptimizationHint>()
        val resolution = 50

        for (i in 0..resolution) {
            val t = i.toDouble() / resolution
            val pose = pathData.path.getPoint(t)

            if (pose.x < 9.0 || pose.x > 135.0 || pose.y < 9.0 || pose.y > 135.0) {
                hints.add(OptimizationHint(
                    pose,
                    OptimizationHint.Severity.CRITICAL,
                    "Path goes outside field boundary!"
                ))
            } else if (pose.x < 12.0 || pose.x > 132.0 || pose.y < 12.0 || pose.y > 132.0) {
                hints.add(OptimizationHint(
                    pose,
                    OptimizationHint.Severity.WARNING,
                    "Very close to wall. Risk of collision."
                ))
            }
        }

        val robotRadius = 9.0
        val goals = listOf(
            Triple(12.0, FIELD_SIZE - 12.0, "Red Goal"),
            Triple(FIELD_SIZE - 12.0, FIELD_SIZE - 12.0, "Blue Goal")
        )

        for (i in 0..resolution) {
            val t = i.toDouble() / resolution
            val pose = pathData.path.getPoint(t)

            for ((goalX, goalY, goalName) in goals) {
                val dx = pose.x - goalX
                val dy = pose.y - goalY
                val distance = sqrt(dx * dx + dy * dy)

                if (distance < 12.0 + robotRadius + 3.0) {
                    hints.add(OptimizationHint(
                        pose,
                        OptimizationHint.Severity.CRITICAL,
                        "Robot may collide with $goalName!"
                    ))
                }
            }
        }

        val startPose = pathData.path.getPoint(0.0)
        val endPose = pathData.path.getPoint(1.0)
        val dx = endPose.x - startPose.x
        val dy = endPose.y - startPose.y
        val straightLineDistance = sqrt(dx * dx + dy * dy)
        val pathLength = pathData.getLength()
        val efficiency = straightLineDistance / pathLength

        if (efficiency < 0.7) {
            hints.add(OptimizationHint(
                pathData.path.getPoint(0.5),
                OptimizationHint.Severity.INFO,
                "Path efficiency: ${String.format("%.1f", efficiency * 100)}%. Consider more direct route."
            ))
        }

        return hints.sortedBy { it.severity.ordinal }.reversed()
    }

    private fun createMenuBar(): JMenuBar {
        val menuBar = JMenuBar()

        val fileMenu = JMenu("File")
        val saveImage = JMenuItem("Save Field Image...")
        saveImage.addActionListener { saveFieldImage() }
        fileMenu.add(saveImage)
        menuBar.add(fileMenu)

        val pathsMenu = JMenu("Paths")
        val loadPath = JMenuItem("Load Path...")
        loadPath.addActionListener { showPathSelector() }
        pathsMenu.add(loadPath)

        val addComparison = JMenuItem("Add Comparison Path...")
        addComparison.addActionListener { addComparisonPath() }
        pathsMenu.add(addComparison)

        val clearComparison = JMenuItem("Clear All Comparisons")
        clearComparison.addActionListener {
            comparisonPaths.clear()
            fieldPanel.repaint()
            infoPanel.updateInfo()
        }
        pathsMenu.add(clearComparison)
        menuBar.add(pathsMenu)

        val viewMenu = JMenu("View")
        val toggleVelocity = JCheckBoxMenuItem("Show Velocity Vectors", showVelocityVectors)
        toggleVelocity.addActionListener {
            showVelocityVectors = toggleVelocity.isSelected
            fieldPanel.repaint()
        }
        viewMenu.add(toggleVelocity)

        val toggleField = JCheckBoxMenuItem("Show Field Elements", showFieldElements)
        toggleField.addActionListener {
            showFieldElements = toggleField.isSelected
            fieldPanel.repaint()
        }
        viewMenu.add(toggleField)

        val toggleTiming = JCheckBoxMenuItem("Show Timing Markers", showTiming)
        toggleTiming.addActionListener {
            showTiming = toggleTiming.isSelected
            fieldPanel.repaint()
        }
        viewMenu.add(toggleTiming)

        val toggleOptimization = JCheckBoxMenuItem("Show Optimization Hints", showOptimizationHints)
        toggleOptimization.addActionListener {
            showOptimizationHints = toggleOptimization.isSelected
            fieldPanel.repaint()
        }
        viewMenu.add(toggleOptimization)
        menuBar.add(viewMenu)

        return menuBar
    }

    private fun createControlPanel(): JPanel {
        val panel = JPanel()
        panel.layout = FlowLayout(FlowLayout.LEFT)
        panel.background = Color.DARK_GRAY

        // FIXED: Create combo box but don't populate it yet (will happen in init after subclass is ready)
        pathComboBox = JComboBox()
        pathComboBox.addActionListener {
            val selected = pathComboBox.selectedItem as? String
            selected?.let { loadPathByName(it) }
        }
        panel.add(JLabel("Primary Path:").apply { foreground = Color.WHITE })
        panel.add(pathComboBox)

        val playButton = JButton("▶").apply {
            addActionListener { toggleAnimation() }
        }
        panel.add(playButton)

        val resetButton = JButton("⏮").apply {
            addActionListener { resetAnimation() }
        }
        panel.add(resetButton)

        val speedSlider = JSlider(1, 20, 10)
        speedSlider.addChangeListener {
            animationSpeed = speedSlider.value / 10.0
        }
        panel.add(JLabel("Speed:").apply { foreground = Color.WHITE })
        panel.add(speedSlider)

        setupKeyboardShortcuts(panel)

        return panel
    }

    private fun setupKeyboardShortcuts(panel: JPanel) {
        val inputMap = panel.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
        val actionMap = panel.actionMap

        inputMap.put(KeyStroke.getKeyStroke("SPACE"), "playPause")
        actionMap.put("playPause", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) { toggleAnimation() }
        })

        inputMap.put(KeyStroke.getKeyStroke('R'), "reset")
        actionMap.put("reset", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) { resetAnimation() }
        })

        inputMap.put(KeyStroke.getKeyStroke('V'), "toggleV")
        actionMap.put("toggleV", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                showVelocityVectors = !showVelocityVectors
                fieldPanel.repaint()
            }
        })

        inputMap.put(KeyStroke.getKeyStroke('F'), "toggleF")
        actionMap.put("toggleF", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                showFieldElements = !showFieldElements
                fieldPanel.repaint()
            }
        })

        inputMap.put(KeyStroke.getKeyStroke('T'), "toggleT")
        actionMap.put("toggleT", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                showTiming = !showTiming
                fieldPanel.repaint()
            }
        })

        inputMap.put(KeyStroke.getKeyStroke('O'), "toggleO")
        actionMap.put("toggleO", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent?) {
                showOptimizationHints = !showOptimizationHints
                fieldPanel.repaint()
            }
        })
    }

    open fun getAvailablePaths(): Array<String> {
        return arrayOf(
            "Test: Simple Line",
            "Test: Square Path",
            "Test: Diagonal",
            "Auto: Sample Path"
        )
    }

    private fun loadPathByName(name: String) {
        if (loadedPaths.containsKey(name)) {
            primaryPath = loadedPaths[name]
        } else {
            val path = createPathFromName(name)
            path?.let {
                val color = Color.CYAN
                val pathData = PathData(name, it, color)
                loadedPaths[name] = pathData
                primaryPath = pathData
            }
        }
        resetAnimation()
        fieldPanel.repaint()
        infoPanel.updateInfo()
    }

    open fun createPathFromName(name: String): Path? {
        return when (name) {
            "Test: Simple Line" -> LinearPath(Pose(24.0, 24.0), Pose(120.0, 120.0))
            "Test: Square Path" -> {
                CompoundPath(listOf(
                    LinearPath(Pose(36.0, 36.0), Pose(108.0, 36.0)),
                    LinearPath(Pose(108.0, 36.0), Pose(108.0, 108.0)),
                    LinearPath(Pose(108.0, 108.0), Pose(36.0, 108.0)),
                    LinearPath(Pose(36.0, 108.0), Pose(36.0, 36.0))
                ))
            }
            "Test: Diagonal" -> LinearPath(Pose(12.0, 12.0), Pose(132.0, 132.0))
            "Auto: Sample Path" -> HermitePath.Builder()
                .addPoint(Pose(24.0, 24.0))
                .addPoint(Pose(72.0, 60.0))
                .addPoint(Pose(120.0, 96.0))
                .setTension(0.5)
                .build()
            else -> null
        }
    }

    private fun showPathSelector() {
        val selection = JOptionPane.showInputDialog(
            this,
            "Select path:",
            "Load Path",
            JOptionPane.PLAIN_MESSAGE,
            null,
            getAvailablePaths(),
            getAvailablePaths()[0]
        ) as? String

        selection?.let { loadPathByName(it) }
    }

    private fun addComparisonPath() {
        val pathName = JOptionPane.showInputDialog(
            this,
            "Select comparison path:",
            "Add Comparison",
            JOptionPane.PLAIN_MESSAGE,
            null,
            getAvailablePaths(),
            getAvailablePaths()[0]
        ) as? String ?: return

        val path = createPathFromName(pathName) ?: return

        val colors = listOf(Color.MAGENTA, Color.ORANGE, Color.GREEN, Color.YELLOW)
        val color = colors[comparisonPaths.size % colors.size]

        val pathData = PathData(pathName, path, color)
        comparisonPaths.add(pathData)

        fieldPanel.repaint()
        infoPanel.updateInfo()
    }

    private fun toggleAnimation() {
        isAnimating = !isAnimating
        if (isAnimating) {
            animationTimer.start()
        } else {
            animationTimer.stop()
        }
    }

    private fun resetAnimation() {
        animationProgress = 0.0
        isAnimating = false
        animationTimer.stop()
        fieldPanel.repaint()
    }

    private fun updateAnimation() {
        if (!isAnimating) return

        animationProgress += 0.005 * animationSpeed

        if (animationProgress >= 1.0) {
            animationProgress = 0.0
        }

        fieldPanel.repaint()
    }

    private fun saveFieldImage() {
        val fileChooser = JFileChooser()
        fileChooser.fileFilter = FileNameExtensionFilter("PNG Images", "png")
        fileChooser.selectedFile = File("field_visualization.png")

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            val file = fileChooser.selectedFile
            val image = java.awt.image.BufferedImage(
                fieldPanel.width,
                fieldPanel.height,
                java.awt.image.BufferedImage.TYPE_INT_RGB
            )
            val g = image.createGraphics()
            fieldPanel.paint(g)
            g.dispose()
            javax.imageio.ImageIO.write(image, "png", file)
            JOptionPane.showMessageDialog(this, "Image saved to ${file.name}")
        }
    }
}

fun main() {
    SwingUtilities.invokeLater {
        EnhancedPioneerPathVisualizer()
    }
}
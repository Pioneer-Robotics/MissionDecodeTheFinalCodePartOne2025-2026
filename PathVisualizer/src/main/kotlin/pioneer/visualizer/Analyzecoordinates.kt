package pioneer.visualizer

import pioneer.decode.Points
import pioneer.general.AllianceColor
import kotlin.math.abs

fun main() {
    println("=== ANALYZING YOUR COORDINATE SYSTEM ===\n")

    val red = Points(AllianceColor.RED)
    val blue = Points(AllianceColor.BLUE)

    println("RED ALLIANCE POSITIONS:")
    println("  START_GOAL: ${red.START_GOAL}")
    println("  START_FAR: ${red.START_FAR}")
    println("  SHOOT_GOAL_CLOSE: ${red.SHOOT_GOAL_CLOSE}")
    println("  SHOOT_GOAL_FAR: ${red.SHOOT_GOAL_FAR}")
    println("  COLLECT_GOAL: ${red.COLLECT_GOAL}")
    println("  COLLECT_MID: ${red.COLLECT_MID}")
    println("  COLLECT_AUDIENCE: ${red.COLLECT_AUDIENCE}")

    println("\nBLUE ALLIANCE POSITIONS:")
    println("  START_GOAL: ${blue.START_GOAL}")
    println("  START_FAR: ${blue.START_FAR}")
    println("  SHOOT_GOAL_CLOSE: ${blue.SHOOT_GOAL_CLOSE}")
    println("  SHOOT_GOAL_FAR: ${blue.SHOOT_GOAL_FAR}")

    // Find the actual range of your coordinates
    val allPoints = listOf(
        red.START_GOAL, red.START_FAR, red.SHOOT_GOAL_CLOSE, red.SHOOT_GOAL_FAR,
        red.COLLECT_GOAL, red.COLLECT_MID, red.COLLECT_AUDIENCE,
        blue.START_GOAL, blue.START_FAR, blue.SHOOT_GOAL_CLOSE, blue.SHOOT_GOAL_FAR
    )

    val minX = allPoints.minOf { it.x }
    val maxX = allPoints.maxOf { it.x }
    val minY = allPoints.minOf { it.y }
    val maxY = allPoints.maxOf { it.y }

    println("\n=== COORDINATE RANGE ===")
    println("X range: $minX to $maxX (span: ${maxX - minX})")
    println("Y range: $minY to $maxY (span: ${maxY - minY})")

    val centerX = (minX + maxX) / 2
    val centerY = (minY + maxY) / 2
    println("Apparent center: ($centerX, $centerY)")

    println("\n=== FIELD INTERPRETATION ===")

    // Check if coordinates are in centimeters
    val xSpanCm = maxX - minX
    val ySpanCm = maxY - minY
    println("If units are CENTIMETERS:")
    println("  Field size: ${xSpanCm}cm × ${ySpanCm}cm")
    println("  In inches: ${xSpanCm / 2.54} × ${ySpanCm / 2.54}")
    println("  Expected: 144\" × 144\" (365.76cm × 365.76cm)")

    // Suggest scaling factor
    val fieldSizeInches = 144.0
    val scaleX = fieldSizeInches / (maxX - minX)
    val scaleY = fieldSizeInches / (maxY - minY)

    println("\n=== SUGGESTED TRANSFORMATION ===")
    println("To convert to visualizer coordinates (0-144):")
    println("  Scale factor X: $scaleX")
    println("  Scale factor Y: $scaleY")
    println("  Translation X: ${-minX * scaleX}")
    println("  Translation Y: ${-minY * scaleY}")

    println("\nTransformation formula:")
    println("  viz_x = (robot_x - $minX) * $scaleX")
    println("  viz_y = (robot_y - $minY) * $scaleY")

    // Test the transformation
    println("\n=== TESTING TRANSFORMATION ===")
    fun transform(x: Double, y: Double): Pair<Double, Double> {
        return Pair(
            (x - minX) * scaleX,
            (y - minY) * scaleY
        )
    }

    println("RED START_GOAL ${red.START_GOAL.x}, ${red.START_GOAL.y} → ${transform(red.START_GOAL.x, red.START_GOAL.y)}")
    println("RED START_FAR ${red.START_FAR.x}, ${red.START_FAR.y} → ${transform(red.START_FAR.x, red.START_FAR.y)}")
    println("RED SHOOT_GOAL_CLOSE ${red.SHOOT_GOAL_CLOSE.x}, ${red.SHOOT_GOAL_CLOSE.y} → ${transform(red.SHOOT_GOAL_CLOSE.x, red.SHOOT_GOAL_CLOSE.y)}")

    println("\n=== FIELD LOCATIONS (BEST GUESS) ===")
    // Based on names, figure out where things should be
    println("START_GOAL should be: near base zone (B2 for red at ~39,33 or E2 for blue at ~105,33)")
    println("START_FAR should be: audience side, far from goals (maybe loading zone A1 or F1)")
    println("SHOOT_GOAL_CLOSE should be: position to shoot into nearby basket")
    println("SHOOT_GOAL_FAR should be: position to shoot from far away")
    println("COLLECT_GOAL/MID/AUDIENCE: positions to collect artifacts from spike marks at y=36/60/84")
}
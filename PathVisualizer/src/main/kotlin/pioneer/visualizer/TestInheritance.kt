package pioneer.visualizer

import pioneer.helpers.Pose
import pioneer.pathing.paths.*
import pioneer.decode.Points
import pioneer.general.AllianceColor
import javax.swing.*

open class TestBase : JFrame() {
    private lateinit var combo: JComboBox<String>

    init {
        println("TestBase: init starting")

        combo = JComboBox()
        combo.addItem("Should not see this")

        add(combo)

        setSize(400, 100)
        setLocationRelativeTo(null)
        defaultCloseOperation = EXIT_ON_CLOSE
        isVisible = true

        println("TestBase: calling populatePaths in SwingUtilities.invokeLater")
        SwingUtilities.invokeLater {
            println("TestBase: invokeLater executing")
            populatePaths()
        }

        println("TestBase: init complete")
    }

    private fun populatePaths() {
        println("TestBase.populatePaths() called")
        combo.removeAllItems()
        val paths = getPaths()
        println("TestBase: got ${paths.size} paths from getPaths()")
        for (p in paths) {
            println("  Adding: $p")
            combo.addItem(p)
        }
    }

    open fun getPaths(): Array<String> {
        println("TestBase.getPaths() called - returning base paths")
        return arrayOf("Base Path 1", "Base Path 2")
    }
}

class TestSub : TestBase() {
    init {
        println("TestSub: init starting")
        title = "Test Subclass"
        println("TestSub: init complete")
    }

    override fun getPaths(): Array<String> {
        println("TestSub.getPaths() called - returning CUSTOM paths")
        return arrayOf("CUSTOM PATH A", "CUSTOM PATH B", "CUSTOM PATH C")
    }
}

fun main() {
    println("=== STARTING TEST ===\n")
    SwingUtilities.invokeLater {
        TestSub()
    }
    println("\n=== MAIN COMPLETE ===")
}
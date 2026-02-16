package pioneer.hardware

import com.qualcomm.robotcore.hardware.HardwareMap
import pioneer.Constants
import org.firstinspires.ftc.teamcode.prism.Color
import org.firstinspires.ftc.teamcode.prism.GoBildaPrismDriver
import org.firstinspires.ftc.teamcode.prism.PrismAnimations
import pioneer.decode.Artifact

class LED(
    private val hardwareMap: HardwareMap,
    private val driverName: String = Constants.HardwareNames.LED_DRIVER,
) : HardwareComponent {
    private data class SolidState(
        val color: Color,
        val start: Int,
        val end: Int,
        val brightness: Int
    )

    private val layerCache =
        mutableMapOf<GoBildaPrismDriver.LayerHeight, SolidState>()

    private lateinit var driver: GoBildaPrismDriver

    override fun init() {
        driver = hardwareMap.get(
            GoBildaPrismDriver::class.java,
            driverName
        )
    }

    // Only update prism if the state changes
    private fun setSolidIfChanged(
        layer: GoBildaPrismDriver.LayerHeight,
        color: Color,
        start: Int,
        end: Int,
        brightness: Int
    ) {
        val newState = SolidState(color, start, end, brightness)
        val oldState = layerCache[layer]

        if (newState == oldState) return

        layerCache[layer] = newState

        driver.insertAndUpdateAnimation(
            layer,
            PrismAnimations.Solid(color).apply {
                this.startIndex = start
                this.stopIndex = end
                this.brightness = brightness
            }
        )
    }

    fun setColor(color: Color, start: Int = 0, end: Int = 11, brightness: Int = 100) {
        setSolidIfChanged(
            GoBildaPrismDriver.LayerHeight.LAYER_0,
            color,
            start,
            end,
            brightness
        )
    }

    fun displayArtifacts(
        artifacts: List<Artifact?>,
        brightness: Int = 100
    ) {
        require(artifacts.size == 3)

        val artifactColors = mapOf(
            Artifact.PURPLE to Color.PURPLE,
            Artifact.GREEN to Color.GREEN,
            null to Color(0,0,0)
        )

        val layers = listOf(
            GoBildaPrismDriver.LayerHeight.LAYER_1,
            GoBildaPrismDriver.LayerHeight.LAYER_2,
            GoBildaPrismDriver.LayerHeight.LAYER_3
        )

        val segments = listOf(
            12..15,
            16..19,
            20..23
        )

        artifacts
            .map { artifactColors.getValue(it) }
            .zip(layers)
            .zip(segments)
            .forEach { (colorAndLayer, segment) ->
                val (color, layer) = colorAndLayer

                setSolidIfChanged(
                    layer,
                    color,
                    segment.first,
                    segment.last,
                    brightness
                )
            }
    }

    fun setAnimation(animation: PrismAnimations.AnimationBase) {
        driver.insertAndUpdateAnimation(
            GoBildaPrismDriver.LayerHeight.LAYER_0,
            animation,
        )
    }

    fun clear() {
        layerCache.clear()
        driver.clearAllAnimations()
    }
}

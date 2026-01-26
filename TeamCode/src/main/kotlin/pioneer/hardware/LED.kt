package pioneer.hardware

import com.qualcomm.robotcore.hardware.HardwareMap
import pioneer.Constants
import pioneer.hardware.prism.Color
import pioneer.hardware.prism.GoBildaPrismDriver
import pioneer.hardware.prism.PrismAnimations

class LED(
    private val hardwareMap: HardwareMap,
    private val driverName: String = Constants.HardwareNames.LED_DRIVER,
) : HardwareComponent {
    private lateinit var driver: GoBildaPrismDriver

    override fun init() {
        driver = hardwareMap.get(
            GoBildaPrismDriver::class.java,
            driverName
        )
    }

    fun setColor(color: Color, brightness: Int = 100) {
        driver.insertAndUpdateAnimation(
            GoBildaPrismDriver.LayerHeight.LAYER_0,
            PrismAnimations.Solid(color).apply{
                this.brightness = brightness
            }
        )
    }

    fun setAnimation(animation: PrismAnimations.AnimationBase) {
        driver.insertAndUpdateAnimation(
            GoBildaPrismDriver.LayerHeight.LAYER_0,
            animation,
        )
    }

    fun clear() {
        driver.clearAllAnimations()
    }
}
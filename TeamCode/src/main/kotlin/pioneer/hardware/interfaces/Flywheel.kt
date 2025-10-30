package pioneer.hardware.interfaces


interface Flywheel {
    val velocity: Double
    fun setSpeed(velocity: Double)
}

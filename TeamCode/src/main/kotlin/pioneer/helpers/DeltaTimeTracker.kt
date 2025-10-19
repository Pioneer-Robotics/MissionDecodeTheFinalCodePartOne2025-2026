package pioneer.helpers

import com.qualcomm.robotcore.util.ElapsedTime

class DeltaTimeTracker {
    private val timer: ElapsedTime = ElapsedTime()
    private var lastTime = timer.milliseconds()
    var dt = 0.0 // in milliseconds
        private set

    fun update() {
        val current = timer.milliseconds()
        dt = (current - lastTime)
        lastTime = current
    }
}

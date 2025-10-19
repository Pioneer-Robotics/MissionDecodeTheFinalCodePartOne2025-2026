package pioneer

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.robotcore.external.Telemetry
import pioneer.hardware.drivebase.MecanumBase
import pioneer.hardware.BatteryMonitor
import pioneer.helpers.DashboardPlotter
import pioneer.helpers.FileLogger
import pioneer.localization.Localizer
import pioneer.helpers.Pose
import pioneer.localization.localizers.Pinpoint
import pioneer.pathing.follower.Follower

class Bot {
    companion object {
        private val timer: ElapsedTime = ElapsedTime()
        private var prevTime: Double = timer.milliseconds()

        var telemetryPacket: TelemetryPacket = TelemetryPacket(false)

        lateinit var telemetry: Telemetry
            private set // Prevent external modification

        var dt: Double = 0.0 // Delta time in milliseconds
            private set

        lateinit var mecanumBase: MecanumBase
            private set

        lateinit var follower: Follower
            private set

        lateinit var localizer: Localizer

        lateinit var batteryMonitor: BatteryMonitor
            private set

        fun initialize(hardwareMap: HardwareMap, telemetry: Telemetry, startPose: Pose = Pose()) {
            this.telemetry = telemetry
            mecanumBase = MecanumBase(hardwareMap)
            localizer = Pinpoint(hardwareMap, startPose)
            follower = Follower()
            batteryMonitor = BatteryMonitor(hardwareMap)
        }

        /** Updates the bot's systems. Call this before every loop.  */
        fun update() {
            // Update delta time
            dt = timer.milliseconds() - prevTime
            prevTime = timer.milliseconds()

            // Update localizer
            localizer.update(dt / 1000.0) // Convert milliseconds to seconds

            // Update follower
            if (follower.path != null) follower.update()
        }

        fun sendTelemetryPacket() {
            FtcDashboard.getInstance().sendTelemetryPacket(telemetryPacket)
            telemetryPacket = TelemetryPacket() // Reset the packet for the next loop
        }

        fun stop() {
            mecanumBase.stop()
            follower.path = null // Clear the path
            FileLogger.flush() // Save any pending logs
            DashboardPlotter.clearPreviousPositions()
        }
    }
}

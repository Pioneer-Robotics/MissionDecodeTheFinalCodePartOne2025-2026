package org.firstinspires.ftc.teamcode

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.teamcode.hardware.drivebase.MecanumBase
import org.firstinspires.ftc.teamcode.hardware.VoltageHandler
import org.firstinspires.ftc.teamcode.helpers.DashboardPlotter
import org.firstinspires.ftc.teamcode.helpers.FileLogger
import org.firstinspires.ftc.teamcode.localization.Localizer
import org.firstinspires.ftc.teamcode.localization.Pose
import org.firstinspires.ftc.teamcode.localization.localizers.Pinpoint
import org.firstinspires.ftc.teamcode.pathing.follower.Follower

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

        lateinit var voltageHandler: VoltageHandler
            private set

        fun initialize(hardwareMap: HardwareMap, telemetry: Telemetry, startPose: Pose = Pose()) {
            this.telemetry = telemetry
            mecanumBase = MecanumBase(hardwareMap)
            localizer = Pinpoint(hardwareMap, startPose)
            follower = Follower()
            voltageHandler = VoltageHandler(hardwareMap)
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

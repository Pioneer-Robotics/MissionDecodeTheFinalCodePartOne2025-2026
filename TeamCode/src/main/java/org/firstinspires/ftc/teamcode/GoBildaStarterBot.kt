package org.firstinspires.ftc.teamcode

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.teamcode.hardware.CV.AprilTag
import org.firstinspires.ftc.teamcode.hardware.drivebase.MecanumBase
import org.firstinspires.ftc.teamcode.hardware.VoltageHandler
import org.firstinspires.ftc.teamcode.helpers.DashboardPlotter
import org.firstinspires.ftc.teamcode.helpers.FileLogger
import org.firstinspires.ftc.teamcode.localization.Pose
import org.firstinspires.ftc.teamcode.hardware.Flywheel
import org.firstinspires.ftc.teamcode.hardware.LaunchServos

class GoBildaStarterBot {
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

        lateinit var voltageHandler: VoltageHandler
            private set

        lateinit var flywheel: Flywheel
            private set

        lateinit var launchServos: LaunchServos
            private set

        lateinit var aprilTagProcessor: AprilTag
            private set

        fun initialize(hardwareMap: HardwareMap, telemetry: Telemetry, startPose: Pose = Pose()) {
            this.telemetry = telemetry
            mecanumBase = MecanumBase(hardwareMap)
            voltageHandler = VoltageHandler(hardwareMap)
            flywheel = Flywheel(hardwareMap)
            launchServos = LaunchServos(hardwareMap)
            aprilTagProcessor = AprilTag(hardwareMap)
        }

        /** Updates the bot's systems. Call this before every loop.  */
        fun update() {
            // Update delta time
            dt = timer.milliseconds() - prevTime
            prevTime = timer.milliseconds()
        }

        fun sendTelemetryPacket() {
            FtcDashboard.getInstance().sendTelemetryPacket(telemetryPacket)
            telemetryPacket = TelemetryPacket() // Reset the packet for the next loop
        }

        fun stop() {
            mecanumBase.stop()
            FileLogger.flush() // Save any pending logs
            DashboardPlotter.clearPreviousPositions()
        }
    }
}

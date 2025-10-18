package org.firstinspires.ftc.teamcode

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.telemetry.TelemetryPacket
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.robotcore.external.Telemetry
import org.firstinspires.ftc.teamcode.hardware.CV.AprilTag
import org.firstinspires.ftc.teamcode.hardware.implementations.FlywheelImpl
import org.firstinspires.ftc.teamcode.hardware.LaunchServos
import org.firstinspires.ftc.teamcode.hardware.VoltageHandler
import org.firstinspires.ftc.teamcode.hardware.drivebase.MecanumBase
import org.firstinspires.ftc.teamcode.helpers.DashboardPlotter
import org.firstinspires.ftc.teamcode.helpers.FileLogger
import org.firstinspires.ftc.teamcode.localization.Pose
import org.firstinspires.ftc.teamcode.localization.localizers.Pinpoint
import org.firstinspires.ftc.teamcode.pathing.follower.Follower

class GoBildaStarterBot(val hardwareMap: HardwareMap, val telemetry: Telemetry, startPose: Pose = Pose()) {
    private val timer: ElapsedTime = ElapsedTime()
    private var prevTime: Double = timer.milliseconds()

    var dt: Double = 0.0 // Delta time in milliseconds
        private set

    var telemetryPacket: TelemetryPacket = TelemetryPacket(false)

    val mecanumBase = MecanumBase(hardwareMap)
    val localizer = Pinpoint(hardwareMap, startPose)
    val follower = Follower(this)
    val voltageHandler = VoltageHandler(hardwareMap)
    val flywheel = FlywheelImpl(hardwareMap)
    val launchServos = LaunchServos(hardwareMap)
    val aprilTagProcessor = AprilTag(hardwareMap)

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

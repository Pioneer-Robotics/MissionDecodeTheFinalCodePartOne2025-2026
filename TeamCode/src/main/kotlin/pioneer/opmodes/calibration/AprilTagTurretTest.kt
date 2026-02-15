package pioneer.opmodes.calibration

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.eventloop.opmode.OpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import pioneer.Constants
import pioneer.hardware.Camera
import pioneer.helpers.Chrono
import pioneer.helpers.PIDController
import pioneer.vision.AprilTag
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sign

@Config
object TurretPIDConstants {
    @JvmField var KP: Double = 0.0
    @JvmField var KI: Double = 0.0
    @JvmField var KD: Double = 0.0
    @JvmField var KS: Double = 0.125
}

@TeleOp(name = "April Tag Turret Test", group = "Testing")
class AprilTagTurretTest : OpMode() {
    lateinit var motor: DcMotorEx
    lateinit var camera: Camera
    lateinit var dashboard: FtcDashboard

    var pid = PIDController(
        TurretPIDConstants.KP,
        TurretPIDConstants.KI,
        TurretPIDConstants.KD
    )
    val chrono = Chrono()

    val ticksPerRadian = Constants.Turret.TICKS_PER_REV / (2 * PI)
    val currentAngle: Double get() = motor.currentPosition / ticksPerRadian

    override fun init() {
        motor = hardwareMap.get(DcMotorEx::class.java, Constants.HardwareNames.TURRET_MOTOR)
        camera = Camera(hardwareMap, processors = arrayOf(AprilTag().processor)).apply { init() }
        dashboard = FtcDashboard.getInstance()
    }

    override fun loop() {
        val detections = camera.getProcessor<AprilTagProcessor>()?.detections
        val targetTag = detections?.find { it.id == 21 } // Only track id 21
        val tagError = targetTag?.ftcPose?.bearing?.let { it * -1 } ?: 0.0

        motor.targetPosition = ((currentAngle + tagError) * ticksPerRadian).toInt()
        motor.mode = DcMotor.RunMode.RUN_TO_POSITION

        dashboard.telemetry.addData("April Tag Error", tagError)
        dashboard.telemetry.addData("Current Turret Angle", currentAngle)
        dashboard.telemetry.addData("Target Turret Angle", tagError)
        dashboard.telemetry.update()

//        if (gamepad1.touchpad) updatePIDConstants()
    }

    private fun updatePIDConstants() {
        pid = PIDController(
            TurretPIDConstants.KP,
            TurretPIDConstants.KI,
            TurretPIDConstants.KD
        )
        pid.integralClamp = 0.5 / TurretPIDConstants.KI
    }
}

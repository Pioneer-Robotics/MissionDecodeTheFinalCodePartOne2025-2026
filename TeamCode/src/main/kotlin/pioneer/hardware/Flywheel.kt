package pioneer.hardware

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.PIDFCoefficients
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import pioneer.Constants
import pioneer.decode.GoalTag
import pioneer.helpers.FileLogger
import pioneer.helpers.Pose
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class Flywheel(
    private val hardwareMap: HardwareMap,
    private val motorName: String = Constants.HardwareNames.FLYWHEEL,
) : HardwareComponent {
    private lateinit var flywheel: DcMotorEx

    val motor: DcMotorEx
        get() = flywheel

    var velocity
        get() = flywheel.velocity
        set(value) {
            flywheel.velocity = value
        }

    val current
        get() = flywheel.getCurrent(CurrentUnit.MILLIAMPS)

    override fun init() {
        flywheel =
            hardwareMap.get(DcMotorEx::class.java, motorName).apply {
                mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
                mode = DcMotor.RunMode.RUN_USING_ENCODER
                zeroPowerBehavior = DcMotor.ZeroPowerBehavior.FLOAT
                direction = DcMotorSimple.Direction.FORWARD
            }
        FileLogger.info(name, flywheel.getPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER).toString())
        flywheel.setPIDFCoefficients(
            DcMotor.RunMode.RUN_USING_ENCODER,
            PIDFCoefficients(
                50.0,
                3.0,
                0.0,
                0.0,
            ),
        )
    }

    // https://www.desmos.com/calculator/uofqeqqyn1
    fun estimateVelocity(
        target: Pose,
        pose: Pose,
    ): Double {
        val shootPose = pose + Pose(pose.vx * Constants.Turret.LAUNCH_TIME, pose.vy * Constants.Turret.LAUNCH_TIME)
        val heightDiff = GoalTag.BLUE.shootingHeight - Constants.Turret.HEIGHT
        val groundDistance = shootPose distanceTo target
        val v0 =
            (groundDistance) / (
                cos(
                    Constants.Turret.THETA,
                ) * sqrt((2.0 * (heightDiff - tan(Constants.Turret.THETA) * (groundDistance))) / (-980))
            )

        val flywheelVelocity = 1.58901 * v0 + 17.2812
        val thetaToTarget = -(shootPose angleTo target)
        val newTargetVelocityX = sin(thetaToTarget) * flywheelVelocity - pose.vx
        val newTargetVelocityY = cos(thetaToTarget) * flywheelVelocity - pose.vy
        val newTargetVelocity = sqrt(newTargetVelocityX.pow(2) + newTargetVelocityY.pow(2))
        return newTargetVelocity
    }
}

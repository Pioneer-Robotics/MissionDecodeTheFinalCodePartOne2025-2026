package pioneer.hardware

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import pioneer.Constants
import pioneer.helpers.PIDController
import pioneer.helpers.Pose
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tan

class Flywheel(
    private val hardwareMap: HardwareMap,
    private val motorName: String = Constants.HardwareNames.FLYWHEEL,
) : HardwareComponent {
    private lateinit var flywheel: DcMotorEx
    private val motorPID = PIDController(
        Constants.Flywheel.KP,
        Constants.Flywheel.KI,
        Constants.Flywheel.KD,
    )

    val motor: DcMotorEx
        get() = flywheel

    var targetVelocity = 0.0

    var velocity
        get() = flywheel.velocity
        set(value) {
            targetVelocity = value
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
    }

    override fun update(dt: Double) {
        if (targetVelocity == 0.0) {
            flywheel.power = 0.0
            return
        }
        val correction = motorPID.update(targetVelocity - velocity, dt)
        flywheel.power = Constants.Flywheel.KF * targetVelocity + correction
//        FileLogger.debug("Flywheel","Set Power: ${Constants.Flywheel.KF * targetVelocity + correction}")
    }

    // https://www.desmos.com/calculator/uofqeqqyn1
    //12/22: https://www.desmos.com/calculator/1kj8xzklqp
    fun estimateVelocity(
        pose: Pose,
        target: Pose,
        targetHeight: Double
    ): Double {

        val shootPose = pose +
                Pose(x = Constants.Turret.OFFSET * sin(-pose.theta), y = Constants.Turret.OFFSET * cos(-pose.theta)) +
                Pose(pose.vx * Constants.Turret.LAUNCH_TIME, pose.vy * Constants.Turret.LAUNCH_TIME)

        val heightDiff = targetHeight - Constants.Turret.HEIGHT
        //TODO Double check AprilTag height
        val groundDistance = shootPose distanceTo target
        //Real world v0 of the ball
        val v0 =
            (groundDistance) / (
                cos(
                    Constants.Turret.THETA,
                ) * sqrt((2.0 * (heightDiff - tan(Constants.Turret.THETA) * (groundDistance))) / (-980))
            )
        //Regression to convert real world velocity to flywheel speed
        val flywheelVelocity = 1.583 * v0 - 9.86811 //From 12/22 testing

        //Adjust for velocity of the bot when moving
//        val thetaToTarget = -(shootPose angleTo target)
//        val newTargetVelocityX = sin(thetaToTarget) * flywheelVelocity - pose.vx
//        val newTargetVelocityY = cos(thetaToTarget) * flywheelVelocity - pose.vy
//        val newTargetVelocity = sqrt(newTargetVelocityX.pow(2) + newTargetVelocityY.pow(2))

        val newTargetVelocity = flywheelVelocity
        return newTargetVelocity
    }
}

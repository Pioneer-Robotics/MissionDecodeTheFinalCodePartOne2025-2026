package pioneer.hardware

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import pioneer.Constants
import pioneer.helpers.Chrono
import pioneer.helpers.FileLogger
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

    private val chrono = Chrono()

    val motor: DcMotorEx
        get() = flywheel

    var targetVelocity = 0.0

    var velocity
        get() = flywheel.velocity
        set(value) {
            targetVelocity = value
        }

    var idleVelocity: Double = 0.0
        get() = when (operatingMode){
            Constants.Flywheel.FlywheelOperatingMode.ALWAYS_IDLE -> Constants.Flywheel.idleVelocity
            Constants.Flywheel.FlywheelOperatingMode.FULL_OFF -> 0.0
//            Constants.Flywheel.FlywheelOperatingMode.TIMED_IDLE -> {
//                if (idleTrue) Constants.Flywheel.idleVelocity else 0.0
//            }
    }

    val current
        get() = flywheel.getCurrent(CurrentUnit.MILLIAMPS)


    var operatingMode = Constants.Flywheel.FlywheelOperatingMode.FULL_OFF

    override fun init() {
        flywheel =
            hardwareMap.get(DcMotorEx::class.java, motorName).apply {
                mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
                mode = DcMotor.RunMode.RUN_USING_ENCODER
                zeroPowerBehavior = DcMotor.ZeroPowerBehavior.FLOAT
                direction = DcMotorSimple.Direction.FORWARD
            }
    }

    override fun update() {
        if (targetVelocity == 0.0) {
            flywheel.power = 0.0
            return
        }
        val correction = motorPID.update(targetVelocity - velocity, chrono.dt)
        flywheel.power = (Constants.Flywheel.KF * targetVelocity + correction).coerceIn(-1.0, 1.0)
    }

    // https://www.desmos.com/calculator/uofqeqqyn1
    //12/22: https://www.desmos.com/calculator/1kj8xzklqp
    // 2/4: https://www.desmos.com/calculator/uyk9a9vs5s
    // 2/9:
    fun estimateVelocity(
        pose: Pose,
        target: Pose,
        targetHeight: Double
    ): Double {
        val shootPose = pose +
                Pose(x = Constants.Turret.OFFSET * sin(-pose.theta), y = Constants.Turret.OFFSET * cos(-pose.theta)) +
                Pose(pose.vx * Constants.Turret.LAUNCH_TIME, pose.vy * Constants.Turret.LAUNCH_TIME)

        //TODO Double check AprilTag height
        val groundDistance = shootPose distanceTo target
        return estimateVelocity(groundDistance, targetHeight)
    }

    fun estimateVelocity(targetDistance: Double, targetHeight: Double) : Double {
        val heightDiff = targetHeight - Constants.Turret.HEIGHT
        //Real world v0 of the ball
        val v0 =
            (targetDistance) / (
                    cos(
                        Constants.Turret.THETA,
                    ) * sqrt((2.0 * (heightDiff - tan(Constants.Turret.THETA) * (targetDistance))) / (-980))
                )
        //Regression to convert real world velocity to flywheel speed
//        val flywheelVelocity = 1.583 * v0 - 9.86811 // From 12/22 testing
//        val flywheelVelocity = 1.64545 * v0 - 51.56276 // From 2/4 testing
        val flywheelVelocity = 2.05204 * v0 - 290.74829 // From 2/9 testing
        //Adjust for velocity of the bot when moving
//        val thetaToTarget = -(shootPose angleTo target)
//        val newTargetVelocityX = sin(thetaToTarget) * flywheelVelocity - pose.vx
//        val newTargetVelocityY = cos(thetaToTarget) * flywheelVelocity - pose.vy
//        val newTargetVelocity = sqrt(newTargetVelocityX.pow(2) + newTargetVelocityY.pow(2))

        val newTargetVelocity = flywheelVelocity
        return newTargetVelocity
    }
}

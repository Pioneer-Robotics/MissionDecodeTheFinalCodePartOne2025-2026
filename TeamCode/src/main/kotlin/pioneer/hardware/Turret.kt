package pioneer.hardware

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import pioneer.constants.HardwareNames
import pioneer.constants.Turret
import pioneer.helpers.MathUtils
import kotlin.math.PI

class Turret(
    private val hardwareMap: HardwareMap,
    private val motorName: String = HardwareNames.TURRET,
    private val motorRange: Pair<Double, Double> = -PI to PI,
) : HardwareComponent {

    override val name = "Turret"

    private lateinit var turret: DcMotorEx

    private val ticksPerRadian: Double = Turret.TICKS_PER_REV / (2 * PI)

    init {
        require(motorRange.first < motorRange.second) {
            "Motor range must be valid: ${motorRange.first} to ${motorRange.second}"
        }
    }

    override fun init() {
        turret =
            hardwareMap.get(DcMotorEx::class.java, motorName).apply {
                mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
                zeroPowerBehavior = DcMotor.ZeroPowerBehavior.FLOAT
                direction = DcMotorSimple.Direction.FORWARD
            }
    }

    val currentAngle: Double
        get() = turret.currentPosition / ticksPerRadian

    val targetAngle: Double
        get() = turret.targetPosition / ticksPerRadian

    fun gotoAngle(
        angle: Double,
        power: Double = 0.5,
    ) {
        require(power in -1.0..1.0)
        check(::turret.isInitialized)

        val desiredAngle = MathUtils
            .normalizeRadians(angle)
            .coerceIn(motorRange.first, motorRange.second)

        val currentTicks = turret.currentPosition
        val currentAngle = currentTicks / ticksPerRadian

        val delta = desiredAngle - currentAngle
        val targetTicks = (currentTicks + delta * ticksPerRadian).toInt()

        with(turret) {
            targetPosition = targetTicks
            mode = DcMotor.RunMode.RUN_TO_POSITION
            this.power = power
        }
    }
}
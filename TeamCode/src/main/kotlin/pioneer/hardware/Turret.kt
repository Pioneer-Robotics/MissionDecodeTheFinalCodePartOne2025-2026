package pioneer.hardware

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.HardwareMap
import pioneer.constants.*
import java.lang.Math.PI

class Turret(
    private val hardwareMap: HardwareMap,
    private val motorName: String = HardwareNames.TURRET
) : HardwareComponent {

    override val name = "Turret"

    private lateinit var turret: DcMotorEx

    override fun init() {
        turret = hardwareMap.get(DcMotorEx::class.java, motorName)
        turret.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        turret.zeroPowerBehavior = DcMotor.ZeroPowerBehavior.FLOAT
        turret.direction = DcMotorSimple.Direction.FORWARD
    }

    fun gotoAngle(angle: Double, power: Double) {
        turret.targetPosition = (angle * OtherConst.TURRET_TICKS_PER_REV / (2*PI)).toInt()
        turret.mode = DcMotor.RunMode.RUN_TO_POSITION
        turret.power = power
    }
}

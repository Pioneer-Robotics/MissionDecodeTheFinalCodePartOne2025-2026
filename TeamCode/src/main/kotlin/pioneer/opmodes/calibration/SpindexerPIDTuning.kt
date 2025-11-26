//package pioneer.opmodes.calibration
//
//import com.qualcomm.robotcore.eventloop.opmode.OpMode
//import com.qualcomm.robotcore.eventloop.opmode.TeleOp
//import pioneer.Constants
//import pioneer.hardware.Spindexer
//import pioneer.helpers.Toggle
//import pioneer.helpers.next
//
//@TeleOp(name="Spindexer PID Tuner", group = "Calibration")
//class SpindexerPIDTuning : OpMode() {
//    lateinit var spindexer: Spindexer
//
//    val changePositionToggle = Toggle(false)
//    val applyPositionToggle = Toggle(false)
//
//    var targetPosition = Spindexer.MotorPosition.INTAKE_1
//
//    override fun init() {
//        spindexer = Spindexer(
//            hardwareMap = hardwareMap,
//            motorName = "spindexerMotor",
//            intakeSensorName = "intakeSensor",
//            outtakeSensorName = "outtakeSensor",
//        ).apply { init() }
//    }
//
//    override fun loop() {
//        changePositionToggle.toggle(gamepad1.circle)
//        if (changePositionToggle.justChanged) {
//            targetPosition = targetPosition.next()
//        }
//
//        applyPositionToggle.toggle(gamepad1.cross)
//        if (applyPositionToggle.justChanged) {
//            spindexer.motorState = targetPosition
//        }
//
//        spindexer.update()
//
//        telemetry.addData("Spindexer Target", targetPosition)
//        telemetry.addData("Spindexer Motor Ticks", spindexer.motorCurrentTicks)
//        telemetry.update()
//    }
//}

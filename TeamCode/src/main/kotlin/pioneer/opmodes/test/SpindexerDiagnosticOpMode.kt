package pioneer.opmodes.test

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import pioneer.Bot
import pioneer.hardware.spindexer.SpindexerMotionController
import pioneer.opmodes.BaseOpMode

/**
 * Spindexer Diagnostic OpMode
 * 
 * Use this to test and tune your spindexer PID gains.
 * 
 * CONTROLS:
 * - DPAD UP/DOWN: Cycle through positions manually
 * - X: Move to INTAKE_1
 * - B: Move to OUTTAKE_1  
 * - Y: Move to INTAKE_2
 * - A: Move to OUTTAKE_2
 * - LEFT BUMPER: Move to INTAKE_3
 * - RIGHT BUMPER: Move to OUTTAKE_3
 * - LEFT TRIGGER: Manual reverse (hold)
 * - RIGHT TRIGGER: Manual forward (hold)
 * - START: Calibrate encoder to current position
 * - BACK: Reset to INTAKE_1
 */
@TeleOp(name = "Spindexer Diagnostic", group = "Test")
class SpindexerDiagnosticOpMode : BaseOpMode() {
    
    private lateinit var bot: Bot
    private var lastPosition: SpindexerMotionController.MotorPosition? = null
    private var manualMode = false
    
    override fun onInit() {
        bot = Bot(hardwareMap, this)
        bot.initSpindexer()
        
        telemetry.addLine("Spindexer Diagnostic Ready!")
        telemetry.addLine("Use buttons to move to positions")
        telemetry.addLine("Triggers for manual control")
        telemetry.update()
    }
    
    override fun onStart() {
        bot.spindexer?.moveToPosition(SpindexerMotionController.MotorPosition.INTAKE_1)
    }
    
    override fun onLoop() {
        // Manual position selection
        handlePositionSelection()
        
        // Manual control
        handleManualControl()
        
        // Calibration
        if (gamepad1.start) {
            bot.spindexer?.resetMotorPosition(0)
            telemetry.addLine("✓ Encoder calibrated!")
        }
        
        // Reset
        if (gamepad1.back) {
            bot.spindexer?.moveToPosition(SpindexerMotionController.MotorPosition.INTAKE_1)
            bot.spindexer?.reset()
        }
        
        // Update
        bot.updateAll()
        
        // Display telemetry
        displayTelemetry()
    }
    
    private fun handlePositionSelection() {
        val spindexer = bot.spindexer ?: return
        
        when {
            gamepad1.cross -> spindexer.moveToPosition(SpindexerMotionController.MotorPosition.INTAKE_1)
            gamepad1.circle -> spindexer.moveToPosition(SpindexerMotionController.MotorPosition.OUTTAKE_1)
            gamepad1.triangle -> spindexer.moveToPosition(SpindexerMotionController.MotorPosition.INTAKE_2)
            gamepad1.square -> spindexer.moveToPosition(SpindexerMotionController.MotorPosition.OUTTAKE_2)
            gamepad1.left_bumper -> spindexer.moveToPosition(SpindexerMotionController.MotorPosition.INTAKE_3)
            gamepad1.right_bumper -> spindexer.moveToPosition(SpindexerMotionController.MotorPosition.OUTTAKE_3)
        }
    }
    
    private fun handleManualControl() {
        val spindexer = bot.spindexer ?: return
        
        when {
            gamepad1.left_trigger > 0.1 -> {
                spindexer.moveManual(-gamepad1.left_trigger.toDouble())
                manualMode = true
            }
            gamepad1.right_trigger > 0.1 -> {
                spindexer.moveManual(gamepad1.right_trigger.toDouble())
                manualMode = true
            }
            manualMode -> {
                spindexer.moveManual(0.0)
                manualMode = false
            }
        }
    }
    
    private fun displayTelemetry() {
        val spindexer = bot.spindexer ?: return
        
        telemetry.addLine("=== SPINDEXER DIAGNOSTIC ===")
        telemetry.addLine()
        
        // Position info
        telemetry.addData("Target Position", spindexer.motorState.name)
        telemetry.addData("Closest Position", spindexer.closestMotorPosition.name)
        telemetry.addLine()
        
        // Encoder info
        telemetry.addData("Current Ticks", spindexer.currentMotorTicks)
        telemetry.addData("Target Ticks", spindexer.targetMotorTicks)
        telemetry.addData("Error Ticks", spindexer.targetMotorTicks - spindexer.currentMotorTicks)
        telemetry.addLine()
        
        // Motion info
        telemetry.addData("Velocity (ticks/sec)", "%.0f", spindexer.currentMotorVelocity)
        telemetry.addLine()
        
        // Status flags
        telemetry.addData("✓ Reached Target", if (spindexer.reachedTarget) "YES" else "NO")
        telemetry.addData("✓ Detection Range", if (spindexer.withinDetectionTolerance) "YES" else "NO")
        telemetry.addData("Manual Override", if (spindexer.manualOverride) "YES" else "NO")
        telemetry.addLine()
        
        // Artifacts
        telemetry.addData("Artifacts", "%s | %s | %s",
            spindexer.artifacts[0]?.name ?: "---",
            spindexer.artifacts[1]?.name ?: "---",
            spindexer.artifacts[2]?.name ?: "---"
        )
        telemetry.addData("Count", spindexer.numStoredArtifacts)
        telemetry.addLine()
        
        // Position change detection
        if (spindexer.motorState != lastPosition) {
            telemetry.addLine(">>> POSITION CHANGE DETECTED <<<")
            lastPosition = spindexer.motorState
        }
        
        telemetry.update()
    }
}

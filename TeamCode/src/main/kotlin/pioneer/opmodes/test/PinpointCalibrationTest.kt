package pioneer.opmodes.test

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.hardware.gobilda.GoBildaPinpointDriver
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.Pose2D
import pioneer.Constants

/**
 * Pinpoint Calibration Verification Test
 *
 * This OpMode helps verify if your Pinpoint odometry is properly calibrated.
 *
 * INSTRUCTIONS:
 * 1. Deploy this OpMode to the robot
 * 2. Select it on the Driver Station
 * 3. Press INIT, then START
 * 4. Use a measuring tape to physically move the robot:
 *    - Push forward EXACTLY 100cm
 *    - Push right EXACTLY 100cm
 *    - Rotate EXACTLY 360 degrees
 * 5. Compare actual measurements to telemetry readings
 * 6. If errors are > 5%, run GoBILDA's calibration procedure
 *
 * INTERPRETATION:
 * - Error < 2%: Excellent calibration
 * - Error 2-5%: Acceptable for most purposes
 * - Error > 5%: Needs calibration
 * - Error > 10%: Definitely needs calibration
 *
 * Created: January 29, 2026
 * Team: Pioneer Robotics 327
 */
@TeleOp(name = "Pinpoint Calibration Test", group = "Test")
class PinpointCalibrationTest : LinearOpMode() {

    override fun runOpMode() {
        telemetry.addLine("=================================")
        telemetry.addLine("PINPOINT CALIBRATION TEST")
        telemetry.addLine("=================================")
        telemetry.addLine()
        telemetry.addLine("This test verifies odometry accuracy")
        telemetry.addLine()
        telemetry.addLine("INSTRUCTIONS:")
        telemetry.addLine("1. Press START when ready")
        telemetry.addLine("2. Use tape measure to move robot:")
        telemetry.addLine("   - Push FORWARD exactly 100cm")
        telemetry.addLine("   - Push RIGHT exactly 100cm")
        telemetry.addLine("   - Rotate exactly 360 degrees")
        telemetry.addLine("3. Check if readings match")
        telemetry.addLine()
        telemetry.addLine("Press START to begin...")
        telemetry.update()

        // Initialize Pinpoint
        val pinpoint = hardwareMap.get(GoBildaPinpointDriver::class.java, Constants.HardwareNames.PINPOINT)

        // Configure with current robot settings
        pinpoint.setOffsets(Constants.Pinpoint.X_POD_OFFSET_MM, Constants.Pinpoint.Y_POD_OFFSET_MM, DistanceUnit.MM)
        pinpoint.setEncoderResolution(Constants.Pinpoint.ENCODER_RESOLUTION)
        pinpoint.setEncoderDirections(
            Constants.Pinpoint.X_ENCODER_DIRECTION,
            Constants.Pinpoint.Y_ENCODER_DIRECTION
        )

        waitForStart()

        // Recalibrate IMU and zero position
        telemetry.addLine("Calibrating IMU...")
        telemetry.update()
        pinpoint.recalibrateIMU()
        sleep(1000)

        telemetry.addLine("Setting zero position...")
        telemetry.update()
        pinpoint.setPosition(Pose2D(DistanceUnit.CM, 0.0, 0.0, AngleUnit.RADIANS, 0.0))

        telemetry.clear()
        telemetry.addLine("=================================")
        telemetry.addLine("READY TO TEST")
        telemetry.addLine("=================================")
        telemetry.addLine()
        telemetry.addLine("Move the robot using these steps:")
        telemetry.addLine()
        telemetry.addLine("STEP 1: Push FORWARD 100cm")
        telemetry.addLine("  - Check 'Forward (Y)' reads ~100")
        telemetry.addLine()
        telemetry.addLine("STEP 2: Push RIGHT 100cm")
        telemetry.addLine("  - Check 'Right (X)' reads ~100")
        telemetry.addLine()
        telemetry.addLine("STEP 3: Rotate 360 degrees")
        telemetry.addLine("  - Check 'Heading' returns to ~0")
        telemetry.addLine()
        telemetry.addLine("=================================")
        telemetry.update()

        sleep(3000)  // Give time to read instructions

        // Main loop - display position continuously
        while (opModeIsActive()) {
            pinpoint.update()

            // Convert coordinates to robot frame (same as main code)
            val x = -pinpoint.getPosY(DistanceUnit.CM)  // Robot X (right)
            val y = pinpoint.getPosX(DistanceUnit.CM)   // Robot Y (forward)
            val heading = pinpoint.getHeading(AngleUnit.DEGREES)

            telemetry.clear()
            telemetry.addLine("=================================")
            telemetry.addLine("CURRENT POSITION")
            telemetry.addLine("=================================")
            telemetry.addLine()

            // Position readings
            telemetry.addLine("POSITION (should match your movements):")
            telemetry.addData("  Forward (Y)", "%.2f cm", y)
            telemetry.addData("  Right (X)", "%.2f cm", x)
            telemetry.addData("  Heading", "%.1f degrees", heading)
            telemetry.addLine()

            // Error calculation helpers
            if (Math.abs(y - 100.0) < 30.0) {  // If near 100cm forward
                val forwardError = y - 100.0
                val forwardErrorPercent = (forwardError / 100.0) * 100.0
                telemetry.addLine("FORWARD TEST:")
                telemetry.addData("  Target", "100.0 cm")
                telemetry.addData("  Actual", "%.2f cm", y)
                telemetry.addData("  Error", "%.2f cm (%.1f%%)", forwardError, forwardErrorPercent)

                when {
                    Math.abs(forwardErrorPercent) < 2.0 -> telemetry.addData("  Status", "✓ EXCELLENT")
                    Math.abs(forwardErrorPercent) < 5.0 -> telemetry.addData("  Status", "✓ Good")
                    Math.abs(forwardErrorPercent) < 10.0 -> telemetry.addData("  Status", "⚠ Needs tuning")
                    else -> telemetry.addData("  Status", "✗ NEEDS CALIBRATION")
                }
                telemetry.addLine()
            }

            if (Math.abs(x - 100.0) < 30.0) {  // If near 100cm right
                val strafeError = x - 100.0
                val strafeErrorPercent = (strafeError / 100.0) * 100.0
                telemetry.addLine("STRAFE TEST:")
                telemetry.addData("  Target", "100.0 cm")
                telemetry.addData("  Actual", "%.2f cm", x)
                telemetry.addData("  Error", "%.2f cm (%.1f%%)", strafeError, strafeErrorPercent)

                when {
                    Math.abs(strafeErrorPercent) < 2.0 -> telemetry.addData("  Status", "✓ EXCELLENT")
                    Math.abs(strafeErrorPercent) < 5.0 -> telemetry.addData("  Status", "✓ Good")
                    Math.abs(strafeErrorPercent) < 10.0 -> telemetry.addData("  Status", "⚠ Needs tuning")
                    else -> telemetry.addData("  Status", "✗ NEEDS CALIBRATION")
                }
                telemetry.addLine()
            }

            if (Math.abs(heading) < 10.0 && (Math.abs(x) > 50.0 || Math.abs(y) > 50.0)) {  // If back near 0° after movement
                telemetry.addLine("ROTATION TEST:")
                telemetry.addData("  Target", "0.0 degrees")
                telemetry.addData("  Actual", "%.2f degrees", heading)
                telemetry.addData("  Error", "%.2f degrees", heading)

                when {
                    Math.abs(heading) < 2.0 -> telemetry.addData("  Status", "✓ EXCELLENT")
                    Math.abs(heading) < 5.0 -> telemetry.addData("  Status", "✓ Good")
                    Math.abs(heading) < 10.0 -> telemetry.addData("  Status", "⚠ Needs tuning")
                    else -> telemetry.addData("  Status", "✗ NEEDS CALIBRATION")
                }
                telemetry.addLine()
            }

            telemetry.addLine("---------------------------------")
            telemetry.addLine("RAW ENCODER DATA:")
            telemetry.addData("  X Encoder", "%d ticks", pinpoint.encoderX)
            telemetry.addData("  Y Encoder", "%d ticks", pinpoint.encoderY)
            telemetry.addLine()
            telemetry.addData("Pinpoint Status", pinpoint.deviceStatus)
            telemetry.addLine()
            telemetry.addLine("=================================")

            telemetry.update()
        }
    }
}
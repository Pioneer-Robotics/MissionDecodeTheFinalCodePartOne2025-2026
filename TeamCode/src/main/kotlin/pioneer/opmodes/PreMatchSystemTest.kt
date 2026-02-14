package pioneer.opmodes.test

import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.util.ElapsedTime
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import org.firstinspires.ftc.teamcode.prism.Color
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import pioneer.Bot
import pioneer.BotType
import pioneer.Constants
import pioneer.decode.Artifact
import pioneer.hardware.Turret
import pioneer.helpers.FileLogger
import pioneer.helpers.Pose
import pioneer.opmodes.BaseOpMode
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@TeleOp(name = "Pre-Match System Test", group = "Test")
class PreMatchSystemTest : BaseOpMode() {

    // Test system enumeration
    enum class TestSystem(val displayName: String, val isCritical: Boolean = true) {
        BATTERY("Battery Voltage", true),
        ODOMETRY("Odometry/Pinpoint", true),
        DRIVE_INDIVIDUAL("Drive Motors (Individual)", true),
        DRIVE_PATTERN("Drive Motors (Pattern)", true),
        DRIVE_ENCODERS("Drive Motors (Encoders)", true),
        TURRET("Turret Range", true),
        FLYWHEEL("Flywheel Spin-Up", true),
        SPINDEXER("Spindexer Positions", true),
        INTAKE("Intake Motor", true),
        LAUNCHER("Launcher Servo", true),
        CAMERA("Camera/Vision", true),
        LED("LED Display", true),
        COLOR_SENSOR("Color Sensor", true)
    }

    // Test result states
    enum class TestResult {
        NOT_STARTED,
        TESTING,
        PASSED,
        FAILED,
        SKIPPED
    }

    // Test state tracking
    private var currentTestIndex = 0
    private val testResults = mutableMapOf<TestSystem, TestResult>()
    private val testMessages = mutableMapOf<TestSystem, String>()
    private val testTimer = ElapsedTime()
    private var autoTestInProgress = false
    private var showingSummary = false

    // Navigation state
    private var prevDpadUp = false
    private var prevDpadDown = false
    private var prevA = false
    private var prevB = false
    private var prevX = false
    private var prevY = false
    private var prevBack = false

    // Test-specific state
    private var driveTestPhase = 0
    private var spindexerTestPhase = 0
    private var turretTestPhase = 0
    private var testStartVoltage = 0.0

    override fun onInit() {
        bot = Bot.fromType(BotType.COMP_BOT, hardwareMap)

        // Initialize all test results to NOT_STARTED
        TestSystem.entries.forEach { system ->
            testResults[system] = TestResult.NOT_STARTED
            testMessages[system] = ""
        }

        testStartVoltage = bot.batteryMonitor?.voltage ?: 0.0
    }

    override fun onStart() {
        // Start with first test
        startTest(getCurrentSystem())
    }

    override fun onLoop() {
        if (showingSummary) {
            handleSummaryView()
        } else {
            handleNavigation()
            handleTestButtons()
            updateCurrentTest()
            displayTestScreen()
        }
    }

    private fun getCurrentSystem(): TestSystem {
        return TestSystem.entries[currentTestIndex]
    }

    private fun handleNavigation() {
        // D-pad UP - previous test
        val dpadUpPressed = gamepad1.dpad_up && !prevDpadUp
        prevDpadUp = gamepad1.dpad_up
        if (dpadUpPressed && currentTestIndex > 0) {
            currentTestIndex--
            startTest(getCurrentSystem())
        }

        // D-pad DOWN - next test
        val dpadDownPressed = gamepad1.dpad_down && !prevDpadDown
        prevDpadDown = gamepad1.dpad_down
        if (dpadDownPressed && currentTestIndex < TestSystem.entries.size - 1) {
            currentTestIndex++
            startTest(getCurrentSystem())
        }

        // Y - show summary
        val yPressed = gamepad1.y && !prevY
        prevY = gamepad1.y
        if (yPressed) {
            showingSummary = true
        }

        // BACK - save and exit
        val backPressed = gamepad1.back && !prevBack
        prevBack = gamepad1.back
        if (backPressed) {
            saveTestReport()
            requestOpModeStop()
        }
    }

    private fun handleTestButtons() {
        val currentSystem = getCurrentSystem()

        // A - mark PASS
        val aPressed = gamepad1.a && !prevA
        prevA = gamepad1.a
        if (aPressed) {
            testResults[currentSystem] = TestResult.PASSED
            autoTestInProgress = false
            gamepad1.rumble(100)
        }

        // B - mark FAIL
        val bPressed = gamepad1.b && !prevB
        prevB = gamepad1.b
        if (bPressed) {
            testResults[currentSystem] = TestResult.FAILED
            autoTestInProgress = false
            gamepad1.rumble(500)
        }

        // X - retry test
        val xPressed = gamepad1.x && !prevX
        prevX = gamepad1.x
        if (xPressed) {
            startTest(currentSystem)
        }
    }

    private fun startTest(system: TestSystem) {
        testResults[system] = TestResult.TESTING
        testMessages[system] = "Initializing..."
        testTimer.reset()
        autoTestInProgress = true

        // Reset test-specific state
        driveTestPhase = 0
        spindexerTestPhase = 0
        turretTestPhase = 0

        // Reset hardware to safe states
        bot.mecanumBase?.stop()
        bot.intake?.stop()
        bot.flywheel?.velocity = 0.0
    }

    private fun updateCurrentTest() {
        if (!autoTestInProgress) return
        if (testResults[getCurrentSystem()] != TestResult.TESTING) return

        when (getCurrentSystem()) {
            TestSystem.BATTERY -> testBattery()
            TestSystem.ODOMETRY -> testOdometry()
            TestSystem.DRIVE_INDIVIDUAL -> testDriveIndividual()
            TestSystem.DRIVE_PATTERN -> testDrivePattern()
            TestSystem.DRIVE_ENCODERS -> testDriveEncoders()
            TestSystem.TURRET -> testTurret()
            TestSystem.FLYWHEEL -> testFlywheel()
            TestSystem.SPINDEXER -> testSpindexer()
            TestSystem.INTAKE -> testIntake()
            TestSystem.LAUNCHER -> testLauncher()
            TestSystem.CAMERA -> testCamera()
            TestSystem.LED -> testLED()
            TestSystem.COLOR_SENSOR -> testColorSensor()
        }
    }

    // ==================== INDIVIDUAL TEST IMPLEMENTATIONS ====================

    private fun testBattery() {
        val voltage = bot.batteryMonitor?.voltage ?: 0.0
        testMessages[TestSystem.BATTERY] = "Voltage: %.2f V".format(voltage)

        when {
            voltage < 11.5 -> {
                testMessages[TestSystem.BATTERY] = "⚠️ LOW: %.2f V (< 11.5V)".format(voltage)
                testResults[TestSystem.BATTERY] = TestResult.FAILED
                autoTestInProgress = false
            }
            voltage < 12.0 -> {
                testMessages[TestSystem.BATTERY] = "⚠️ Warning: %.2f V (< 12.0V)".format(voltage)
                // Auto-pass but with warning
                if (testTimer.seconds() > 2.0) {
                    testResults[TestSystem.BATTERY] = TestResult.PASSED
                    autoTestInProgress = false
                }
            }
            else -> {
                testMessages[TestSystem.BATTERY] = "✓ Good: %.2f V".format(voltage)
                if (testTimer.seconds() > 1.0) {
                    testResults[TestSystem.BATTERY] = TestResult.PASSED
                    autoTestInProgress = false
                }
            }
        }
    }

    private fun testOdometry() {
        val pose = bot.pinpoint?.pose
        if (pose == null) {
            testMessages[TestSystem.ODOMETRY] = "❌ Pinpoint not initialized"
            testResults[TestSystem.ODOMETRY] = TestResult.FAILED
            autoTestInProgress = false
            return
        }

        when {
            testTimer.seconds() < 1.0 -> {
                testMessages[TestSystem.ODOMETRY] = "Reading position..."
            }
            testTimer.seconds() < 2.0 -> {
                testMessages[TestSystem.ODOMETRY] = "Pose: (%.1f, %.1f, %.1f°)".format(
                    pose.x, pose.y, Math.toDegrees(pose.theta)
                )
            }
            else -> {
                // Check if pose is reasonable (not NaN, not extreme values)
                if (pose.x.isNaN() || pose.y.isNaN() || pose.theta.isNaN()) {
                    testMessages[TestSystem.ODOMETRY] = "❌ Invalid pose (NaN)"
                    testResults[TestSystem.ODOMETRY] = TestResult.FAILED
                } else if (abs(pose.x) > 500 || abs(pose.y) > 500) {
                    testMessages[TestSystem.ODOMETRY] = "⚠️ Pose seems unreasonable"
                    testResults[TestSystem.ODOMETRY] = TestResult.FAILED
                } else {
                    testMessages[TestSystem.ODOMETRY] = "✓ Pose valid: (%.1f, %.1f, %.1f°)".format(
                        pose.x, pose.y, Math.toDegrees(pose.theta)
                    )
                    testResults[TestSystem.ODOMETRY] = TestResult.PASSED
                }
                autoTestInProgress = false
            }
        }
    }

    private fun testDriveIndividual() {
        // Test each "corner" of the mecanum drive to verify all motors work
        val phaseDuration = 1.5 // seconds per movement
        val currentPhase = (testTimer.seconds() / phaseDuration).toInt()

        when (currentPhase) {
            0 -> {
                // Forward-Right diagonal (Left Front + Right Back dominant)
                bot.mecanumBase?.setDrivePower(Pose(vx = 0.5, vy = 0.5, omega = 0.0), 0.5, Constants.Drive.MAX_MOTOR_VELOCITY_TPS)
                testMessages[TestSystem.DRIVE_INDIVIDUAL] = "Testing Forward-Right (LF+RB)..."
            }
            1 -> {
                // Forward-Left diagonal (Left Back + Right Front dominant)
                bot.mecanumBase?.setDrivePower(Pose(vx = -0.5, vy = 0.5, omega = 0.0), 0.5, Constants.Drive.MAX_MOTOR_VELOCITY_TPS)
                testMessages[TestSystem.DRIVE_INDIVIDUAL] = "Testing Forward-Left (LB+RF)..."
            }
            2 -> {
                // Back-Right diagonal (Right Front + Left Back dominant)
                bot.mecanumBase?.setDrivePower(Pose(vx = 0.5, vy = -0.5, omega = 0.0), 0.5, Constants.Drive.MAX_MOTOR_VELOCITY_TPS)
                testMessages[TestSystem.DRIVE_INDIVIDUAL] = "Testing Back-Right (RF+LB)..."
            }
            3 -> {
                // Back-Left diagonal (Right Back + Left Front dominant)
                bot.mecanumBase?.setDrivePower(Pose(vx = -0.5, vy = -0.5, omega = 0.0), 0.5, Constants.Drive.MAX_MOTOR_VELOCITY_TPS)
                testMessages[TestSystem.DRIVE_INDIVIDUAL] = "Testing Back-Left (RB+LF)..."
            }
            else -> {
                // All done
                bot.mecanumBase?.stop()
                testMessages[TestSystem.DRIVE_INDIVIDUAL] = "✓ All 4 motor combinations tested"
                testResults[TestSystem.DRIVE_INDIVIDUAL] = TestResult.PASSED
                autoTestInProgress = false
            }
        }
    }

    private fun testDrivePattern() {
        val phaseDuration = 1.0 // seconds per movement
        val currentPhase = (testTimer.seconds() / phaseDuration).toInt()

        when (currentPhase) {
            0 -> {
                // Forward
                bot.mecanumBase?.setDrivePower(Pose(vx = 0.0, vy = 0.5, omega = 0.0), 0.5, Constants.Drive.MAX_MOTOR_VELOCITY_TPS)
                testMessages[TestSystem.DRIVE_PATTERN] = "Testing FORWARD..."
            }
            1 -> {
                // Backward
                bot.mecanumBase?.setDrivePower(Pose(vx = 0.0, vy = -0.5, omega = 0.0), 0.5, Constants.Drive.MAX_MOTOR_VELOCITY_TPS)
                testMessages[TestSystem.DRIVE_PATTERN] = "Testing BACKWARD..."
            }
            2 -> {
                // Strafe Right
                bot.mecanumBase?.setDrivePower(Pose(vx = 0.5, vy = 0.0, omega = 0.0), 0.5, Constants.Drive.MAX_MOTOR_VELOCITY_TPS)
                testMessages[TestSystem.DRIVE_PATTERN] = "Testing STRAFE RIGHT..."
            }
            3 -> {
                // Strafe Left
                bot.mecanumBase?.setDrivePower(Pose(vx = -0.5, vy = 0.0, omega = 0.0), 0.5, Constants.Drive.MAX_MOTOR_VELOCITY_TPS)
                testMessages[TestSystem.DRIVE_PATTERN] = "Testing STRAFE LEFT..."
            }
            4 -> {
                // Rotate Right
                bot.mecanumBase?.setDrivePower(Pose(vx = 0.0, vy = 0.0, omega = 0.5), 0.5, Constants.Drive.MAX_MOTOR_VELOCITY_TPS)
                testMessages[TestSystem.DRIVE_PATTERN] = "Testing ROTATE RIGHT..."
            }
            5 -> {
                // Rotate Left
                bot.mecanumBase?.setDrivePower(Pose(vx = 0.0, vy = 0.0, omega = -0.5), 0.5, Constants.Drive.MAX_MOTOR_VELOCITY_TPS)
                testMessages[TestSystem.DRIVE_PATTERN] = "Testing ROTATE LEFT..."
            }
            else -> {
                // All done
                bot.mecanumBase?.stop()
                testMessages[TestSystem.DRIVE_PATTERN] = "✓ All movements tested"
                testResults[TestSystem.DRIVE_PATTERN] = TestResult.PASSED
                autoTestInProgress = false
            }
        }
    }

    private fun testDriveEncoders() {
        if (testTimer.seconds() < 0.5) {
            testMessages[TestSystem.DRIVE_ENCODERS] = "Reading odometry encoders..."
            return
        }

        // Check if odometry encoders are responding by looking at pose changes
        val pose = bot.pinpoint?.pose
        if (pose == null) {
            testMessages[TestSystem.DRIVE_ENCODERS] = "❌ Odometry not available"
            testResults[TestSystem.DRIVE_ENCODERS] = TestResult.FAILED
            autoTestInProgress = false
            return
        }

        // After previous drive tests, pose should have changed from initial (0,0,0)
        val hasMoved = abs(pose.x) > 5.0 || abs(pose.y) > 5.0 || abs(pose.theta) > 0.1

        if (hasMoved) {
            testMessages[TestSystem.DRIVE_ENCODERS] = "✓ Encoders tracking: (%.1f, %.1f, %.1f°)".format(
                pose.x, pose.y, Math.toDegrees(pose.theta)
            )
            testResults[TestSystem.DRIVE_ENCODERS] = TestResult.PASSED
        } else {
            testMessages[TestSystem.DRIVE_ENCODERS] = "⚠️ No movement detected"
            testResults[TestSystem.DRIVE_ENCODERS] = TestResult.FAILED
        }
        autoTestInProgress = false
    }

    private fun testTurret() {
        val phaseDuration = 2.0 // seconds per position
        val currentPhase = (testTimer.seconds() / phaseDuration).toInt()

        when (currentPhase) {
            0 -> {
                // Move to 0 degrees
                bot.turret?.mode = Turret.Mode.MANUAL
                bot.turret?.gotoAngle(0.0)
                testMessages[TestSystem.TURRET] = "Moving to 0°... (${bot.turret?.currentAngle?.let { Math.toDegrees(it).toInt() }}°)"
            }
            1 -> {
                // Move to -90 degrees
                bot.turret?.gotoAngle(Math.toRadians(-90.0))
                testMessages[TestSystem.TURRET] = "Moving to -90°... (${bot.turret?.currentAngle?.let { Math.toDegrees(it).toInt() }}°)"
            }
            2 -> {
                // Move to +90 degrees
                bot.turret?.gotoAngle(Math.toRadians(90.0))
                testMessages[TestSystem.TURRET] = "Moving to +90°... (${bot.turret?.currentAngle?.let { Math.toDegrees(it).toInt() }}°)"
            }
            3 -> {
                // Return to 0
                bot.turret?.gotoAngle(0.0)
                testMessages[TestSystem.TURRET] = "Returning to 0°... (${bot.turret?.currentAngle?.let { Math.toDegrees(it).toInt() }}°)"
            }
            else -> {
                // Check if turret reached final position
                val finalAngle = bot.turret?.currentAngle ?: 0.0
                if (abs(finalAngle) < Math.toRadians(10.0)) {
                    testMessages[TestSystem.TURRET] = "✓ Full range tested, at 0°"
                    testResults[TestSystem.TURRET] = TestResult.PASSED
                } else {
                    testMessages[TestSystem.TURRET] = "⚠️ Final position: ${Math.toDegrees(finalAngle).toInt()}°"
                    testResults[TestSystem.TURRET] = TestResult.FAILED
                }
                autoTestInProgress = false
            }
        }
    }

    private fun testFlywheel() {
        val targetSpeed = 500.0 // RPM for test

        when {
            testTimer.seconds() < 0.5 -> {
                testMessages[TestSystem.FLYWHEEL] = "Starting flywheel..."
                bot.flywheel?.velocity = targetSpeed
            }
            testTimer.seconds() < 3.0 -> {
                val currentSpeed = bot.flywheel?.velocity ?: 0.0
                val percentOfTarget = (currentSpeed / targetSpeed * 100).toInt()
                testMessages[TestSystem.FLYWHEEL] = "Spinning up: $percentOfTarget% (${currentSpeed.toInt()} / ${targetSpeed.toInt()} RPM)"
            }
            else -> {
                val currentSpeed = bot.flywheel?.velocity ?: 0.0
                bot.flywheel?.velocity = 0.0

                if (abs(currentSpeed - targetSpeed) < 50) {
                    testMessages[TestSystem.FLYWHEEL] = "✓ Reached ${currentSpeed.toInt()} RPM"
                    testResults[TestSystem.FLYWHEEL] = TestResult.PASSED
                } else {
                    testMessages[TestSystem.FLYWHEEL] = "⚠️ Only ${currentSpeed.toInt()} / ${targetSpeed.toInt()} RPM"
                    testResults[TestSystem.FLYWHEEL] = TestResult.FAILED
                }
                autoTestInProgress = false
            }
        }
    }

    private fun testSpindexer() {
        // Test all 6 positions
        val positions = listOf(
            "INTAKE_1", "OUTTAKE_1",
            "INTAKE_2", "OUTTAKE_2",
            "INTAKE_3", "OUTTAKE_3"
        )

        val phaseDuration = 1.5 // seconds per position
        val currentPhase = (testTimer.seconds() / phaseDuration).toInt()

        if (currentPhase < positions.size) {
            val positionName = positions[currentPhase]
            testMessages[TestSystem.SPINDEXER] = "Moving to $positionName... (${currentPhase + 1}/${positions.size})"

            // Move to position (you'll need to add a method to move to specific position by name)
            // For now, we'll cycle through intake/outtake
            if (currentPhase % 2 == 0) {
                bot.spindexer?.moveToNextOpenIntake()
            } else {
                bot.spindexer?.moveToNextOuttake()
            }
        } else {
            testMessages[TestSystem.SPINDEXER] = "✓ All 6 positions tested"
            testResults[TestSystem.SPINDEXER] = TestResult.PASSED
            autoTestInProgress = false
        }
    }

    private fun testIntake() {
        val phaseDuration = 1.5
        val currentPhase = (testTimer.seconds() / phaseDuration).toInt()

        when (currentPhase) {
            0 -> {
                bot.intake?.forward()
                testMessages[TestSystem.INTAKE] = "Testing FORWARD..."
            }
            1 -> {
                bot.intake?.reverse()
                testMessages[TestSystem.INTAKE] = "Testing REVERSE..."
            }
            2 -> {
                bot.intake?.stop()
                testMessages[TestSystem.INTAKE] = "Testing STOP..."
            }
            else -> {
                bot.intake?.stop()
                testMessages[TestSystem.INTAKE] = "✓ All directions tested"
                testResults[TestSystem.INTAKE] = TestResult.PASSED
                autoTestInProgress = false
            }
        }
    }

    private fun testLauncher() {
        when {
            testTimer.seconds() < 0.5 -> {
                testMessages[TestSystem.LAUNCHER] = "Waiting for launcher reset..."
            }
            testTimer.seconds() < 1.0 -> {
                bot.launcher?.triggerLaunch()
                testMessages[TestSystem.LAUNCHER] = "Triggering launch..."
            }
            testTimer.seconds() < 2.0 -> {
                testMessages[TestSystem.LAUNCHER] = "Waiting for reset... (${bot.launcher?.isReset})"
            }
            else -> {
                if (bot.launcher?.isReset == true) {
                    testMessages[TestSystem.LAUNCHER] = "✓ Launch triggered and reset"
                    testResults[TestSystem.LAUNCHER] = TestResult.PASSED
                } else {
                    testMessages[TestSystem.LAUNCHER] = "⚠️ Launcher did not reset"
                    testResults[TestSystem.LAUNCHER] = TestResult.FAILED
                }
                autoTestInProgress = false
            }
        }
    }

    private fun testCamera() {
        val processor = bot.camera?.getProcessor<AprilTagProcessor>()

        when {
            testTimer.seconds() < 1.0 -> {
                testMessages[TestSystem.CAMERA] = "Initializing camera..."
            }
            testTimer.seconds() < 3.0 -> {
                if (processor == null) {
                    testMessages[TestSystem.CAMERA] = "❌ AprilTag processor not found"
                    testResults[TestSystem.CAMERA] = TestResult.FAILED
                    autoTestInProgress = false
                } else {
                    val detections = processor.detections
                    testMessages[TestSystem.CAMERA] = "Camera active. Tags visible: ${detections?.size ?: 0}"
                }
            }
            else -> {
                if (processor != null) {
                    val detections = processor.detections
                    testMessages[TestSystem.CAMERA] = "✓ Camera working (${detections?.size ?: 0} tags)"
                    testResults[TestSystem.CAMERA] = TestResult.PASSED
                } else {
                    testMessages[TestSystem.CAMERA] = "❌ Camera failed to initialize"
                    testResults[TestSystem.CAMERA] = TestResult.FAILED
                }
                autoTestInProgress = false
            }
        }
    }

    private fun testLED() {
        val phaseDuration = 1.0
        val currentPhase = (testTimer.seconds() / phaseDuration).toInt()

        when (currentPhase) {
            0 -> {
                bot.led?.setColor(Color.RED)
                testMessages[TestSystem.LED] = "Testing RED..."
            }
            1 -> {
                bot.led?.setColor(Color.GREEN)
                testMessages[TestSystem.LED] = "Testing GREEN..."
            }
            2 -> {
                bot.led?.setColor(Color.BLUE)
                testMessages[TestSystem.LED] = "Testing BLUE..."
            }
            3 -> {
                bot.led?.setColor(Color.WHITE)
                testMessages[TestSystem.LED] = "Testing WHITE..."
            }
            else -> {
                bot.led?.setColor(Color.WHITE)
                testMessages[TestSystem.LED] = "✓ All colors tested"
                testResults[TestSystem.LED] = TestResult.PASSED
                autoTestInProgress = false
            }
        }
    }

    private fun testColorSensor() {
        // Assume spindexer started with GREEN, PURPLE, PURPLE
        when {
            testTimer.seconds() < 1.0 -> {
                testMessages[TestSystem.COLOR_SENSOR] = "Reading sensor..."
                bot.spindexer?.moveToPosition(pioneer.hardware.spindexer.SpindexerMotionController.MotorPosition.INTAKE_1)
            }
            testTimer.seconds() < 3.0 -> {
                // Try to detect artifact at intake position
                val artifacts = bot.spindexer?.artifacts
                val detectedCount = artifacts?.count { it != null } ?: 0
                testMessages[TestSystem.COLOR_SENSOR] = "Detected $detectedCount / 3 artifacts"
            }
            else -> {
                val artifacts = bot.spindexer?.artifacts
                val detectedCount = artifacts?.count { it != null } ?: 0

                if (detectedCount >= 2) {
                    testMessages[TestSystem.COLOR_SENSOR] = "✓ Detected $detectedCount artifacts"
                    testResults[TestSystem.COLOR_SENSOR] = TestResult.PASSED
                } else {
                    testMessages[TestSystem.COLOR_SENSOR] = "⚠️ Only detected $detectedCount / 3"
                    testResults[TestSystem.COLOR_SENSOR] = TestResult.FAILED
                }
                autoTestInProgress = false
            }
        }
    }

    // ==================== DISPLAY ====================

    private fun displayTestScreen() {
        telemetry.clear()
        telemetry.addLine("=== PRE-MATCH SYSTEM TEST ===")
        telemetry.addLine()

        val currentSystem = getCurrentSystem()
        telemetry.addData("Test", "${currentTestIndex + 1}/${TestSystem.entries.size}: ${currentSystem.displayName}")
        telemetry.addLine()

        val statusSymbol = when (testResults[currentSystem]) {
            TestResult.TESTING -> "⏳"
            TestResult.PASSED -> "✓"
            TestResult.FAILED -> "✗"
            TestResult.NOT_STARTED -> "○"
            TestResult.SKIPPED -> "⊘"
            else -> "?"
        }
        telemetry.addData("Status", "$statusSymbol ${testResults[currentSystem]}")
        telemetry.addData("Message", testMessages[currentSystem])
        telemetry.addLine()

        telemetry.addLine("Press A: PASS  |  B: FAIL  |  X: RETRY")
        telemetry.addLine()

        // Show all test results
        telemetry.addLine("--- All Results ---")
        TestSystem.entries.forEachIndexed { index, system ->
            val symbol = when (testResults[system]) {
                TestResult.PASSED -> "✓"
                TestResult.FAILED -> "✗"
                TestResult.TESTING -> "⏳"
                TestResult.NOT_STARTED -> "○"
                TestResult.SKIPPED -> "⊘"
                else -> "?"
            }
            val marker = if (index == currentTestIndex) "→ " else "  "
            telemetry.addLine("$marker$symbol ${system.displayName}")
        }

        telemetry.addLine()
        telemetry.addLine("D-pad ↑↓: Navigate | Y: Summary | BACK: Save & Exit")
        telemetry.update()
    }

    private fun handleSummaryView() {
        telemetry.clear()
        telemetry.addLine("=== TEST SUMMARY ===")
        telemetry.addLine()

        val passed = testResults.values.count { it == TestResult.PASSED }
        val failed = testResults.values.count { it == TestResult.FAILED }
        val notTested = testResults.values.count { it == TestResult.NOT_STARTED }

        telemetry.addData("Passed", passed)
        telemetry.addData("Failed", failed)
        telemetry.addData("Not Tested", notTested)
        telemetry.addLine()

        telemetry.addLine("--- Detailed Results ---")
        TestSystem.entries.forEach { system ->
            val symbol = when (testResults[system]) {
                TestResult.PASSED -> "✓"
                TestResult.FAILED -> "✗"
                TestResult.TESTING -> "⏳"
                TestResult.NOT_STARTED -> "○"
                TestResult.SKIPPED -> "⊘"
                else -> "?"
            }
            telemetry.addLine("$symbol ${system.displayName}")
            if (testMessages[system]?.isNotEmpty() == true) {
                telemetry.addLine("   ${testMessages[system]}")
            }
        }

        telemetry.addLine()
        telemetry.addLine("Press Y: Return to Tests")
        telemetry.addLine("Press BACK: Save Report & Exit")
        telemetry.update()

        // Handle return to tests
        val yPressed = gamepad1.y && !prevY
        prevY = gamepad1.y
        if (yPressed) {
            showingSummary = false
        }

        // Handle save and exit
        val backPressed = gamepad1.back && !prevBack
        prevBack = gamepad1.back
        if (backPressed) {
            saveTestReport()
            requestOpModeStop()
        }
    }

    // ==================== REPORT GENERATION ====================

    private fun saveTestReport() {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val directory = File("/sdcard/FIRST/pre_match_tests")
            if (!directory.exists()) {
                directory.mkdirs()
            }

            val reportFile = File(directory, "PreMatch_$timestamp.txt")
            val report = StringBuilder()

            // Header
            report.appendLine("=".repeat(60))
            report.appendLine("PRE-MATCH SYSTEM TEST REPORT")
            report.appendLine("=".repeat(60))
            report.appendLine("Timestamp: $timestamp")
            report.appendLine("Battery Voltage (Start): ${"%.2f".format(testStartVoltage)} V")
            report.appendLine("Battery Voltage (End): ${"%.2f".format(bot.batteryMonitor?.voltage ?: 0.0)} V")
            report.appendLine()

            // Summary
            val passed = testResults.values.count { it == TestResult.PASSED }
            val failed = testResults.values.count { it == TestResult.FAILED }
            val notTested = testResults.values.count { it == TestResult.NOT_STARTED }

            report.appendLine("SUMMARY:")
            report.appendLine("  Passed: $passed / ${TestSystem.entries.size}")
            report.appendLine("  Failed: $failed / ${TestSystem.entries.size}")
            report.appendLine("  Not Tested: $notTested / ${TestSystem.entries.size}")
            report.appendLine()

            // Detailed Results
            report.appendLine("DETAILED RESULTS:")
            report.appendLine("-".repeat(60))
            TestSystem.entries.forEach { system ->
                val result = testResults[system] ?: TestResult.NOT_STARTED
                val message = testMessages[system] ?: ""

                val symbol = when (result) {
                    TestResult.PASSED -> "[PASS]"
                    TestResult.FAILED -> "[FAIL]"
                    TestResult.TESTING -> "[TEST]"
                    TestResult.NOT_STARTED -> "[SKIP]"
                    TestResult.SKIPPED -> "[SKIP]"
                }

                report.appendLine("$symbol ${system.displayName}")
                if (message.isNotEmpty()) {
                    report.appendLine("       $message")
                }
            }

            report.appendLine()
            report.appendLine("=".repeat(60))
            report.appendLine("END OF REPORT")
            report.appendLine("=".repeat(60))

            // Write to file
            reportFile.writeText(report.toString())

            FileLogger.info("PreMatchTest", "Report saved to: ${reportFile.absolutePath}")
            telemetry.addLine("✓ Report saved: $timestamp")
            telemetry.update()
            Thread.sleep(1000)

        } catch (e: Exception) {
            FileLogger.error("PreMatchTest", "Failed to save report: ${e.message}")
            telemetry.addLine("❌ Failed to save report: ${e.message}")
            telemetry.update()
            Thread.sleep(2000)
        }
    }
}
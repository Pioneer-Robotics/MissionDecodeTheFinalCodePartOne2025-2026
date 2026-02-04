# TEST MATRIX AUTOMATION - QUICK START GUIDE
**FTC DECODE 2025-2026 - Pioneer Robotics Team 327**

---

## 🎯 What This Does

Automatically fills your Requirements Test Matrix spreadsheet with data from robot tests:
- **No manual data entry** - runs capture everything
- **Automatic calculations** - shot accuracy, collection success, etc.
- **Updates your ODS file** - keeps test matrix current
- **Generates reports** - see trends and progress

---

## ⚡ Quick Start (5 Minutes)

### Step 1: Add Logger to OpMode (2 min)

```kotlin
// Add to any autonomous or teleop OpMode

import pioneer.helpers.TestMatrixLogger
import pioneer.helpers.TestMatrixLogger.TestType

class YourOpMode : BaseOpMode() {
    private lateinit var testLogger: TestMatrixLogger
    
    override fun onInit() {
        // ... your existing init code ...
        
        // Add this:
        testLogger = TestMatrixLogger(
            bot, 
            "AUTO_001",              // Test ID
            "Goal Side Autonomous",  // Description
            TestType.FUNCTIONAL      // Type
        )
    }
    
    override fun onStart() {
        // ... your existing start code ...
        
        testLogger.start()
    }
    
    override fun onLoop() {
        // ... your existing loop code ...
        
        // Logs telemetry automatically:
        testLogger.logAutomaticTelemetry()
        
        // Log specific events:
        if (shotFired) {
            testLogger.logShot(
                shotNumber = 1,
                distance = distanceToGoal,
                targetX = goalX,
                targetY = goalY,
                hit = ballWentIn
            )
        }
    }
    
    override fun onStop() {
        // Determine pass/fail
        val accuracy = calculateAccuracy()
        if (accuracy >= 90.0) {
            testLogger.setResult(
                TestMatrixLogger.TestResult.PASS,
                "Shot accuracy: $accuracy%"
            )
        } else {
            testLogger.setResult(
                TestMatrixLogger.TestResult.FAIL,
                "Shot accuracy: $accuracy% (threshold: 90%)"
            )
        }
        
        testLogger.stop()
        super.onStop()
    }
}
```

### Step 2: Run Test (1 min)

1. Deploy code to robot
2. Run your OpMode
3. Test logs automatically to `/sdcard/FIRST/test_logs/`

### Step 3: Pull Logs (1 min)

```bash
# Connect robot via USB
adb pull /sdcard/FIRST/test_logs/ ./test_logs/
```

### Step 4: Update Matrix (1 min)

```bash
# Install Python dependencies (first time only)
pip install odfpy pandas numpy

# Run updater
python update_test_matrix.py ./test_logs FTC_2025-26_Decode_Requirements_TestMatrix.ods
```

**Done!** Your test matrix ODS file is now updated with the results! 🎉

---

## 📊 What Gets Logged Automatically

### Motion Control System (MCS):
- Position (x, y, heading)
- Velocity (vx, vy, omega)
- Acceleration
- Path following errors

### Projectile Launch Control System (PLCS):
- Flywheel velocity (target vs actual)
- Flywheel current
- Turret angle (target vs actual)
- Shot accuracy
- Spin-up time

### Artifact Management System (AMS):
- Spindexer position/velocity
- Artifact count
- Collection success rate
- Intake timing

### Power System:
- Battery voltage (start/end)
- Voltage drop during test

---

## 📝 Logging Specific Events

### Log a Shot:
```kotlin
testLogger.logShot(
    shotNumber = 1,
    distance = 120.0,     // cm to goal
    targetX = goalX,
    targetY = goalY,
    hit = true            // or false
)
```

### Log a Collection:
```kotlin
testLogger.logCollection(
    artifactType = "PURPLE",
    position = "AUDIENCE",
    success = true,
    timeMs = 850
)
```

### Log Custom Metric:
```kotlin
testLogger.logMetric(
    name = "turret_settle_time",
    value = 0.325,
    unit = "seconds",
    system = "PLCS"
)
```

### Log Custom Event:
```kotlin
testLogger.logEvent("PATH_START", mapOf(
    "path_name" to "collect_goal",
    "distance" to "85.2"
))
```

---

## 🎨 Example OpModes

### Autonomous Test Example:

```kotlin
@Autonomous(name = "Goal Side Auto - Testable")
class GoalSideAutoTest : BaseOpMode() {
    private lateinit var testLogger: TestMatrixLogger
    private var shotsFired = 0
    private var shotsHit = 0
    private var collectionsAttempted = 0
    private var collectionsSuccessful = 0
    
    override fun onInit() {
        bot = Bot.fromType(BotType.COMP_BOT, hardwareMap)
        testLogger = TestMatrixLogger(
            bot,
            "AUTO_GOAL_${SimpleDateFormat("MMdd_HHmm").format(Date())}",
            "Goal Side Autonomous - Full Cycle",
            TestMatrixLogger.TestType.FUNCTIONAL
        )
    }
    
    override fun onStart() {
        testLogger.start()
        // ... autonomous setup ...
    }
    
    override fun onLoop() {
        // Your autonomous state machine
        when (state) {
            State.SHOOT -> {
                if (shotJustFired) {
                    shotsFired++
                    val hit = checkIfHit()
                    if (hit) shotsHit++
                    
                    testLogger.logShot(
                        shotNumber = shotsFired,
                        distance = bot.pinpoint!!.pose.distanceTo(goalPose),
                        targetX = goalPose.x,
                        targetY = goalPose.y,
                        hit = hit
                    )
                }
            }
            
            State.COLLECT -> {
                if (collectionAttempted) {
                    collectionsAttempted++
                    val success = bot.spindexer?.numStoredArtifacts!! > previousCount
                    if (success) collectionsSuccessful++
                    
                    testLogger.logCollection(
                        artifactType = "PURPLE",
                        position = currentCollectionSpot,
                        success = success,
                        timeMs = collectionTimer.milliseconds().toLong()
                    )
                }
            }
        }
        
        // Auto-log telemetry
        testLogger.logAutomaticTelemetry()
    }
    
    override fun onStop() {
        // Calculate results
        val shotAccuracy = if (shotsFired > 0) {
            (shotsHit.toDouble() / shotsFired * 100)
        } else 0.0
        
        val collectionRate = if (collectionsAttempted > 0) {
            (collectionsSuccessful.toDouble() / collectionsAttempted * 100)
        } else 0.0
        
        // Determine pass/fail (90% threshold)
        val passed = shotAccuracy >= 90.0 && collectionRate >= 90.0
        
        testLogger.setResult(
            if (passed) TestMatrixLogger.TestResult.PASS 
            else TestMatrixLogger.TestResult.FAIL,
            "Shot: $shotAccuracy%, Collection: $collectionRate%"
        )
        
        testLogger.stop()
        super.onStop()
    }
}
```

### Characterization Test Example:

```kotlin
@TeleOp(name = "Flywheel Characterization")
class FlywheelCharacterization : BaseOpMode() {
    private lateinit var testLogger: TestMatrixLogger
    private var testPhase = 0
    private val speeds = listOf(500.0, 750.0, 1000.0)  // TPS
    
    override fun onInit() {
        bot = Bot.fromType(BotType.COMP_BOT, hardwareMap)
        val battery = bot.batteryMonitor?.voltage ?: 0.0
        
        testLogger = TestMatrixLogger(
            bot,
            "CHAR_FW_${(battery/12*100).toInt()}PCT",
            "Flywheel Characterization @ ${battery}V",
            TestMatrixLogger.TestType.CHARACTERIZATION
        )
    }
    
    override fun onStart() {
        testLogger.start()
    }
    
    override fun onLoop() {
        // Cycle through speeds on button press
        if (gamepad1.a) {
            testPhase = (testPhase + 1) % speeds.size
            val targetSpeed = speeds[testPhase]
            
            // Log spin-up
            val startTime = System.currentTimeMillis()
            bot.flywheel?.velocity = targetSpeed
            
            // Wait for speed
            while ((bot.flywheel?.velocity ?: 0.0) < targetSpeed - 20) {
                sleep(10)
            }
            val spinUpTime = (System.currentTimeMillis() - startTime) / 1000.0
            
            testLogger.logMetric(
                "spin_up_time",
                spinUpTime,
                "seconds",
                "PLCS"
            )
            
            testLogger.logEvent("SPEED_TEST", mapOf(
                "target_speed" to targetSpeed.toString(),
                "spin_up_time" to spinUpTime.toString()
            ))
        }
        
        testLogger.logAutomaticTelemetry()
        
        telemetry.addData("Test Phase", "$testPhase / ${speeds.size}")
        telemetry.addData("Target Speed", speeds[testPhase])
        telemetry.addData("Press A", "Next Speed")
    }
    
    override fun onStop() {
        testLogger.setResult(
            TestMatrixLogger.TestResult.PASS,
            "Completed ${speeds.size} speed tests"
        )
        testLogger.stop()
        super.onStop()
    }
}
```

---

## 📈 Understanding the Output

### CSV File Structure:
```csv
timestamp,elapsed_s,metric_name,metric_value,unit,system,event_type,event_params
0.125,0.125,mcs_pos_x,12.5,cm,MCS,,
0.125,0.125,mcs_pos_y,45.2,cm,MCS,,
0.125,0.125,plcs_flywheel_velocity,815.2,tps,PLCS,,
1.523,1.523,,,,,SHOT_FIRED,"shot_number=1;distance=120.5;result=HIT"
```

### Updated ODS File:
Your test matrix will have new rows added automatically:
- **Test Matrix sheet**: New test with results
- **Characterization sheet**: Data from characterization runs
- **Telemetry sheet**: Validation of update rates

### Summary Report:
```
============================================================
FTC DECODE Test Matrix Summary
============================================================
Generated: 2026-01-31 19:45:23
Total Tests: 5

Test Results:
  PASS: 4 (80.0%)
  FAIL: 1 (20.0%)

Performance Metrics:
  Shot Accuracy: 92.3% (avg)
    Best: 95.8%
    Worst: 78.2%
  Collection Success: 94.1% (avg)
    Best: 100.0%
    Worst: 83.3%

Individual Tests:
  [PASS] AUTO_GOAL_0131_1930: Shot accuracy: 95.8%
  [PASS] AUTO_GOAL_0131_1945: Shot accuracy: 93.2%
  [FAIL] AUTO_GOAL_0131_2000: Shot accuracy: 78.2%
  [PASS] AUTO_AUD_0131_2015: Shot accuracy: 94.5%
  [PASS] CHAR_FW_95PCT: Characterization complete
============================================================
```

---

## 🔧 Troubleshooting

### "No test files found"
- Check you pulled from correct directory
- Verify logger.start() was called
- Check `/sdcard/FIRST/test_logs/` on robot

### "Module 'odfpy' not found"
```bash
pip install odfpy pandas numpy
```

### "Can't open ODS file"
- Ensure file isn't open in LibreOffice/Excel
- Check file path is correct
- Try making a backup first

### Test result not showing in matrix
- Verify test completed (logger.stop() called)
- Check CSV has TEST_END event
- Look for errors in Python output

---

## 🚀 Advanced Usage

### Batch Processing Multiple Tests:
```bash
# Pull all logs
adb pull /sdcard/FIRST/test_logs/ ./all_tests/

# Process all at once
python update_test_matrix.py ./all_tests FTC_Matrix.ods -o ./results

# View summary
cat results/test_summary_*.txt
```

### Custom Pass/Fail Criteria:
```kotlin
// In your OpMode
val shotAccuracy = shotsHit.toDouble() / shotsFired * 100
val result = when {
    shotAccuracy >= 95.0 -> TestMatrixLogger.TestResult.PASS
    shotAccuracy >= 85.0 -> TestMatrixLogger.TestResult.MARGINAL
    else -> TestMatrixLogger.TestResult.FAIL
}

testLogger.setResult(result, "Accuracy: $shotAccuracy%")
```

### Statistics for Any Metric:
```kotlin
// After test completes
val stats = testLogger.getStatistics("shot_distance")
println("Shot distance: ${stats?.mean} ± ${stats?.stdDev} cm")
```

---

## 📋 Pre-Competition Checklist

Use test matrix to verify before competition:

1. **All Tests Passed?**
   - Check Test Matrix sheet for any FAIL results
   - Address failures before competing

2. **Key Metrics in Range?**
   - Shot accuracy ≥ 90%
   - Collection success ≥ 90%
   - Battery doesn't drop > 2V during match

3. **Systems Validated?**
   - MCS: Path following works
   - PLCS: Shooting accurate
   - AMS: Intake reliable

4. **Characterization Complete?**
   - Flywheel characterized at multiple battery levels
   - Know performance degradation pattern

---

## 🎓 Tips & Best Practices

1. **Test ID Naming**: Use consistent format
   - `AUTO_GOAL_MMDD_HHMM` for autonomous
   - `CHAR_SYSTEM_CONDITION` for characterization
   - `FUNC_FEATURE_VERSION` for functional tests

2. **Always Set Result**: Don't forget to call `setResult()` in `onStop()`

3. **Log Events, Not Just Metrics**: Events tell the story of what happened

4. **Use Descriptive Names**: `turret_settle_time` better than `tst`

5. **Test One Thing at a Time**: Easier to diagnose failures

6. **Keep Tests Short**: 30 seconds is ideal, max 2 minutes

7. **Run Tests Multiple Times**: 3-5 runs show consistency

---

## 📞 Need Help?

See the full documentation in `TEST_MATRIX_AUTOMATION_OVERVIEW.md`

Or check example code in the provided OpMode files!

---

**You're all set!** Start logging tests and watch your matrix fill automatically! 🎯

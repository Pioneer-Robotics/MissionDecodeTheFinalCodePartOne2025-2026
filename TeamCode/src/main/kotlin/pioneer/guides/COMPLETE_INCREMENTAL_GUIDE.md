# COMPLETE INCREMENTAL IMPROVEMENTS GUIDE
**Pioneer Robotics Team 327 - FTC DECODE 2025-2026**

---

## 📦 BASELINE - START HERE

### **What files to use as your starting point:**

**Use the 7 BASELINE files provided with this guide** - these are the files you uploaded at the start of this thread.

These files are your working code that has the issues we're fixing:
- Turret mis-aiming after autonomous (drift problem)
- Shooting accuracy inconsistent
- Ghost balls from spindexer
- Collection reliability issues

**BASELINE FILES PROVIDED:**
```
Constants.kt                - All robot constants
Flywheel.kt                 - Flywheel control and shooting physics
Teleop.kt                   - Main teleop OpMode
TeleopDriver1.kt            - Driver 1 controls (driving)
TeleopDriver2.kt            - Driver 2 controls (shooting/turret)
AudienceSideAuto.kt         - Audience side autonomous
GoalSideAuto.kt             - Goal side autonomous
```

**Copy these to your project:**
```
Constants.kt         → TeamCode/src/main/kotlin/pioneer/Constants.kt
Flywheel.kt          → TeamCode/src/main/kotlin/pioneer/hardware/Flywheel.kt
Teleop.kt            → TeamCode/src/main/kotlin/pioneer/opmodes/teleop/Teleop.kt
TeleopDriver1.kt     → TeamCode/src/main/kotlin/pioneer/opmodes/teleop/drivers/TeleopDriver1.kt
TeleopDriver2.kt     → TeamCode/src/main/kotlin/pioneer/opmodes/teleop/drivers/TeleopDriver2.kt
AudienceSideAuto.kt  → TeamCode/src/main/kotlin/pioneer/opmodes/auto/AudienceSideAuto.kt
GoalSideAuto.kt      → TeamCode/src/main/kotlin/pioneer/opmodes/auto/GoalSideAuto.kt
```

**Note:** You don't have a baseline Pinpoint.kt file, so use whatever you currently have. When you get to FIX A3, you'll fix the theta bug in your existing file.

---

## 🎯 IMPROVEMENT PRIORITY ORDER

Apply changes in this exact order:

**SECTION A: DRIFT CORRECTION** (Critical - fixes turret aiming)
- A1: Manual Odometry Reset
- A2: Automatic AprilTag Drift Correction
- A3: Pinpoint Theta Bug Fix

**SECTION B: TURRET IMPROVEMENTS** (High priority)
- B1: Always-On Turret Option
- B2: Turret Operating Mode Enum

**SECTION C: SHOOTING PRECISION** (Medium-high priority)
- C1: Distance-Based Speed Tuning
- C2: Shooting Tolerance Tightening
- C3: Battery Voltage Compensation

**SECTION D: SPINDEXER RELIABILITY** (Medium priority)
- D1: Intake Confirmation Time
- D2: Ball Loss Timeout
- D3: Spindexer Tolerances (if magnets work well)

**SECTION E: COLLECTION IMPROVEMENTS** (Medium priority)
- E1: Stop-and-Collect Pattern
- E2: Collection Speed Tuning

**SECTION F: TEST AUTOMATION** (Low priority, saves time)
- F1: Test Matrix Logger Setup
- F2: Python ODS Automation

---

# SECTION A: DRIFT CORRECTION (HIGHEST PRIORITY)

## ⚡ FIX A1: Manual Odometry Reset
**Problem:** After autonomous, odometry has 10-20cm drift, turret mis-aims  
**Time:** 15 minutes  
**Impact:** Driver can instantly fix drift mid-match

### **File:** `TeleopDriver1.kt`

**1. Add import at top:**
```kotlin
import pioneer.decode.Points
```

**2. Add these properties after existing properties (around line 20-30):**
```kotlin
class TeleopDriver1(
    private val gamepad: Gamepad,
    private val bot: Bot,
    private val driver2: TeleopDriver2  // ✅ ADD THIS if not present
) {
    // ... existing properties ...
    
    // ✅ NEW: Manual reset
    private var lastResetTime = 0L
    private val RESET_COOLDOWN_MS = 2000L
    private lateinit var P: Points  // ✅ ADD THIS
```

**3. Add initialization in init or update:**
```kotlin
// ✅ NEW: Initialize field points based on alliance
fun updateFieldPoints() {
    P = when (bot.allianceColor) {
        AllianceColor.RED -> Points(AllianceColor.RED)
        AllianceColor.BLUE -> Points(AllianceColor.BLUE)
        else -> Points(AllianceColor.RED)
    }
}
```

**4. Add manual reset logic in `update()` function, BEFORE other controls:**
```kotlin
fun update() {
    // ✅ NEW: Manual odometry reset with BACK + START + D-pad
    if (gamepad.back && gamepad.start) {
        val currentTime = System.currentTimeMillis()
        
        // Cooldown to prevent double-resets
        if (currentTime - lastResetTime > RESET_COOLDOWN_MS) {
            // Select position based on D-pad
            val resetPose = when {
                gamepad.dpad_up -> P.SHOOT_GOAL_CLOSE      // Shooting position
                gamepad.dpad_down -> P.START_GOAL          // Starting zone
                gamepad.dpad_left -> P.COLLECT_AUDIENCE    // Audience collection
                gamepad.dpad_right -> P.COLLECT_GOAL       // Goal collection
                else -> null
            }
            
            if (resetPose != null) {
                // Reset odometry
                bot.pinpoint?.reset(resetPose)
                
                // Sync turret offsets
                driver2.resetTurretOffsets()
                
                // Haptic feedback
                gamepad.rumble(500)
                
                lastResetTime = currentTime
                
                FileLogger.info("TeleopDriver1", "Manual reset to: $resetPose")
            }
        }
    }
    
    // ... rest of existing update() code ...
}
```

### **Testing:**
1. Run autonomous (let drift accumulate)
2. In teleop, position robot at known location (e.g., shooting position)
3. Hold D-pad UP, press BACK + START
4. Check if turret now aims correctly at goal
5. Try different positions (D-pad DOWN, LEFT, RIGHT)

### **Record:**
- Works? YES / NO
- Turret aims correctly after reset? YES / NO
- Easy for drivers to use? YES / NO
- Keep? YES / NO

---

## ⚡ FIX A2: Automatic AprilTag Drift Correction
**Problem:** Drift accumulates during teleop, manual resets are annoying  
**Time:** 20 minutes  
**Impact:** Automatic continuous drift correction using camera

### **File:** `Teleop.kt`

**1. Add imports at top:**
```kotlin
import org.firstinspires.ftc.vision.apriltag.AprilTagProcessor
import pioneer.decode.GoalTagProcessor
import kotlin.math.*
```

**2. Add properties after existing properties:**
```kotlin
class Teleop : BaseOpMode() {
    // ... existing properties ...
    
    // ✅ NEW: AprilTag drift correction
    private var enableAprilTagCorrection = true
    private var lastCorrectionTime = 0L
    private val CORRECTION_INTERVAL_MS = 500L
    private var visionBlendFactor = 0.3  // 30% vision, 70% odometry
    private val MIN_TAG_CONFIDENCE = 0.7
    private val MAX_CORRECTION_DISTANCE = 20.0  // cm
```

**3. Add in `onLoop()` function, AFTER driver updates:**
```kotlin
override fun onLoop() {
    // Update gamepad inputs
    driver1.update()
    driver2.update()
    
    // ✅ NEW: Automatic AprilTag drift correction
    if (enableAprilTagCorrection) {
        correctOdometryWithAprilTags()
    }
    
    // ✅ NEW: Toggle drift correction with gamepad1.left_stick_button
    if (gamepad1.left_stick_button) {
        enableAprilTagCorrection = !enableAprilTagCorrection
        gamepad1.rumble(200)
    }
    
    // Add telemetry data
    addTelemetryData()
}
```

**4. Add this NEW function anywhere in the class:**
```kotlin
// ✅ NEW: Automatic drift correction using AprilTags
private fun correctOdometryWithAprilTags() {
    val currentTime = System.currentTimeMillis()
    
    // Rate limiting
    if (currentTime - lastCorrectionTime < CORRECTION_INTERVAL_MS) {
        return
    }
    
    // Get AprilTag detections
    val detections = bot.camera?.getProcessor<AprilTagProcessor>()?.detections
    
    if (detections.isNullOrEmpty()) {
        return
    }
    
    // Get current odometry pose
    val currentPose = bot.pinpoint?.pose ?: return
    
    // Filter for high-quality detections of goal tags only
    val goodDetections = detections.filter { detection ->
        detection.ftcPose != null &&
                detection.hamming <= 2 &&
                detection.decisionMargin > MIN_TAG_CONFIDENCE &&
                GoalTagProcessor.isValidGoalTag(detection.id)  // Only goal tags 20, 24
    }
    
    if (goodDetections.isEmpty()) {
        return
    }
    
    // Use GoalTagProcessor to calculate robot pose from tags
    val robotPoseFromTag = GoalTagProcessor.getRobotFieldPose(goodDetections) ?: return
    
    // Calculate correction distance
    val correctionDistance = sqrt(
        (robotPoseFromTag.x - currentPose.x).pow(2) +
                (robotPoseFromTag.y - currentPose.y).pow(2)
    )
    
    // Ignore corrections that are too large (probably wrong)
    if (correctionDistance > MAX_CORRECTION_DISTANCE) {
        return
    }
    
    // Blend vision pose with odometry pose (Kalman-style)
    val blendedPose = Pose(
        x = (1 - visionBlendFactor) * currentPose.x + visionBlendFactor * robotPoseFromTag.x,
        y = (1 - visionBlendFactor) * currentPose.y + visionBlendFactor * robotPoseFromTag.y,
        theta = blendAngles(currentPose.theta, robotPoseFromTag.theta, visionBlendFactor)
    )
    
    // Reset odometry to blended pose
    bot.pinpoint?.reset(blendedPose)
    
    // Update last correction time
    lastCorrectionTime = currentTime
}

// ✅ NEW: Blend two angles with proper wrapping
private fun blendAngles(angle1: Double, angle2: Double, blendFactor: Double): Double {
    val x1 = cos(angle1)
    val y1 = sin(angle1)
    val x2 = cos(angle2)
    val y2 = sin(angle2)
    
    val xBlended = (1 - blendFactor) * x1 + blendFactor * x2
    val yBlended = (1 - blendFactor) * y1 + blendFactor * y2
    
    return atan2(yBlended, xBlended)
}
```

**5. Add telemetry in `addTelemetryData()`:**
```kotlin
private fun addTelemetryData() {
    // ... existing telemetry ...
    
    // ✅ NEW: Drift correction status
    addTelemetryData("AprilTag Correction", if (enableAprilTagCorrection) "ON" else "OFF", Verbose.INFO)
    val visibleGoalTags = bot.camera?.getProcessor<AprilTagProcessor>()?.detections
        ?.count { GoalTagProcessor.isValidGoalTag(it.id) } ?: 0
    addTelemetryData("Visible Goal Tags", visibleGoalTags, Verbose.INFO)
}
```

### **Testing:**
1. Run autonomous (let drift accumulate)
2. In teleop, position robot where it can see goal tags
3. Watch odometry pose in telemetry - should gradually correct
4. Test turret aiming - should improve automatically
5. Press left stick button to toggle ON/OFF, verify difference

### **Record:**
- Works? YES / NO
- Drift reduces over time? YES / NO
- Turret aiming improves? YES / NO
- Keep enabled by default? YES / NO

---

## ⚡ FIX A3: Pinpoint Theta Bug Fix
**Problem:** Heading drift accumulation due to sign error  
**Time:** 2 minutes  
**Impact:** Less heading drift over time

### **File:** `Pinpoint.kt`

**Find the `reset()` function (around line 60-70):**
```kotlin
override fun reset(pose: Pose) {
    this.pose = pose
    pinpoint.setPosition(Pose2D(DistanceUnit.CM, pose.y, -pose.x, AngleUnit.RADIANS, pose.theta))
    //                                                                                    ↑ WRONG!
}
```

**Change to:**
```kotlin
override fun reset(pose: Pose) {
    this.pose = pose
    pinpoint.setPosition(Pose2D(DistanceUnit.CM, pose.y, -pose.x, AngleUnit.RADIANS, -pose.theta))
    //                                                                                   ↑ FIXED!
}
```

**The negative sign on theta must match the one in `init()`.**

### **Testing:**
1. Build and deploy
2. Run autonomous + teleop
3. Check if heading drift is reduced
4. Monitor theta value in telemetry

### **Record:**
- Works? YES / NO
- Heading drift reduced? YES / NO
- Keep? YES / NO

---

# SECTION B: TURRET IMPROVEMENTS

## ⚡ FIX B1: Turret Operating Mode Enum
**Problem:** Need option for always-on vs manual turret control  
**Time:** 10 minutes  
**Impact:** Flexibility for different driver preferences

### **File:** `Constants.kt`

**1. Add enum at TOP LEVEL (outside any object, with FlywheelOperatingMode):**
```kotlin
package pioneer

// ... existing imports ...

// ✅ NEW: Turret operating mode enum
enum class TurretOperatingMode {
    MANUAL,      // Driver controls turret with gamepad (original behavior)
    ALWAYS_AUTO, // Turret always auto-tracks, no manual override
    TOGGLE_AUTO  // Driver can toggle auto-tracking on/off (hybrid)
}

enum class FlywheelOperatingMode {
    // ... existing ...
}
```

**2. Add to Turret constants object:**
```kotlin
object Turret {
    // ... existing constants ...
    
    // ✅ NEW: Turret operating mode
    @JvmField var OPERATING_MODE = TurretOperatingMode.TOGGLE_AUTO  // Start with toggle mode
```

### **Testing:**
- Just adds the enum, no behavior change yet
- Verify it compiles

### **Record:**
- Compiles? YES / NO

---

## ⚡ FIX B2: Always-On Turret Implementation
**Problem:** Want option for turret to always auto-aim  
**Time:** 15 minutes  
**Impact:** Easier for inexperienced drivers

### **File:** `TeleopDriver2.kt`

**Find the `handleTurret()` function:**
```kotlin
private fun handleTurret() {
    isAutoTracking.toggle(gamepad.cross)  // ← Find this line
    
    // ... rest of function ...
}
```

**Replace ENTIRE `handleTurret()` function with:**
```kotlin
private fun handleTurret() {
    // ✅ UPDATED: Respect turret operating mode
    when (Constants.Turret.OPERATING_MODE) {
        TurretOperatingMode.MANUAL -> {
            // Original behavior - manual control only, no auto-tracking
            isAutoTracking.state = false
            
            if (abs(gamepad.right_stick_x) > 0.02) {
                turretAngle -= gamepad.right_stick_x.toDouble().pow(3) / 10.0
            }
            if (gamepad.right_stick_button) {
                turretAngle = 0.0
            }
            bot.turret?.setAngle(turretAngle)
        }
        
        TurretOperatingMode.ALWAYS_AUTO -> {
            // New behavior - always auto-track, no manual override
            isAutoTracking.state = true
            
            // Still allow offset adjustments with right stick
            if (abs(gamepad.right_stick_x) > 0.02) {
                useAutoTrackOffset = true
                offsetShootingTarget = offsetShootingTarget.rotate(-gamepad.right_stick_x.toDouble().pow(3) / 17.5)
            }
            if (gamepad.right_stick_button) {
                useAutoTrackOffset = false
                offsetShootingTarget = tagShootingTarget
            }
            
            // Auto-track
            finalShootingTarget = if (useAutoTrackOffset) offsetShootingTarget else tagShootingTarget
            bot.turret?.autoTrack(bot.pinpoint?.pose ?: Pose(), finalShootingTarget)
        }
        
        TurretOperatingMode.TOGGLE_AUTO -> {
            // Hybrid behavior - driver can toggle auto-tracking (original + toggle)
            isAutoTracking.toggle(gamepad.cross)
            
            if (isAutoTracking.state) {
                // Auto-tracking mode with offset capability
                if (abs(gamepad.right_stick_x) > 0.02) {
                    useAutoTrackOffset = true
                    offsetShootingTarget = offsetShootingTarget.rotate(-gamepad.right_stick_x.toDouble().pow(3) / 17.5)
                }
                if (gamepad.right_stick_button) {
                    useAutoTrackOffset = false
                    offsetShootingTarget = tagShootingTarget
                }
                
                finalShootingTarget = if (useAutoTrackOffset) offsetShootingTarget else tagShootingTarget
                bot.turret?.autoTrack(bot.pinpoint?.pose ?: Pose(), finalShootingTarget)
            } else {
                // Manual mode
                if (abs(gamepad.right_stick_x) > 0.02) {
                    turretAngle -= gamepad.right_stick_x.toDouble().pow(3) / 10.0
                }
                if (gamepad.right_stick_button) {
                    turretAngle = 0.0
                }
                bot.turret?.setAngle(turretAngle)
            }
        }
    }
}
```

**Add import at top if not present:**
```kotlin
import pioneer.TurretOperatingMode
```

### **Testing:**

**Test Mode 1: MANUAL**
```kotlin
Constants.Turret.OPERATING_MODE = TurretOperatingMode.MANUAL
```
- X button should do nothing
- Right stick controls turret manually
- No auto-tracking available

**Test Mode 2: ALWAYS_AUTO**
```kotlin
Constants.Turret.OPERATING_MODE = TurretOperatingMode.ALWAYS_AUTO
```
- Turret ALWAYS auto-tracks goal
- X button does nothing
- Right stick adds offset
- R3 clears offset

**Test Mode 3: TOGGLE_AUTO** (default)
```kotlin
Constants.Turret.OPERATING_MODE = TurretOperatingMode.TOGGLE_AUTO
```
- X button toggles auto-tracking ON/OFF
- When ON: auto-tracks with offset capability
- When OFF: manual control

### **Record:**
- All 3 modes work? YES / NO
- Which mode do you prefer? MANUAL / ALWAYS_AUTO / TOGGLE_AUTO
- Keep? YES / NO

---

# SECTION C: SHOOTING PRECISION

## ⚡ FIX C1: Distance-Based Speed Tuning System
**Problem:** Want to fine-tune shooting for specific distances  
**Time:** 30 minutes  
**Impact:** Precision shooting at all ranges

### **File:** `Constants.kt`

**Add to Flywheel object:**
```kotlin
object Flywheel {
    // ... existing constants ...
    
    // ✅ NEW: Distance-based speed adjustments
    // Format: Pair(distance in cm, speed multiplier)
    @JvmField var DISTANCE_SPEED_MAP = mutableListOf(
        Pair(60.0, 1.00),   // Close range - baseline
        Pair(100.0, 1.00),  // Mid range - baseline
        Pair(140.0, 1.00),  // Long range - baseline
        Pair(180.0, 1.00),  // Very long range - baseline
    )
    
    // ✅ NEW: Global speed adjustment (multiply all speeds)
    @JvmField var GLOBAL_SPEED_MULTIPLIER = 1.00  // Start at no adjustment
```

### **File:** `Flywheel.kt`

**Find `estimateVelocity()` function, at the END before return:**
```kotlin
fun estimateVelocity(
    pose: Pose,
    target: Pose,
    targetHeight: Double
): Double {
    // ... existing physics calculation ...
    
    val flywheelVelocity = 1.583 * v0 - 9.86811  // From 12/22 testing
    
    // ✅ NEW: Apply distance-based adjustment
    var adjustedVelocity = flywheelVelocity
    
    // Find closest distance bracket
    val distance = pose distanceTo target
    val closestEntry = Constants.Flywheel.DISTANCE_SPEED_MAP.minByOrNull { 
        abs(it.first - distance) 
    }
    
    if (closestEntry != null) {
        adjustedVelocity *= closestEntry.second
    }
    
    // Apply global multiplier
    adjustedVelocity *= Constants.Flywheel.GLOBAL_SPEED_MULTIPLIER
    
    return adjustedVelocity
}
```

### **Testing Procedure:**

**Step 1: Establish baselines**
1. Set `GLOBAL_SPEED_MULTIPLIER = 1.00`
2. Test at each distance (60, 100, 140, 180cm)
3. Record results:

```
Distance: 60cm
Shots: ___ over | ___ in | ___ short (out of 10)
Adjustment needed: ___

Distance: 100cm
Shots: ___ over | ___ in | ___ short (out of 10)
Adjustment needed: ___

Distance: 140cm
Shots: ___ over | ___ in | ___ short (out of 10)
Adjustment needed: ___

Distance: 180cm
Shots: ___ over | ___ in | ___ short (out of 10)
Adjustment needed: ___
```

**Step 2: Identify pattern**
- **If ALL distances go over:** Reduce `GLOBAL_SPEED_MULTIPLIER` to 0.90
- **If ALL distances go short:** Increase `GLOBAL_SPEED_MULTIPLIER` to 1.05
- **If mixed:** Adjust individual distance entries

**Step 3: Fine-tune specific distances**

Example if 60cm goes over but 180cm is perfect:
```kotlin
DISTANCE_SPEED_MAP = mutableListOf(
    Pair(60.0, 0.95),   // 5% slower for close range
    Pair(100.0, 1.00),  // Keep baseline
    Pair(140.0, 1.00),  // Keep baseline
    Pair(180.0, 1.00),  // Keep baseline
)
```

**Step 4: Verify**
Re-test all distances with new values.

### **Record:**
- Final GLOBAL_SPEED_MULTIPLIER: _____
- Final DISTANCE_SPEED_MAP values:
  - 60cm: _____
  - 100cm: _____
  - 140cm: _____
  - 180cm: _____
- Overall accuracy: ___%
- Keep? YES / NO

---

## ⚡ FIX C2: Shooting Tolerance Tightening
**Problem:** Turret aims at goal but shooting direction isn't precise  
**Time:** 15 minutes  
**Impact:** Tighter aiming requirements before shooting

### **File:** `Constants.kt`

**Find in Turret object:**
```kotlin
object Turret {
    // ... other constants ...
    
    const val SHOOTING_TOLERANCE_RAD = 0.1  // ← Find this
```

**Test incrementally:**

**Test 1: Current value (0.1 rad = 5.7°)**
- Fire 20 shots
- Hits: ___ / 20 = ___%

**Test 2: Tighter (0.05 rad = 2.9°)**
```kotlin
const val SHOOTING_TOLERANCE_RAD = 0.05
```
- Fire 20 shots
- Hits: ___ / 20 = ___%
- Shots delayed? YES / NO

**Test 3: Very tight (0.03 rad = 1.7°)**
```kotlin
const val SHOOTING_TOLERANCE_RAD = 0.03
```
- Fire 20 shots
- Hits: ___ / 20 = ___%
- Shots delayed? YES / NO

**Test 4: Extremely tight (0.01 rad = 0.6°)**
```kotlin
const val SHOOTING_TOLERANCE_RAD = 0.01
```
- Fire 20 shots
- Hits: ___ / 20 = ___%
- Shots delayed? YES / NO

### **Find optimal balance:**
- Tighter = more accurate BUT might never shoot if tolerance too tight
- Find the tightest value that still allows timely shots

### **Record:**
- Best value: `SHOOTING_TOLERANCE_RAD = _____`
- Accuracy improvement: ___%
- Keep? YES / NO

---

## ⚡ FIX C3: Battery Voltage Compensation
**Problem:** Shooting changes as battery drains  
**Time:** 20 minutes  
**Impact:** Consistent performance throughout match

### **File:** `Constants.kt`

**Add to Flywheel object:**
```kotlin
object Flywheel {
    // ... existing constants ...
    
    // ✅ NEW: Battery compensation
    @JvmField var USE_BATTERY_COMPENSATION = true
    @JvmField var MIN_VOLTAGE_COMPENSATION = 0.90
    @JvmField var MAX_VOLTAGE_COMPENSATION = 1.15
```

### **File:** `Flywheel.kt`

**Change function signature:**
```kotlin
fun estimateVelocity(
    pose: Pose,
    target: Pose,
    targetHeight: Double,
    batteryVoltage: Double = 12.0  // ✅ ADD THIS
): Double {
```

**Add AFTER distance adjustments, BEFORE return:**
```kotlin
    // Apply global multiplier
    adjustedVelocity *= Constants.Flywheel.GLOBAL_SPEED_MULTIPLIER
    
    // ✅ NEW: Battery compensation
    if (Constants.Flywheel.USE_BATTERY_COMPENSATION) {
        val voltageCompensation = 12.0 / batteryVoltage.coerceIn(10.0, 13.5)
        adjustedVelocity *= voltageCompensation.coerceIn(
            Constants.Flywheel.MIN_VOLTAGE_COMPENSATION,
            Constants.Flywheel.MAX_VOLTAGE_COMPENSATION
        )
    }
    
    return adjustedVelocity
```

### **File:** `TeleopDriver2.kt`

**Find estimateVelocity call:**
```kotlin
flywheelSpeed = bot.flywheel!!.estimateVelocity(
    bot.pinpoint?.pose ?: Pose(),
    tagShootingTarget,
    targetGoal.shootingHeight
) + flywheelSpeedOffset
```

**Change to:**
```kotlin
flywheelSpeed = bot.flywheel!!.estimateVelocity(
    bot.pinpoint?.pose ?: Pose(),
    tagShootingTarget,
    targetGoal.shootingHeight,
    bot.batteryMonitor?.voltage ?: 12.0  // ✅ ADD THIS
) + flywheelSpeedOffset
```

### **Testing:**
1. **Full battery (>12.5V):** 10 shots at 100cm → ___ hits
2. **Med battery (~11.5V):** 10 shots at 100cm → ___ hits
3. **Low battery (~10.5V):** 10 shots at 100cm → ___ hits
4. Compare variance

### **Record:**
- Variance with compensation: ___%
- Keep? YES / NO

---

# SECTION D: SPINDEXER RELIABILITY

## ⚡ FIX D1: Intake Confirmation Time
**Problem:** Balls confirmed too quickly, fall out  
**Time:** 10 minutes  
**Impact:** Fewer ghost balls

### **File:** `Constants.kt`

**Current:**
```kotlin
const val CONFIRM_INTAKE_MS = 67.0
```

**Change to:**
```kotlin
const val CONFIRM_INTAKE_MS = 150.0  // ✅ Increased for settling
```

### **Testing:**
1. Collect 10 balls
2. Count ghost balls: ___ / 10

### **Record:**
- Ghost balls reduced? YES / NO
- Keep? YES / NO

---

## ⚡ FIX D2: Ball Loss Timeout
**Problem:** False resets when ball briefly not detected  
**Time:** 5 minutes  
**Impact:** More stable tracking

### **File:** `Constants.kt`

**Current:**
```kotlin
const val CONFIRM_LOSS_MS = 10
```

**Change to:**
```kotlin
const val CONFIRM_LOSS_MS = 50  // ✅ Increased to prevent false resets
```

### **Testing:**
1. Collect 10 balls
2. Watch for unexpected resets

### **Record:**
- More stable? YES / NO
- Keep? YES / NO

---

## ⚡ FIX D3: Spindexer Tolerances (Optional)
**Problem:** Spindexer stops too far from target  
**Time:** 10 minutes  
**Impact:** Better positioning

**⚠️ ONLY IF your magnets work well!**

### **File:** `Constants.kt`

**Test incrementally:**

**Current:**
```kotlin
@JvmField var MOTOR_TOLERANCE_TICKS = 75
const val SHOOTING_TOLERANCE_TICKS = 100
```

**Test 1: Slightly tighter**
```kotlin
@JvmField var MOTOR_TOLERANCE_TICKS = 60
const val SHOOTING_TOLERANCE_TICKS = 80
```
- Test 10 movements
- Oscillation? YES / NO
- Better positioning? YES / NO

**Test 2: Much tighter**
```kotlin
@JvmField var MOTOR_TOLERANCE_TICKS = 50
const val SHOOTING_TOLERANCE_TICKS = 75
```
- Test 10 movements
- Oscillation? YES / NO
- Better positioning? YES / NO

**If oscillation occurs, revert to looser values!**

### **Record:**
- Best values: MOTOR=___ SHOOTING=___
- Keep? YES / NO

---

# SECTION E: COLLECTION IMPROVEMENTS

## ⚡ FIX E1: Stop-and-Collect Pattern
**Problem:** Balls ejected during collection  
**Time:** 30 minutes  
**Impact:** Much higher collection success

*(This is the full implementation from the earlier guide)*

### **Record:**
- Collections before: ___ / 3
- Collections after: ___ / 3
- Keep? YES / NO

---

## ⚡ FIX E2: Collection Speed Tuning
**Problem:** Robot moving too fast during collection  
**Time:** 10 minutes  
**Impact:** More reliable intake

### **File:** `AudienceSideAuto.kt`

**Find collection paths, change speeds:**

**Current:**
```kotlin
bot.follower.followPath(LinearPath(..., P.COLLECT_GOAL), 6.7)
```

**Test speeds:**
- 10.0 cm/s - Very slow, very reliable
- 8.0 cm/s - Slow, reliable  
- 6.7 cm/s - Current
- 5.0 cm/s - Very slow

Find best balance of speed vs reliability.

### **Record:**
- Best speed: _____ cm/s
- Collections: ___ / 10
- Keep? YES / NO

---

# SECTION F: TEST AUTOMATION

## ⚡ FIX F1: Test Matrix Logger
**Problem:** Manual data entry takes forever  
**Time:** 1 hour setup  
**Impact:** Saves hours over season

*(Full setup from test matrix guides)*

### **Record:**
- Setup complete? YES / NO
- Time saved: ___ min per test
- Keep? YES / NO

---

# 📊 MASTER TRACKING TABLE

| Fix | Name | Applied? | Tested? | Keep? | Notes |
|-----|------|----------|---------|-------|-------|
| A1 | Manual Reset | ☐ | ☐ | ☐ | |
| A2 | Auto Correction | ☐ | ☐ | ☐ | |
| A3 | Theta Bug | ☐ | ☐ | ☐ | |
| B1 | Turret Enum | ☐ | ☐ | ☐ | |
| B2 | Always-On Turret | ☐ | ☐ | ☐ | Mode: _____ |
| C1 | Distance Tuning | ☐ | ☐ | ☐ | Global: _____ |
| C2 | Shooting Tolerance | ☐ | ☐ | ☐ | Value: _____ |
| C3 | Battery Comp | ☐ | ☐ | ☐ | |
| D1 | Intake Time | ☐ | ☐ | ☐ | |
| D2 | Loss Timeout | ☐ | ☐ | ☐ | |
| D3 | Tolerances | ☐ | ☐ | ☐ | Values: _____ |
| E1 | Stop-Collect | ☐ | ☐ | ☐ | |
| E2 | Collection Speed | ☐ | ☐ | ☐ | Speed: _____ |
| F1 | Test Logger | ☐ | ☐ | ☐ | |

---

# 🎯 RECOMMENDED ORDER

**Day 1 - Critical fixes:**
1. A1 - Manual Reset (15 min) ⚡
2. A2 - Auto Correction (20 min) ⚡⚡
3. A3 - Theta Bug (2 min) ⚡
4. **TEST TURRET THOROUGHLY**

**Day 2 - Turret modes:**
1. B1 - Turret Enum (10 min)
2. B2 - Always-On Option (15 min)
3. **TEST ALL 3 MODES**

**Day 3 - Shooting precision:**
1. C1 - Distance Tuning (30 min)
2. C2 - Tolerance Tightening (15 min)
3. C3 - Battery Compensation (20 min)

**Day 4 - Polish:**
1. D1, D2, D3 - Spindexer fixes
2. E1, E2 - Collection improvements
3. F1 - Test automation (if time)

---

**START WITH SECTION A - DRIFT CORRECTION!** This fixes your biggest problem (turret mis-aiming). Everything else builds on this foundation. 🎯

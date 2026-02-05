# Teleop Refactor - Architecture Documentation

## Overview

This refactor completely redesigns the teleop driver architecture using **SOLID principles** and the **Command Pattern**. The goal is to fix button handling issues and make the code more maintainable, testable, and extensible.

---

## The Problem We're Solving

### **Button Timing Issues**

The original code had a subtle but critical bug:

```kotlin
// ORIGINAL CODE (Driver2)
private fun updateFlywheelSpeed() {
    isEstimatingSpeed.toggle(gamepad.dpad_left)  // Toggle happens
    
    if (!isEstimatingSpeed.state) {
        if (gamepad.dpad_up) { ... }    // These checks happen AFTER toggle
        if (gamepad.dpad_down) { ... }  // Timing issues possible!
    }
}
```

**Problem:** If the toggle happens, the subsequent button checks might be affected by the state change happening mid-frame. The `Toggle` class also resets `justChanged` on each call, so if multiple methods check the same toggle, only the first one sees the change.

### **Additional Issues**

1. **Mixed Responsibilities:** Methods like `handleTurret()` both read inputs AND execute actions
2. **Hard to Test:** Can't test individual actions without a full gamepad
3. **Hard to Debug:** Unclear which input caused which action
4. **Hard to Extend:** Adding new features risks breaking existing ones
5. **No Input Validation:** No check for conflicting button presses

---

## The Solution: Command Pattern with Input Mapping

### **Architecture Layers**

```
┌─────────────────────────────────────────────────────────────┐
│                     TeleopDriver1/2                         │
│  Orchestrates the update loop, handles errors gracefully   │
└────────────────────────┬────────────────────────────────────┘
                         │ uses
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                      InputState                             │
│  Immutable snapshot of all button/stick states for 1 frame │
│  Provides edge detection (isPressed, isReleased, isHeld)   │
└────────────────────────┬────────────────────────────────────┘
                         │ passed to
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                 Driver1/2 InputMapper                       │
│  Maps button combinations to Commands with Context         │
│  Maintains driver state (toggles, speeds, angles, etc.)    │
└────────────────────────┬────────────────────────────────────┘
                         │ returns
                         ▼
┌─────────────────────────────────────────────────────────────┐
│              List<Pair<Command, Context>>                   │
│  Commands to execute this frame with their parameters      │
└────────────────────────┬────────────────────────────────────┘
                         │ executed on
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                         Bot                                 │
│  Robot hardware abstraction (unchanged)                     │
└─────────────────────────────────────────────────────────────┘
```

### **Key Design Decisions**

#### **1. InputState is Immutable**

```kotlin
data class InputState(
    val dpadUp: Boolean,
    val dpadDown: Boolean,
    // ... all other buttons
    private val previous: InputState? = null  // For edge detection
)
```

**Why:** Captured ONCE per frame, so all code sees the SAME input state. No timing issues possible!

#### **2. Commands are Stateless and Reusable**

```kotlin
interface Command {
    fun execute(bot: Bot, context: CommandContext)
}

class StartIntakeCommand : Command {
    override fun execute(bot: Bot, context: CommandContext) {
        bot.intake?.forward()
    }
}
```

**Why:** Single Responsibility - each command does ONE thing. Easy to test, debug, and reuse.

#### **3. InputMapper Holds State**

```kotlin
class Driver2InputMapper {
    private var flywheelEnabled = false
    private var manualFlywheelSpeed = 0.0
    // ... other state
    
    override fun mapInputsToCommands(input: InputState, bot: Bot): List<Pair<Command, Context>>
}
```

**Why:** Separates button logic from actions. All button handling in ONE place with ONE view of inputs.

#### **4. CommandContext Carries Parameters**

```kotlin
data class CommandContext(
    val analogValue: Double = 0.0,
    val vectorX: Double = 0.0,
    val vectorY: Double = 0.0,
    val parameters: Map<String, Any> = emptyMap()
)
```

**Why:** Commands can receive analog values, poses, etc. without coupling to specific gamepad layout.

---

## How the Refactor Fixes Button Issues

### **Original Code Flow (Buggy)**

```
Frame N:
  updateFlywheelSpeed() called
    ├─ Toggle checks dpad_left → state changes
    ├─ Check dpad_up → affected by toggle?
    └─ Check dpad_down → affected by toggle?
  handleTurret() called
    ├─ Toggle checks cross → state changes
    └─ Check right_stick_x → affected by toggle?
```

**Problem:** Multiple state changes mid-frame, button checks can interfere with each other.

### **New Code Flow (Fixed)**

```
Frame N:
  1. Capture InputState (IMMUTABLE snapshot)
     ├─ dpad_left: true
     ├─ dpad_up: false
     ├─ dpad_down: true
     └─ ... all other buttons
  
  2. InputMapper.mapInputsToCommands(input, bot)
     ├─ if (input.isPressed(DPAD_LEFT)) → toggle state
     ├─ if (input.isPressed(DPAD_UP)) → adjust speed
     └─ if (input.isPressed(DPAD_DOWN)) → adjust speed
     
     All checks see the SAME input snapshot!
     No timing issues possible!
  
  3. Execute all commands
```

**Solution:** Input captured once, all logic sees consistent state, no mid-frame corruption.

---

## Code Organization

### **File Structure**

```
pioneer/opmodes/teleop/
├── Teleop.kt                           (unchanged - main coordinator)
├── drivers/
│   ├── TeleopDriver1_Refactored.kt    (NEW - clean driver class)
│   └── TeleopDriver2_Refactored.kt    (NEW - clean driver class)
├── input/
│   ├── InputState.kt                   (NEW - input snapshot)
│   ├── Driver1InputMapper.kt          (NEW - driver 1 button mapping)
│   └── Driver2InputMapper.kt          (NEW - driver 2 button mapping)
└── commands/
    └── Commands.kt                     (NEW - all command implementations)
```

### **What Each File Does**

- **InputState.kt:** Captures gamepad state, provides edge detection
- **Commands.kt:** Individual robot actions (drive, shoot, intake, etc.)
- **Driver1/2InputMapper.kt:** Maps buttons to commands, holds driver state
- **TeleopDriver1/2_Refactored.kt:** Orchestrates input → commands → execution

---

## Migration Guide

### **Step 1: Add New Files**

Copy these files to your project:
1. `InputState.kt` → `pioneer/opmodes/teleop/input/`
2. `Commands.kt` → `pioneer/opmodes/teleop/commands/`
3. `Driver1InputMapper.kt` → `pioneer/opmodes/teleop/input/`
4. `Driver2InputMapper.kt` → `pioneer/opmodes/teleop/input/`
5. `TeleopDriver1_Refactored.kt` → `pioneer/opmodes/teleop/drivers/`
6. `TeleopDriver2_Refactored.kt` → `pioneer/opmodes/teleop/drivers/`

### **Step 2: Update Teleop.kt**

Change imports:
```kotlin
// OLD
import pioneer.opmodes.teleop.drivers.TeleopDriver1
import pioneer.opmodes.teleop.drivers.TeleopDriver2

// NEW - just rename the files or update imports
// No other changes needed to Teleop.kt!
```

### **Step 3: Test Incrementally**

1. **Test Driver1 first:**
   - Deploy and test all drive controls
   - Test intake, spindexer, odometry reset
   - Verify telemetry still works

2. **Test Driver2:**
   - Test flywheel speed controls (dpad up/down/left/right)
   - Test turret modes
   - Test shooting sequence
   - **Pay attention to dpad buttons - these were the problematic ones!**

3. **Verify button fixes:**
   - Rapidly press dpad buttons - they should respond every time
   - Try button combinations - no conflicts
   - Check LED indicators update correctly

### **Step 4: Remove Old Code**

Once testing is complete, you can delete:
- `TeleopDriver1.kt` (original)
- `TeleopDriver2.kt` (original)
- Rename `_Refactored` files to remove the suffix

---

## Benefits of the Refactor

### **Immediate Benefits**

✅ **Button timing issues fixed** - Input captured once per frame
✅ **No more missed button presses** - Reliable edge detection
✅ **Clear error handling** - Failed commands don't crash everything
✅ **Better telemetry** - Can log which commands are executing

### **Long-term Benefits**

✅ **Easy to add new controls** - Just add a new command and map it
✅ **Easy to remap buttons** - Change the mapper, not the driver
✅ **Easy to test** - Commands are testable without a robot
✅ **Easy to debug** - Clear separation of input → mapping → actions
✅ **Team-friendly** - New programmers can understand the flow

### **SOLID Principles Achieved**

- **S**ingle Responsibility: Each class has one job
- **O**pen/Closed: Easy to extend without modifying existing code
- **L**iskov Substitution: Can swap mappers for different control schemes
- **I**nterface Segregation: Clean interfaces for Input, Commands, Mapping
- **D**ependency Inversion: Depends on abstractions (Bot, Command) not concrete classes

---

## Advanced Features

### **Adding a New Control**

Want to add a new button for something?

1. **Create a command:**
```kotlin
class MyNewActionCommand : Command {
    override fun execute(bot: Bot, context: CommandContext) {
        // Do the thing
    }
}
```

2. **Map it in the InputMapper:**
```kotlin
// In Driver2InputMapper
private val myNewCommand = MyNewActionCommand()

override fun mapInputsToCommands(...) {
    // ... existing code ...
    
    if (input.isPressed(Button.SOME_BUTTON)) {
        commands.add(myNewCommand to CommandContext.EMPTY)
    }
}
```

That's it! No changes to driver classes, no risk of breaking existing controls.

### **Remapping Buttons**

Create a `Driver1AlternateInputMapper` with different mappings, then just swap:
```kotlin
// In TeleopDriver1
private val inputMapper = Driver1AlternateInputMapper(gamepad)  // Easy!
```

### **Debugging Button Issues**

Add logging to see exactly what's happening:
```kotlin
// In TeleopDriver1.update()
for ((command, context) in commandsToExecute) {
    FileLogger.debug("Driver1", "Executing: ${command.description()}")
    command.execute(bot, context)
}
```

---

## Testing Checklist

### **Driver 1 Controls**

- [ ] Drive with left stick (forward/backward/strafe)
- [ ] Rotate with right stick
- [ ] Increase drive power (right bumper)
- [ ] Decrease drive power (left bumper)
- [ ] Toggle field-centric (touchpad)
- [ ] Toggle intake (circle)
- [ ] Reverse intake (dpad down)
- [ ] Manual spindexer control (triggers)
- [ ] Reset spindexer (share)
- [ ] Reset odometry (options)

### **Driver 2 Controls (CRITICAL - These Were Broken)**

- [ ] Toggle flywheel auto/manual mode (dpad left)
- [ ] Increase flywheel speed (dpad up) - TEST THIS!
- [ ] Decrease flywheel speed (dpad down) - TEST THIS!
- [ ] Toggle flywheel on/off (dpad right) - TEST THIS!
- [ ] Reset speed offset (left stick button)
- [ ] Toggle turret mode (cross)
- [ ] Manual turret control (right stick)
- [ ] Auto-track offset (right stick in auto mode)
- [ ] Reset turret offset (right stick button)
- [ ] Select purple artifact (right bumper)
- [ ] Select green artifact (left bumper)
- [ ] Select any artifact (triangle)
- [ ] Launch (square)
- [ ] LED indicators update correctly

### **Integration Tests**

- [ ] Driver 1 odometry reset syncs with Driver 2 turret
- [ ] Telemetry displays correct values
- [ ] No errors in log files
- [ ] Gamepad LEDs work
- [ ] All existing functionality preserved

---

## Troubleshooting

### **"Buttons still not working!"**

1. Check the InputState is being created correctly
2. Add logging to see which commands are being generated
3. Verify the command is actually executing
4. Check for exceptions in the error log

### **"Telemetry shows wrong values"**

Update Teleop.kt to use the new accessor methods:
```kotlin
// OLD
addTelemetryData("Flywheel Speed", driver2.flywheelSpeed)

// NEW
addTelemetryData("Flywheel Speed", bot.flywheel?.velocity)
```

### **"Compilation errors"**

Make sure all files are in the correct packages and imports are correct.

---

## Questions?

See the inline comments in the code for detailed explanations of each component!

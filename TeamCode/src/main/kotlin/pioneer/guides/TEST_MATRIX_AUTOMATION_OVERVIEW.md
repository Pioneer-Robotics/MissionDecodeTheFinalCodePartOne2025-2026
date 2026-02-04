# AUTOMATED TEST MATRIX POPULATION SYSTEM
## FTC DECODE 2025-2026 Requirements Testing

**Purpose:** Automatically capture robot performance data and populate the Requirements Test Matrix spreadsheet

---

## SYSTEM OVERVIEW

### What This Does:

1. **Captures telemetry data** during OpMode execution
2. **Records test conditions** (battery, distance, speeds, etc.)
3. **Logs performance metrics** (accuracy, timing, success rates)
4. **Exports to CSV** for easy analysis
5. **Generates ODS file** compatible with your test matrix
6. **Auto-fills test results** into the spreadsheet

### What Gets Tracked:

**From Test Matrix Sheet:**
- Test ID, Motion type, Power state
- Objectives and systems under test
- Pass/Fail criteria
- Test conditions and results

**From Characterization Sheet:**
- Battery status
- Flywheel speeds
- Launch angles
- Dependent variables (range, height, velocity)

**From Telemetry Sheet:**
- All subsystem parameters
- Update rates
- Value ranges
- Engineering units

---

## ARCHITECTURE

```
OpMode (Test)
    ↓
TestMatrixLogger (Kotlin)
    ↓
CSV File (on robot)
    ↓
Python Script (on laptop)
    ↓
Updated ODS File (your test matrix)
```

---

## FILES OVERVIEW

1. **TestMatrixLogger.kt** - Captures data during OpMode
2. **TestMatrixConfig.kt** - Test definitions and pass/fail criteria
3. **TestRunner.kt** - OpMode for running specific tests
4. **export_test_data.py** - Converts CSV → ODS
5. **analysis_dashboard.py** - Visual analysis of results

---

## IMPLEMENTATION

### Part 1: Test Matrix Logger (Kotlin)

This captures data during any OpMode execution.

### Part 2: Test Configuration

Defines what tests exist and their criteria.

### Part 3: Dedicated Test Runner OpMode

Structured test execution with prompts.

### Part 4: Python Export Tool

Converts CSV logs to ODS format for your matrix.

### Part 5: Analysis Dashboard (Bonus)

Visual dashboard for tracking progress.

---

## USAGE WORKFLOWS

### Workflow 1: Manual Testing with Auto-Logging

**Use Case:** Running teleop or autonomous normally, want to capture performance data

**Steps:**
1. Add logger to your existing OpMode
2. Run normally
3. Logger captures telemetry automatically
4. Pull CSV from robot
5. Run Python script to update ODS

### Workflow 2: Structured Test Execution

**Use Case:** Running specific requirement tests from test matrix

**Steps:**
1. Select test from list on driver station
2. System prompts for test conditions
3. Execute test
4. System records pass/fail automatically
5. Move to next test

### Workflow 3: Characterization Testing

**Use Case:** Characterizing flywheel at different battery levels and speeds

**Steps:**
1. Launch CharacterizationTest OpMode
2. System guides through battery%, speed, angle combinations
3. Measures spin-up time, velocity, range, height
4. Records all data automatically
5. Exports complete characterization matrix

---

## TRACKED METRICS

### Motion Control System (MCS):
- Drivetrain motor currents
- Velocity commands vs actual
- Acceleration capabilities
- Path following accuracy
- Position error statistics

### Projectile Launch Control System (PLCS):
- Flywheel motor current
- Flywheel velocity (target vs actual)
- Spin-up time
- Launch angle
- Shot accuracy (distance to target)
- Turret positioning accuracy

### Imaging System (IS):
- AprilTag detection rate
- Image processing time
- Tag confidence scores
- Drift correction accuracy

### Artifact Management System (AMS):
- Intake success rate
- Spindexer positioning accuracy
- Collection cycle time
- Ghost ball detection rate

---

## TEST MATRIX MAPPING

### Requirements Sheet → Auto-Population:

**Mission Performance Objective:** "Launch anywhere in designated launch zone"

**Logged Data:**
- Robot position when shooting (from Pinpoint)
- Distance to goal
- Shot outcome (hit/miss)
- Calculated accuracy %

**Auto-Fill:** Pass/Fail based on >90% accuracy threshold

### Test Matrix Sheet → Auto-Population:

**Test ID:** Auto-generated based on test type and date

**Systems:** Selected from dropdown or auto-detected

**Results:** Pass/Fail + detailed metrics in notes

### Characterization Sheet → Auto-Population:

**Battery Status:** From BatteryMonitor

**Launcher Speed Cmd:** From Flywheel target velocity

**Spin-up Time:** Measured automatically

**Range:** Calculated from shot landing position

### Telemetry Sheet → Validation:

**Update Rate:** Verified during test execution

**Range:** Min/Max values recorded

**Actual vs Specified:** Comparison and warnings

---

## DATA EXPORT FORMATS

### CSV Format (Intermediate):
```csv
timestamp,test_id,test_type,system,metric_name,metric_value,pass_fail,notes
2026-01-31T19:30:15,TEST_001,Characterization,PLCS,spin_up_time,0.85,PASS,Battery 95%
2026-01-31T19:30:15,TEST_001,Characterization,PLCS,shot_range,142.5,PASS,Distance: 120cm target
```

### ODS Output:
- Directly populates cells in test matrix
- Preserves formatting
- Adds timestamps
- Highlights pass/fail
- Appends detailed notes

---

## SETUP INSTRUCTIONS

### On Robot (Android Studio):

1. Add `TestMatrixLogger.kt` to `pioneer/helpers/`
2. Add `TestMatrixConfig.kt` to `pioneer/testing/`
3. Add `TestRunner.kt` to `pioneer/opmodes/testing/`
4. Integrate logger into existing OpModes
5. Build and deploy

### On Laptop (Python):

1. Install dependencies:
   ```bash
   pip install odfpy pandas numpy matplotlib
   ```

2. Copy scripts to project directory
3. Configure paths in scripts
4. Ready to use!

---

## NEXT STEPS

I'll create the complete implementation files:

1. **TestMatrixLogger.kt** - Core logging system
2. **TestMatrixConfig.kt** - Test definitions
3. **TestRunner.kt** - Test execution OpMode
4. **CharacterizationTest.kt** - Automated characterization
5. **export_test_data.py** - CSV to ODS converter
6. **update_test_matrix.py** - Direct ODS updater
7. **analysis_dashboard.py** - Visual progress tracker

Would you like me to create all of these now?

---

## EXAMPLE OUTPUT

### After Running Tests:

**Console Output:**
```
Test Matrix Export Complete!
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Tests Run: 12
Passed: 10 (83.3%)
Failed: 2
Updated: FTC_2025-26_Decode_Requirements_TestMatrix.ods

Failed Tests:
  - TEST_005: Shot accuracy 78% (threshold: 90%)
  - TEST_008: Intake success 85% (threshold: 90%)

Next Recommended Tests:
  - Flywheel characterization at 75% battery
  - Spindexer settling time validation
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

**Updated ODS File:**
- Test Matrix sheet: New rows added with results
- Characterization sheet: Data filled in appropriate cells
- Telemetry sheet: Validated against actual performance
- Color-coded: Green (pass), Red (fail), Yellow (marginal)

---

## ADVANCED FEATURES

### 1. Auto-Detection of Test Scenarios
- Recognizes when you're shooting
- Identifies collection attempts
- Detects autonomous vs teleop
- Tags tests automatically

### 2. Statistical Analysis
- Mean, std dev, min/max for all metrics
- Trend analysis over time
- Battery voltage correlation
- Performance degradation detection

### 3. Requirement Traceability
- Links test results to requirements
- Tracks coverage %
- Highlights untested requirements
- Generates compliance reports

### 4. Visual Dashboard
- Real-time test progress
- Pass/fail pie charts
- Performance trend graphs
- System health indicators

### 5. Pre-Competition Checklist
- Auto-generates checklist from test results
- Highlights areas needing attention
- Tracks mandatory vs optional tests
- Competition readiness score

---

## INTEGRATION WITH EXISTING SYSTEMS

### Works With Your Current Code:
- Uses existing `FileLogger` pattern
- Compatible with `OdometryLogger`
- Integrates with `FlywheelLogger`
- Extends `BaseOpMode`

### Minimal Changes Required:
- Add 3-5 lines to OpModes
- No breaking changes
- Optional features - use what you need
- Can disable for competition

---

Ready to proceed with full implementation? I'll create all the files!

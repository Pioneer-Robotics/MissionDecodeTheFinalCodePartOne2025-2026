package pioneer.hardware

import android.content.Context
import com.qualcomm.robotcore.hardware.CRServoImpl
import com.qualcomm.robotcore.hardware.ColorSensor
import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorController
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.DistanceSensor
import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.I2cAddr
import com.qualcomm.robotcore.hardware.PIDCoefficients
import com.qualcomm.robotcore.hardware.PIDFCoefficients
import com.qualcomm.robotcore.hardware.ServoImpl
import com.qualcomm.robotcore.hardware.VoltageSensor
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType
import com.qualcomm.robotcore.util.SerialNumber
import org.firstinspires.ftc.robotcore.external.function.Consumer
import org.firstinspires.ftc.robotcore.external.function.Continuation
import org.firstinspires.ftc.robotcore.external.hardware.camera.CameraCharacteristics
import org.firstinspires.ftc.robotcore.external.hardware.camera.WebcamName
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.internal.system.Deadline

/**
 * MockHardware is a mock implementation of an FTC HardwareMap for testing purposes.
 */
class MockHardware : HardwareMap(null, null) {
    init {
        // Add mock DcMotors for mecanum drive
        dcMotor.put("driveLF", MockDcMotorEx())
        dcMotor.put("driveLB", MockDcMotorEx())
        dcMotor.put("driveRF", MockDcMotorEx())
        dcMotor.put("driveRB", MockDcMotorEx())

        dcMotor.put("flywheel", MockDcMotorEx())

        // Add mock CRServos (continuous rotation servos)
        crservo.put("launchServoL", MockCRServo())
        crservo.put("launchServoR", MockCRServo())

        // Add mock VoltageSensor
        voltageSensor.put("voltageSensor", MockVoltageSensor())

        // Add mock Webcam (minimal implementation for initialization)
        put("Webcam 1", MockWebcamName())
    }

    // Minimal WebcamName implementation for mock initialization
    private class MockWebcamName : WebcamName {
        override fun getManufacturer() = HardwareDevice.Manufacturer.Other

        override fun getDeviceName() = "MockWebcam"

        override fun getConnectionInfo() = "Mock"

        override fun getVersion() = 1

        override fun resetDeviceConfigurationForOpMode() {}

        override fun close() {}

        override fun isWebcam() = true

        override fun isCameraDirection() = false

        override fun isSwitchable() = false

        override fun isUnknown() = false

        override fun asyncRequestCameraPermission(
            context: Context?,
            deadline: Deadline?,
            continuation: Continuation<out Consumer<Boolean>?>?,
        ) {
            // Mock implementation - do nothing
        }

        override fun requestCameraPermission(deadline: Deadline?): Boolean {
            // Mock implementation - always grant permission
            return true
        }

        override fun getCameraCharacteristics(): CameraCharacteristics? {
            // Mock implementation - return null
            return null
        }

        override fun getSerialNumber(): SerialNumber = SerialNumber.createFake()

        override fun getUsbDeviceNameIfAttached(): String? = null

        override fun isAttached(): Boolean = false
    }

    private class MockDcMotorEx : DcMotorEx {
        private var power: Double = 0.0
        private var position: Int = 0
        private var targetPosition: Int = 0
        private var mode: DcMotor.RunMode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
        private var direction: DcMotorSimple.Direction = DcMotorSimple.Direction.FORWARD
        private var zeroPowerBehavior: DcMotor.ZeroPowerBehavior = DcMotor.ZeroPowerBehavior.FLOAT

        // DcMotorEx specific methods
        override fun setMotorEnable() {}

        override fun setMotorDisable() {}

        override fun isMotorEnabled(): Boolean = true

        override fun setVelocity(angularRate: Double) {}

        override fun setVelocity(
            angularRate: Double,
            unit: AngleUnit,
        ) {}

        override fun getVelocity(): Double = 0.0

        override fun getVelocity(unit: AngleUnit): Double = 0.0

        override fun setPIDCoefficients(
            mode: DcMotor.RunMode,
            pidCoefficients: PIDCoefficients,
        ) {}

        override fun setPIDFCoefficients(
            mode: DcMotor.RunMode,
            pidfCoefficients: PIDFCoefficients,
        ) {}

        override fun setVelocityPIDFCoefficients(
            p: Double,
            i: Double,
            d: Double,
            f: Double,
        ) {}

        override fun setPositionPIDFCoefficients(p: Double) {}

        override fun getPIDCoefficients(mode: DcMotor.RunMode): PIDCoefficients = PIDCoefficients(0.0, 0.0, 0.0)

        override fun getPIDFCoefficients(mode: DcMotor.RunMode): PIDFCoefficients = PIDFCoefficients(0.0, 0.0, 0.0, 0.0)

        override fun setTargetPositionTolerance(tolerance: Int) {}

        override fun getTargetPositionTolerance(): Int = 0

        override fun getCurrent(unit: CurrentUnit): Double = 0.0

        override fun getCurrentAlert(unit: CurrentUnit): Double = 0.0

        override fun setCurrentAlert(
            current: Double,
            unit: CurrentUnit,
        ) {}

        override fun isOverCurrent(): Boolean = false

        override fun getMotorType(): MotorConfigurationType = MotorConfigurationType.getUnspecifiedMotorType()

        override fun setMotorType(motorType: MotorConfigurationType) {}

        override fun getController(): DcMotorController? = null

        override fun setPowerFloat() {
            power = 0.0
        }

        override fun getPowerFloat(): Boolean = false

        // DcMotor methods
        override fun setPower(power: Double) {
            this.power = power
        }

        override fun getPower(): Double = power

        override fun getCurrentPosition(): Int = position

        override fun getTargetPosition(): Int = targetPosition

        override fun setTargetPosition(position: Int) {
            this.targetPosition = position
        }

        override fun setMode(mode: DcMotor.RunMode) {
            this.mode = mode
        }

        override fun getMode(): DcMotor.RunMode = mode

        override fun getZeroPowerBehavior(): DcMotor.ZeroPowerBehavior = zeroPowerBehavior

        override fun setZeroPowerBehavior(zeroPowerBehavior: DcMotor.ZeroPowerBehavior) {
            this.zeroPowerBehavior = zeroPowerBehavior
        }

        override fun getPortNumber(): Int = 0

        override fun isBusy(): Boolean = false

        // DcMotorSimple methods
        override fun setDirection(direction: DcMotorSimple.Direction) {
            this.direction = direction
        }

        override fun getDirection(): DcMotorSimple.Direction = direction

        // HardwareDevice methods
        override fun getManufacturer() = HardwareDevice.Manufacturer.Other

        override fun getDeviceName() = "MockDcMotorEx"

        override fun getConnectionInfo() = "Mock"

        override fun getVersion() = 1

        override fun resetDeviceConfigurationForOpMode() {}

        override fun close() {}
    }

    private class MockServo : ServoImpl(null, 0) {
        private var position: Double = 0.0

        override fun setPosition(position: Double) {
            this.position = position.coerceIn(0.0, 1.0)
        }

        override fun getPosition(): Double = position

        override fun scaleRange(
            min: Double,
            max: Double,
        ) {}

        override fun getManufacturer() = HardwareDevice.Manufacturer.Other

        override fun getDeviceName() = "MockServo"

        override fun getConnectionInfo() = "Mock"

        override fun getVersion() = 1

        override fun resetDeviceConfigurationForOpMode() {}

        override fun close() {}
    }

    private class MockCRServo : CRServoImpl(null, 0) {
        private var power: Double = 0.0

        override fun setPower(power: Double) {
            this.power = power
        }

        override fun getPower(): Double = power

        override fun getDirection() = DcMotorSimple.Direction.FORWARD

        override fun setDirection(direction: DcMotorSimple.Direction) {}

        override fun getManufacturer() = HardwareDevice.Manufacturer.Other

        override fun getDeviceName() = "MockCRServo"

        override fun getConnectionInfo() = "Mock"

        override fun getVersion() = 1

        override fun resetDeviceConfigurationForOpMode() {}

        override fun close() {}
    }

    private class MockDistanceSensor : DistanceSensor {
        override fun getDistance(unit: DistanceUnit) = 10.0

        override fun getManufacturer() = HardwareDevice.Manufacturer.Other

        override fun getDeviceName() = "MockDistanceSensor"

        override fun getConnectionInfo() = "Mock"

        override fun getVersion() = 1

        override fun resetDeviceConfigurationForOpMode() {}

        override fun close() {}
    }

    private class MockColorSensor : ColorSensor {
        override fun red() = 0

        override fun green() = 0

        override fun blue() = 0

        override fun alpha() = 0

        override fun argb() = 0

        override fun enableLed(enable: Boolean) {}

        override fun setI2cAddress(address: I2cAddr) {} // Added missing method

        override fun getI2cAddress() = I2cAddr.zero() // Added missing method

        override fun getManufacturer() = HardwareDevice.Manufacturer.Other

        override fun getDeviceName() = "MockColorSensor"

        override fun getConnectionInfo() = "Mock"

        override fun getVersion() = 1

        override fun resetDeviceConfigurationForOpMode() {}

        override fun close() {}
    }

    private class MockVoltageSensor : VoltageSensor {
        override fun getVoltage() = 12.0

        override fun getManufacturer() = HardwareDevice.Manufacturer.Other

        override fun getDeviceName() = "MockVoltageSensor"

        override fun getConnectionInfo() = "Mock"

        override fun getVersion() = 1

        override fun resetDeviceConfigurationForOpMode() {}

        override fun close() {}
    }
}

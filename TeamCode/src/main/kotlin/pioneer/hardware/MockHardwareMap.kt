package pioneer.hardware

import com.qualcomm.robotcore.hardware.*
import com.qualcomm.robotcore.hardware.I2cAddr
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit

/**
 * MockHardware is a mock implementation of an FTC HardwareMap for testing purposes.
 */
class MockHardwareMap : HardwareMap(null, null) {
    init {
        // Add mock DcMotors
        dcMotor.put("leftMotor", MockDcMotor())
        dcMotor.put("rightMotor", MockDcMotor())
        dcMotor.put("flywheelMotor", MockDcMotor())

        // Add mock Servos
        servo.put("armServo", MockServo())
        servo.put("clawServo", MockServo())

        // Add mock CRServos
        crservo.put("intakeCRServo", MockCRServo())

        // Add mock VoltageSensor
        voltageSensor.put("voltageSensor", MockVoltageSensor())
    }

    private class MockDcMotor : DcMotorImpl(null, 0) {
        private var power: Double = 0.0
        private var position: Int = 0
        private var targetPosition: Int = 0
        private var mode: DcMotor.RunMode = DcMotor.RunMode.RUN_WITHOUT_ENCODER

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

        override fun getManufacturer() = HardwareDevice.Manufacturer.Other

        override fun getDeviceName() = "MockDcMotor"

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

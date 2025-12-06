package pioneer

import com.qualcomm.robotcore.hardware.HardwareMap
import pioneer.general.AllianceColor
import pioneer.hardware.BatteryMonitor
import pioneer.hardware.Camera
import pioneer.hardware.Flywheel
import pioneer.hardware.HardwareComponent
import pioneer.hardware.Intake
import pioneer.hardware.LaunchServos
import pioneer.hardware.Launcher
import pioneer.hardware.MecanumBase
import pioneer.hardware.Spindexer
import pioneer.hardware.Turret
import pioneer.localization.localizers.Pinpoint
import pioneer.pathing.follower.Follower
import pioneer.vision.AprilTag

enum class BotType {
    MECANUM_BOT,
    GOBILDA_STARTER_BOT,
    COMP_BOT,
    CUSTOM,
}

class Bot private constructor(
    val type: BotType,
    private val hardwareComponents: Map<Class<out HardwareComponent>, HardwareComponent>,
) {
    // Type-safe access
    internal inline fun <reified T : HardwareComponent> get(): T? = hardwareComponents[T::class.java] as? T

    // Check if a component is present
    internal inline fun <reified T : HardwareComponent> has(): Boolean = hardwareComponents.containsKey(T::class.java)

    fun initAll() {
        hardwareComponents.values.forEach { it.init() }
    }

    var allianceColor = AllianceColor.RED

    // Property-style access for known components
    val mecanumBase get() = get<MecanumBase>()
    val pinpoint get() = get<Pinpoint>()
    val launchServos get() = get<LaunchServos>()
    val flywheel get() = get<Flywheel>()
    val turret get() = get<Turret>()
    val intake get() = get<Intake>()
    val camera get() = get<Camera>()
    val batteryMonitor get() = get<BatteryMonitor>()
    val spindexer get() = get<Spindexer>()
    val launcher get() = get<Launcher>()

    // Follower is lazily initialized (only if accessed)
    // and will error if localizer or mecanumBase is missing
    val follower: Follower by lazy {
        Follower(
            localizer = pinpoint!!,
            mecanumBase = mecanumBase!!,
        )
    }

    fun updateAll(dt: Double) {
        hardwareComponents.values.forEach { it.update() }
        pinpoint?.update(dt)
        // TODO: Add other update methods (ie. localizer, follower)
    }

    // Companion for builder and fromType
    companion object {
        fun builder() = Builder()

        fun fromType(
            type: BotType,
            hardwareMap: HardwareMap,
        ): Bot =
            when (type) {
                BotType.MECANUM_BOT ->
                    builder()
                        .add(MecanumBase(hardwareMap))
                        .add(Pinpoint(hardwareMap))
                        .add(BatteryMonitor(hardwareMap))
                        .build()
                BotType.GOBILDA_STARTER_BOT ->
                    builder()
                        .add(MecanumBase(hardwareMap))
                        .add(Pinpoint(hardwareMap))
                        .add(LaunchServos(hardwareMap))
                        .add(Flywheel(hardwareMap))
                        .add(Camera(hardwareMap, processors = arrayOf(AprilTag().processor)))
                        .add(BatteryMonitor(hardwareMap))
                        .build()
                BotType.COMP_BOT ->
                    builder()
                        .add(MecanumBase(hardwareMap))
                        .add(Pinpoint(hardwareMap))
                        .add(Flywheel(hardwareMap))
                        .add(Intake(hardwareMap))
                        .add(Turret(hardwareMap))
                        .add(Spindexer(hardwareMap))
                        .add(Launcher(hardwareMap))
                        .add(Camera(hardwareMap, processors = arrayOf(AprilTag().processor)))
                        .add(BatteryMonitor(hardwareMap))
                        .build()
                BotType.CUSTOM -> throw IllegalArgumentException("Use Bot.builder() to create a custom bot")
            }
    }

    class Builder {
        private val components = mutableMapOf<Class<out HardwareComponent>, HardwareComponent>()

        fun <T : HardwareComponent> add(component: T): Builder {
            // TODO: Possibly allow interfaces such as Localizer vs Pinpoint
            components[component::class.java] = component
            return this
        }

        fun build(): Bot = Bot(BotType.CUSTOM, components.toMap())
    }
}

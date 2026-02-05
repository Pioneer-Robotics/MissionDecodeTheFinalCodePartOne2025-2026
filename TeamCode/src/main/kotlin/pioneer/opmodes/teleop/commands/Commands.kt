package pioneer.opmodes.teleop.commands

import pioneer.Bot

/**
 * Command interface following the Command Pattern.
 * 
 * Each command represents a single action the robot can perform.
 * Commands are stateless and can be reused.
 * 
 * Design principles:
 * - Single Responsibility: Each command does ONE thing
 * - Open/Closed: Easy to add new commands without modifying existing code
 * - Dependency Inversion: Commands depend on Bot abstraction, not concrete hardware
 */
interface Command {
    /**
     * Execute the command on the robot.
     * 
     * @param bot The robot to execute the command on
     * @param context Additional context needed for execution (e.g., analog values)
     */
    fun execute(bot: Bot, context: CommandContext = CommandContext.EMPTY)
    
    /**
     * Optional: Return a description of what this command does.
     * Useful for debugging and telemetry.
     */
    fun description(): String = this::class.simpleName ?: "Unknown Command"
}

/**
 * Context object that carries additional data needed for command execution.
 * This allows commands to receive analog values, parameters, etc.
 */
data class CommandContext(
    val analogValue: Double = 0.0,
    val vectorX: Double = 0.0,
    val vectorY: Double = 0.0,
    val vectorOmega: Double = 0.0,
    val parameters: Map<String, Any> = emptyMap()
) {
    companion object {
        val EMPTY = CommandContext()
    }
}

/**
 * Composite command that executes multiple commands in sequence.
 * Useful for complex actions that require multiple steps.
 */
class CompositeCommand(private val commands: List<Command>) : Command {
    override fun execute(bot: Bot, context: CommandContext) {
        commands.forEach { it.execute(bot, context) }
    }
    
    override fun description(): String {
        return "Composite[${commands.joinToString(", ") { it.description() }}]"
    }
}

/**
 * Conditional command that only executes if a condition is met.
 * Useful for safety checks or state-dependent actions.
 */
class ConditionalCommand(
    private val condition: (Bot) -> Boolean,
    private val command: Command
) : Command {
    override fun execute(bot: Bot, context: CommandContext) {
        if (condition(bot)) {
            command.execute(bot, context)
        }
    }
    
    override fun description(): String {
        return "If(?) then ${command.description()}"
    }
}

// ============================================================================
// DRIVER 1 COMMANDS
// ============================================================================

/**
 * Drive the robot using mecanum drive.
 * 
 * Context should contain:
 * - vectorX, vectorY: Drive direction
 * - vectorOmega: Rotation
 * - parameters["drivePower"]: Power multiplier
 * - parameters["fieldCentric"]: Whether to use field-centric drive
 * - parameters["robotTheta"]: Current robot heading (for field-centric)
 */
class DriveCommand : Command {
    override fun execute(bot: Bot, context: CommandContext) {
        var x = context.vectorX
        var y = context.vectorY
        
        // Apply field-centric transformation if enabled
        if (context.parameters["fieldCentric"] == true) {
            val robotTheta = context.parameters["robotTheta"] as? Double ?: 0.0
            val allianceColor = bot.allianceColor
            
            var angle = kotlin.math.atan2(y, x) - robotTheta
            angle += if (allianceColor == pioneer.general.AllianceColor.BLUE) {
                kotlin.math.PI / 2
            } else {
                -kotlin.math.PI / 2
            }
            
            val magnitude = kotlin.math.hypot(x, y)
            x = magnitude * kotlin.math.cos(angle)
            y = magnitude * kotlin.math.sin(angle)
        }
        
        val drivePower = context.parameters["drivePower"] as? Double ?: 1.0
        
        bot.mecanumBase?.setDrivePower(
            pioneer.helpers.Pose(vx = x, vy = y, omega = context.vectorOmega),
            drivePower,
            pioneer.Constants.Drive.MAX_MOTOR_VELOCITY_TPS
        )
    }
    
    override fun description() = "Drive"
}

/**
 * Start the intake moving forward.
 */
class StartIntakeCommand : Command {
    override fun execute(bot: Bot, context: CommandContext) {
        bot.intake?.forward()
    }
    
    override fun description() = "Start Intake"
}

/**
 * Stop the intake.
 */
class StopIntakeCommand : Command {
    override fun execute(bot: Bot, context: CommandContext) {
        bot.intake?.stop()
    }
    
    override fun description() = "Stop Intake"
}

/**
 * Reverse the intake.
 */
class ReverseIntakeCommand : Command {
    override fun execute(bot: Bot, context: CommandContext) {
        bot.intake?.reverse()
    }
    
    override fun description() = "Reverse Intake"
}

/**
 * Move spindexer to next open intake position.
 */
class MoveSpindexerToNextIntakeCommand : Command {
    override fun execute(bot: Bot, context: CommandContext) {
        bot.spindexer?.moveToNextOpenIntake()
    }
    
    override fun description() = "Spindexer -> Next Intake"
}

/**
 * Move spindexer manually with analog control.
 * 
 * Context should contain analogValue with the power (-1.0 to 1.0).
 */
class MoveSpindexerManualCommand : Command {
    override fun execute(bot: Bot, context: CommandContext) {
        bot.spindexer?.moveManual(context.analogValue)
    }
    
    override fun description() = "Spindexer Manual"
}

/**
 * Reset the spindexer to home position.
 */
class ResetSpindexerCommand : Command {
    override fun execute(bot: Bot, context: CommandContext) {
        bot.spindexer?.reset()
    }
    
    override fun description() = "Reset Spindexer"
}

/**
 * Reset odometry to a specific pose.
 * 
 * Context should contain parameters["pose"] with the target Pose.
 */
class ResetOdometryCommand : Command {
    override fun execute(bot: Bot, context: CommandContext) {
        val pose = context.parameters["pose"] as? pioneer.helpers.Pose ?: return
        bot.pinpoint?.reset(pose)
    }
    
    override fun description() = "Reset Odometry"
}

// ============================================================================
// DRIVER 2 COMMANDS
// ============================================================================

/**
 * Set flywheel velocity.
 * 
 * Context should contain analogValue with the target velocity.
 */
class SetFlywheelVelocityCommand : Command {
    override fun execute(bot: Bot, context: CommandContext) {
        bot.flywheel?.velocity = context.analogValue
    }
    
    override fun description() = "Set Flywheel"
}

/**
 * Stop the flywheel.
 */
class StopFlywheelCommand : Command {
    override fun execute(bot: Bot, context: CommandContext) {
        bot.flywheel?.velocity = 0.0
    }
    
    override fun description() = "Stop Flywheel"
}

/**
 * Move turret to a specific angle.
 * 
 * Context should contain analogValue with the target angle.
 */
class SetTurretAngleCommand : Command {
    override fun execute(bot: Bot, context: CommandContext) {
        bot.turret?.gotoAngle(context.analogValue)
    }
    
    override fun description() = "Set Turret Angle"
}

/**
 * Auto-track with turret using AprilTag.
 * 
 * Context should contain analogValue with the error in degrees (optional).
 */
class TurretAutoTrackCommand : Command {
    override fun execute(bot: Bot, context: CommandContext) {
        val errorDegrees = context.parameters["errorDegrees"] as? Double
        bot.turret?.tagTrack(errorDegrees)
    }
    
    override fun description() = "Turret Auto-Track"
}

/**
 * Move spindexer to next outtake position.
 * 
 * Context can contain parameters["artifact"] to specify which artifact type.
 */
class MoveSpindexerToNextOuttakeCommand : Command {
    override fun execute(bot: Bot, context: CommandContext) {
        val artifact = context.parameters["artifact"] as? pioneer.decode.Artifact
        if (artifact != null) {
            bot.spindexer?.moveToNextOuttake(artifact)
        } else {
            bot.spindexer?.moveToNextOuttake()
        }
    }
    
    override fun description() = "Spindexer -> Next Outtake"
}

/**
 * Trigger the launcher.
 */
class TriggerLauncherCommand : Command {
    override fun execute(bot: Bot, context: CommandContext) {
        bot.launcher?.triggerLaunch()
    }
    
    override fun description() = "Trigger Launcher"
}

/**
 * Pop current artifact from spindexer (after shooting).
 */
class PopArtifactCommand : Command {
    override fun execute(bot: Bot, context: CommandContext) {
        bot.spindexer?.popCurrentArtifact()
    }
    
    override fun description() = "Pop Artifact"
}

/**
 * Set LED color.
 * 
 * Context should contain parameters["color"] with the Color.
 */
class SetLEDColorCommand : Command {
    override fun execute(bot: Bot, context: CommandContext) {
        val color = context.parameters["color"] as? pioneer.hardware.prism.Color ?: return
        bot.led?.setColor(color)
    }
    
    override fun description() = "Set LED"
}

/**
 * Set gamepad LED color.
 * 
 * Context should contain parameters with r, g, b values.
 */
class SetGamepadLEDCommand(private val gamepad: com.qualcomm.robotcore.hardware.Gamepad) : Command {
    override fun execute(bot: Bot, context: CommandContext) {
        val r = context.parameters["r"] as? Double ?: 0.0
        val g = context.parameters["g"] as? Double ?: 0.0
        val b = context.parameters["b"] as? Double ?: 0.0
        gamepad.setLedColor(r, g, b, -1)
    }
    
    override fun description() = "Set Gamepad LED"
}

package pioneer.opmodes.other

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import pioneer.Bot
import pioneer.BotType
import pioneer.decode.Artifact
import pioneer.opmodes.BaseOpMode

@Autonomous(name="Shoot All Test", group="Testing")
class ShootAllTest : BaseOpMode() {

    override fun onInit() {
        bot = Bot.fromType(BotType.COMP_BOT, hardwareMap)
    }

    override fun start() {
        bot.flywheel!!.velocity = 800.0
        bot.spindexer!!.setArtifacts(Artifact.PURPLE, Artifact.PURPLE, Artifact.PURPLE)
    }

    override fun onLoop() {
        if (bot.flywheel!!.velocity > 790.0) {
            val slowSpeed = 0.085 + 0.05 * bot.spindexer!!.numStoredArtifacts
            val speed = 0.115 + 0.05 * bot.spindexer!!.numStoredArtifacts
            bot.spindexer?.moveManual(if (bot.spindexer!!.reachedOuttakePosition) slowSpeed else speed)
            val closestMotorPosition = bot.spindexer!!.closestMotorState.ordinal / 2
            val hasArtifact = bot.spindexer!!.artifacts[closestMotorPosition] != null
            bot.spindexer!!.motorState = bot.spindexer!!.closestMotorState
            if (bot.spindexer!!.reachedOuttakePosition && hasArtifact) {
                bot.launcher!!.triggerLaunch()
                bot.spindexer!!.popCurrentArtifact()
            }
        }
    }
}

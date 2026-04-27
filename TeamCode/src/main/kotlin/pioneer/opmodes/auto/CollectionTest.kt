package pioneer.opmodes.other

import com.qualcomm.robotcore.eventloop.opmode.Autonomous
import pioneer.Bot
import pioneer.BotType
import pioneer.helpers.DashboardPlotter
import pioneer.helpers.Pose
import pioneer.opmodes.BaseOpMode
import pioneer.pathing.paths.HermitePath
import kotlin.math.hypot

//@Disabled
@Autonomous(name = "Collection Test", group = "Testing")
class CollectionTest : BaseOpMode() {
    override fun onInit() {
        bot = Bot.fromType(BotType.COMP_BOT, hardwareMap)
    }

    override fun onLoop() {
        bot.intake?.forward()
        if (!bot.follower.isFollowing) {
            bot.follower.followPath(
                HermitePath
                    .Builder()
                    .addPoint(Pose(0.0, 0.0, theta = 0.0), Pose(0.0, 0.0))
                    .addPoint(Pose(0.0, 60.0, theta = 0.0), Pose(0.0, 0.0))
                    .build(),
                10.0
            )
        }
        if (bot.follower.done) {
            requestOpModeStop()
        }
    }
}

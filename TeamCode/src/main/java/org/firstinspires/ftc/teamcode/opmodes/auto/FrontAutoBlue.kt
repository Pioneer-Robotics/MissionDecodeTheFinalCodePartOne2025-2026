package org.firstinspires.ftc.teamcode.opmodes.auto

import com.qualcomm.robotcore.eventloop.opmode.OpMode
import org.firstinspires.ftc.teamcode.Bot
import org.firstinspires.ftc.teamcode.localization.Pose
import org.firstinspires.ftc.teamcode.pathing.paths.LinearPath

class FrontAutoBlue : OpMode() {
    enum class STATE {
        INIT,
        LOOK_FOR_TAG,
        GOTO_LAUNCH,
        LAUNCH,
        GOTO_COLLECT,
        GOTO_PARK,
        STOP
    }

    private var state = STATE.INIT
    private lateinit var bot: Bot

    override fun init() {
        bot = Bot(Bot.BotFlavor.GOBILDA_STARTER_BOT, hardwareMap)
    }

    override fun loop() {
        when (state) {
            STATE.INIT -> state_init()
            STATE.LOOK_FOR_TAG -> state_look_for_tag()
            STATE.GOTO_LAUNCH -> state_goto_launch()
            STATE.LAUNCH -> state_launch()
            STATE.GOTO_COLLECT -> state_goto_collect()
            STATE.GOTO_PARK -> state_goto_park()
            STATE.STOP -> state_stop()
        }
    }

    fun state_init() {
        state = STATE.GOTO_LAUNCH
    }
    fun state_look_for_tag() {
        val detections = bot.aprilTagProcessor.aprilTag.detections
        if (detections.size >= 2) {
            for (detection in detections) {
                // TODO: figure out order of obelisk
            }
        }

        if (bot.follower.done) {
            state = STATE.LAUNCH
        }
    }
    fun state_goto_launch() {
        bot.follower.path = LinearPath(bot.localizer.pose, Pose(120.0, 240.0))
        bot.follower.start()
        state = STATE.LOOK_FOR_TAG
    }
    fun state_launch() {
        // TODO: implement launch
    }
    fun state_goto_collect() {
        // TODO: implement collect
    }
    fun state_goto_park() {
        // TODO: determine where to park
    }
    fun state_stop(){
        requestOpModeStop()
    }
}
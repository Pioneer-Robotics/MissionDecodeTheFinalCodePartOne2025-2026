//package pioneer.opmodes.teleop.drivers
//
//import com.qualcomm.robotcore.hardware.Gamepad
//import pioneer.Bot
//import pioneer.decode.Artifact
//import pioneer.decode.GoalTag
//import pioneer.decode.Points
//import pioneer.general.AllianceColor
//import pioneer.hardware.Turret
//import pioneer.helpers.Chrono
//import pioneer.helpers.Pose
//import pioneer.helpers.Toggle
//import pioneer.helpers.next
//import kotlin.math.PI
//import kotlin.math.abs
//import kotlin.math.pow
//
//class TeleopDriver2Testing(
//    private val gamepad: Gamepad,
//    private val bot: Bot,
//) {
//    enum class FlywheelSpeedRange(
//        val velocity: Double,
//    ) {
//        SHORT_RANGE(800.0),
//        LONG_RANGE(1000.0),
//    }
//
//    val isEstimateSpeed = Toggle(true)
//    private val chrono = Chrono(autoUpdate = false)
//    private val isAutoTracking = Toggle(false)
//    private val incCustomTargetDistance = Toggle(false)
//    private val decCustomTargetDistance = Toggle(false)
//    private val incCustomTargetHeight = Toggle(false)
//    private val decCustomTargetHeight = Toggle(false)
//
//    private val flywheelToggle = Toggle(false)
//    private val changeFlywheelRangeToggle = Toggle(false)
//
//    var P = Points(bot.allianceColor)
//
//    enum class ShootState { READY, MOVING_TO_POSITION, LAUNCHING }
//
//    var flywheelVelocityEnum = FlywheelSpeedRange.SHORT_RANGE
//    var shootState = ShootState.READY
//    var targetGoal = GoalTag.RED
//    var customTrackingTarget = Toggle(false)
//    lateinit var shootingTarget: Pose
//    var shootingHeight = 0.0
//    var customTargetHeight = 0.0
//    var customTargetDistance = 0.0
//    private var shootingAll = false
//    private var remainingShots = 0
//    var turretAngle = 0.0
//    var flywheelSpeed = 0.0
//
//
//    fun update() {
//        checkTargetGoal()
//        checkShootingTarget()
//        customTargetHandling()
//        updateFlywheelSpeed()
//        handleFlywheel()
//        handleTurret()
//        handleShootInput()
//        processShooting()
//        updateIndicatorLED()
//        chrono.update() // Manual update to allow dt to match across the loop.
//    }
//
//    private fun checkTargetGoal() {
//        if (bot.allianceColor == AllianceColor.BLUE) {
//            targetGoal = GoalTag.BLUE
//        } else { return }
//    }
//
//    private fun checkShootingTarget(){
//        customTrackingTarget.toggle(gamepad.dpad_right)
//
//        if (customTrackingTarget.state){
//
//            if (gamepad.left_stick_button){
//                shootingTarget = bot.turret?.setCustomTarget(bot.pinpoint?.pose ?: Pose(), customTargetDistance)!!
//                shootingHeight = customTargetHeight
//            }
//        } else {
//            shootingTarget = targetGoal.shootingPose
//            shootingHeight = targetGoal.shootingHeight
//        }
//    }
//
//    private fun customTargetHandling(){
//        incCustomTargetDistance.toggle(gamepad.right_trigger.toDouble() != 0.0)
//        decCustomTargetDistance.toggle(gamepad.left_trigger.toDouble() != 0.0)
//
//        incCustomTargetHeight.toggle(gamepad.right_trigger.toDouble() != 0.0 && gamepad.circle)
//        decCustomTargetHeight.toggle(gamepad.left_trigger.toDouble() != 0.0 && gamepad.circle)
//
//        if (incCustomTargetDistance.justChanged){
//            customTargetDistance += 5.0
//        }
//        if (decCustomTargetDistance.justChanged){
//            customTargetDistance -= 5.0
//        }
//
//        if (incCustomTargetHeight.justChanged){
//            customTargetHeight += 5.0
//        }
//        if (decCustomTargetHeight.justChanged){
//            customTargetHeight -= 5.0
//        }
//
//    }
//
//    private fun updateFlywheelSpeed() {
//        isEstimateSpeed.toggle(gamepad.dpad_right)
////        if (flywheelSpeed < 1.0 && gamepad.dpad_up) {
////            flywheelSpeed += chrono.dt * 0.5
////        }
////        if (flywheelSpeed > 0.0 && gamepad.dpad_down) {
////            flywheelSpeed -= chrono.dt * 0.5
////        }
////        flywheelSpeed = flywheelSpeed.coerceIn(0.0, 1.0)
//
//        if (isEstimateSpeed.state) {
//            flywheelSpeed = bot.flywheel!!.estimateVelocity(bot.pinpoint?.pose ?: Pose(), shootingTarget, shootingHeight)
//        } else {
//            flywheelSpeed = flywheelVelocityEnum.velocity
//        }
//
//        changeFlywheelRangeToggle.toggle(gamepad.dpad_up)
//
//        if (changeFlywheelRangeToggle.justChanged) {
//            flywheelVelocityEnum = flywheelVelocityEnum.next()
//        }
//    }
//
//    private fun handleFlywheel() {
//        flywheelToggle.toggle(gamepad.dpad_left)
//        if (flywheelToggle.state) {
//            bot.flywheel?.velocity = flywheelSpeed
//        } else {
//            bot.flywheel?.velocity = 0.0
//        }
//    }
//
//    private fun handleTurret() {
//        isAutoTracking.toggle(gamepad.cross)
//        bot.turret?.mode = if (isAutoTracking.state) Turret.Mode.AUTO_TRACK else Turret.Mode.MANUAL
//        if (bot.turret?.mode == Turret.Mode.MANUAL) handleManualTrack() else handleAutoTrack()
//    }
//
//    private fun handleShootInput() {
//        if (shootState == ShootState.READY && !shootingAll) {
//            when {
//                gamepad.right_bumper -> shootArtifact(Artifact.PURPLE)
//                gamepad.left_bumper -> shootArtifact(Artifact.GREEN)
//                gamepad.triangle -> shootArtifact()
////                gamepad.touchpad -> startShootingAll()
////                gamepad.touchpad -> startShootingAll()
//            }
//        }
//    }
//
//    private fun processShooting() {
//
//        if (!flywheelToggle.state) return
//        if (gamepad.square &&
//            bot.spindexer?.reachedTarget == true &&
//            bot.spindexer?.isOuttakePosition == true
//        ) {
//            bot.launcher?.triggerLaunch()
//            bot.spindexer?.popCurrentArtifact()
//        }
//    }
//
//    private fun shootArtifact(artifact: Artifact? = null) {
//        // Can't shoot when flywheel isn't moving
//        // Start artifact launch sequence
//        val moved =
//            if (artifact != null) {
//                bot.spindexer?.moveToNextOuttake(artifact)
//            } else {
//                bot.spindexer?.moveToNextOuttake()
//            }
////        if (moved == true) shootState = ShootState.MOVING_TO_POSITION
//    }
//
//    private fun handleManualTrack() {
//        if (abs(gamepad.right_stick_x) > 0.02) {
//            turretAngle -= gamepad.right_stick_x.toDouble().pow(3) * chrono.dt/1000.0 * 5.0
//            turretAngle.coerceIn(
//                -PI,
//                PI,
//            ) // FIX: This will break if the turret has a different range. Try to hand off this to the Turret class
//        }
//
//        if (gamepad.right_stick_button) {
//            turretAngle = 0.0
//        }
//        bot.turret?.gotoAngle(turretAngle)
//    }
//
//    private fun handleAutoTrack() {
//        if (bot.turret?.mode == Turret.Mode.AUTO_TRACK) {
//            bot.turret?.autoTrack(
//                bot.pinpoint?.pose ?: Pose(),
//                shootingTarget,
//            )
//        }
//    }
//
//    private fun updateIndicatorLED() {
//        bot.flywheel?.velocity?.let {
//            if (it >= flywheelSpeed-10 && it <=flywheelSpeed+20) {
//                gamepad.setLedColor(0.0, 1.0, 0.0, -1)
//            } else if (it <flywheelSpeed-10){
//                gamepad.setLedColor(255.0,165.0,0.0, -1)
//            }
//            else {
//                gamepad.setLedColor(1.0, 0.0, 0.0, -1)
//            }
//        }
//    }
//}

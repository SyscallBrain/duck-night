package pt.tiago.ducknight.core.bot

import pt.tiago.ducknight.core.engine.GameState
import pt.tiago.ducknight.core.engine.InputAction
import pt.tiago.ducknight.core.engine.RoundPhase
import pt.tiago.ducknight.core.fixed.TrigTable
import pt.tiago.ducknight.core.fixed.fx
import pt.tiago.ducknight.core.fixed.toUnits
import pt.tiago.ducknight.core.flight.BirdState
import pt.tiago.ducknight.core.flight.BirdStatus
import pt.tiago.ducknight.core.fair.ClayStatus
import pt.tiago.ducknight.core.level.LevelType
import pt.tiago.ducknight.core.level.Modifier
import pt.tiago.ducknight.core.random.SplitMix64

/**
 * A deterministic simulated photographer. It only uses what a player sees: the visible birds,
 * their current position and velocity, the film counter and the manual-advance state. It never
 * looks at hidden birds (fog, cover, night or a dive).
 */
class HumanPhotographer(
    val profile: BotProfile,
    seed: Long,
) {
    private val rng = SplitMix64(seed)
    private var targetId = -1
    private var snapAt = 0
    private var waitForPose = false
    private var poseWaitStarted = 0
    private var advanceAt = 0
    private var aimPointX = 0
    private var aimPointY = 0

    fun actions(state: GameState): List<InputAction> {
        if (state.phase != RoundPhase.RUNNING) return emptyList()
        val extras = state.extras
        if (extras?.needsAdvance == true) {
            if (advanceAt == 0) advanceAt = state.tick + profile.advanceDelayTicks
            if (state.tick >= advanceAt) {
                advanceAt = 0
                return listOf(InputAction.Advance)
            }
            return emptyList()
        }
        if (extras?.filmLeft != null && extras.filmLeft <= 0) return emptyList()
        if (state.spec.type == LevelType.TIMED && extras != null && state.tick < extras.cooldownUntil) {
            return emptyList()
        }
        if (state.spec.type == LevelType.CLAY) return clayActions(state)
        if (remainingPhotos(state) <= 0) return emptyList()

        val visible = state.birds.filter {
            it.status == BirdStatus.FLYING && !it.hidden && !it.resolved
        }
        var target = visible.firstOrNull { it.id == targetId }
        if (target == null) {
            targetId = -1
            target = chooseTarget(visible) ?: return emptyList()
            targetId = target.id
            snapAt = state.tick + reactionTicks() + 1
            waitForPose = poseDecision()
            poseWaitStarted = 0
            val predictionTicks = snapAt - state.tick
            aimPointX = (target.x.toLong() +
                target.vx.toLong() * predictionTicks * profile.anticipationPercent / 100)
                .coerceIn(0L, (pt.tiago.ducknight.core.fixed.WORLD_WIDTH * 256).toLong()).toInt()
            aimPointY = (target.y.toLong() +
                target.vy.toLong() * predictionTicks * profile.anticipationPercent / 100)
                .coerceIn(0L, (pt.tiago.ducknight.core.fixed.WORLD_HEIGHT * 256).toLong()).toInt()
        }
        if (state.tick < snapAt) return emptyList()
        if (waitForPose && !inPoseWindow(target) && remainingPhotos(state) > 1) {
            if (poseWaitStarted == 0) poseWaitStarted = state.tick
            if (state.tick - poseWaitStarted < 120) return emptyList()
        }
        track(target.x, target.y, target.vx, target.vy)

        val aimX = toUnits(aimPointX) + normalOffset(profile.aimSigmaUnits)
        val aimY = toUnits(aimPointY) + normalOffset(profile.aimSigmaUnits)

        targetId = -1
        if (Modifier.MANUAL_ADVANCE in state.spec.modifiers) {
            advanceAt = state.tick + profile.advanceDelayTicks
        }
        return listOf(InputAction.Snap(aimX, aimY))
    }

    private fun clayActions(state: GameState): List<InputAction> {
        val extras = state.extras ?: return emptyList()
        val clays = extras.clays.filter { it.status == ClayStatus.FLYING }
        if (clays.isEmpty() || extras.corksLeft <= 0) return emptyList()
        var target = clays.firstOrNull { it.id == targetId }
        if (target == null) {
            targetId = -1
            target = clays.minByOrNull { it.id } ?: return emptyList()
            targetId = target.id
            snapAt = state.tick + reactionTicks() + 1
            waitForPose = false
            val ticks = snapAt - state.tick
            aimPointX = (target.x + target.vx * ticks * profile.anticipationPercent / 100)
                .coerceIn(0, 240 * 256)
            aimPointY = (target.y + target.vy * ticks * profile.anticipationPercent / 100)
                .coerceIn(0, 300 * 256)
        }
        if (state.tick < snapAt) return emptyList()
        track(target.x, target.y, target.vx, target.vy)
        val aimX = toUnits(aimPointX) + normalOffset(profile.aimSigmaUnits)
        val aimY = toUnits(aimPointY) + normalOffset(profile.aimSigmaUnits)
        targetId = -1
        if (Modifier.MANUAL_ADVANCE in state.spec.modifiers) {
            advanceAt = state.tick + profile.advanceDelayTicks
        }
        return listOf(InputAction.Snap(aimX, aimY))
    }

    /**
     * A player follows the target with the eyes and taps where it is now, as seen [TRACKING_LAG_TICKS]
     * ago, leading it by the profile's anticipation. The reaction decides when to shoot, not where.
     */
    private fun track(x: Int, y: Int, vx: Int, vy: Int) {
        val behind = 100 - profile.anticipationPercent
        aimPointX = (x.toLong() - vx.toLong() * TRACKING_LAG_TICKS * behind / 100)
            .coerceIn(0L, (pt.tiago.ducknight.core.fixed.WORLD_WIDTH * 256).toLong()).toInt()
        aimPointY = (y.toLong() - vy.toLong() * TRACKING_LAG_TICKS * behind / 100)
            .coerceIn(0L, (pt.tiago.ducknight.core.fixed.WORLD_HEIGHT * 256).toLong()).toInt()
    }

    private fun remainingPhotos(state: GameState): Int {
        val extras = state.extras
        if (state.spec.type == LevelType.CLAY) return extras?.corksLeft ?: 0
        if (extras?.filmLeft != null) return extras.filmLeft
        return (state.spec.photosPerBird - state.photosUsed).coerceAtLeast(0)
    }

    private fun chooseTarget(visible: List<BirdState>): BirdState? {
        if (visible.isEmpty()) return null
        val protectedBirds = visible.filter { it.species.protected }
        if (protectedBirds.isNotEmpty() && rng.nextInt(100) < profile.protectedMistakePercent) {
            return protectedBirds.minByOrNull { it.id }
        }
        val targets = visible.filter { !it.species.protected }
        if (targets.isEmpty()) {
            // The perfect witness must never waste a frame on a protected bird when a target
            // still has to appear; the human profiles keep their small mistake rate.
            if (profile.protectedMistakePercent == 0) return null
            return protectedBirds.minByOrNull { it.id }
        }
        return targets.minByOrNull { kotlin.math.abs(it.x - fx(120)) + kotlin.math.abs(it.y - fx(150)) }
    }

    private fun poseDecision(): Boolean = when (profile.posePolicy) {
        PosePolicy.NEVER -> false
        PosePolicy.SOMETIMES -> rng.nextInt(100) < 50
        PosePolicy.ALMOST_ALWAYS -> rng.nextInt(100) < 85
        PosePolicy.ALWAYS -> true
    }

    private fun inPoseWindow(bird: BirdState): Boolean {
        val lower = TrigTable.STEPS * 40 / 100
        val upper = TrigTable.STEPS * 60 / 100
        return bird.wingPhase in lower..upper
    }

    private fun reactionTicks(): Int {
        if (!profile.reactionJitter) return profile.reactionMeanTicks
        val multiplier = REACTION_MULTIPLIERS[rng.nextInt(REACTION_MULTIPLIERS.size)]
        return (profile.reactionMeanTicks * multiplier / 100).coerceAtLeast(1)
    }

    /** Sum of three uniforms: a normal approximation in whole world units. */
    private fun normalOffset(sigma: Int): Int {
        if (sigma <= 0) return 0
        var sum = 0
        repeat(3) { sum += rng.nextInt(2 * sigma + 1) - sigma }
        return sum
    }

    private companion object {
        /** Visual-motor delay of a tap on a moving target: about 130 ms. */
        const val TRACKING_LAG_TICKS = 8

        /**
         * A fixed log-normal-shaped multiplier table (mean around 100). Built once with
         * StrictMath.exp, which is deterministic, and never touched by the simulation.
         */
        val REACTION_MULTIPLIERS: IntArray = IntArray(64) { index ->
            val z = (index - 31.5) / 32.0 * 0.45
            kotlin.math.round(StrictMath.exp(z) * 100.0).toInt()
        }
    }
}

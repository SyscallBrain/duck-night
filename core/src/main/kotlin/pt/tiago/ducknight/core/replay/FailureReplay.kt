package pt.tiago.ducknight.core.replay

import pt.tiago.ducknight.core.engine.GameEvent
import pt.tiago.ducknight.core.engine.GameState
import pt.tiago.ducknight.core.engine.RoundPhase
import pt.tiago.ducknight.core.engine.Simulation
import pt.tiago.ducknight.core.level.GameOptions
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.photo.PhotoFrame
import pt.tiago.ducknight.core.photo.PhotoResolver

/** One viewfinder drawn over the failure replay. */
data class ReplayPhoto(
    val tick: Int,
    val aimX: Int,
    val aimY: Int,
    val frame: PhotoFrame,
)

/** One sampled position of the bird the replay follows. */
data class FailureTracePoint(
    val tick: Int,
    val birdId: Int,
    val x: Int,
    val y: Int,
)

/**
 * Evidence for the failure replay: the last three seconds before a fled
 * bird (or a selected bird), the exact viewfinder of each shutter in that window, and the bird
 * trace. The frames are the real simulation states, so the UI never recomputes a different path.
 */
data class FailureReplay(
    val spec: LevelSpec,
    val options: GameOptions,
    val startTick: Int,
    val endTick: Int,
    val focusBirdId: Int,
    val frames: List<GameState>,
    val trace: List<FailureTracePoint>,
    val photos: List<ReplayPhoto>,
) {
    init {
        require(startTick >= 0 && endTick >= startTick) { "Invalid replay window" }
    }

    companion object {
        const val WINDOW_TICKS = 180

        fun build(
            spec: LevelSpec,
            options: GameOptions,
            log: InputLog,
            untilTick: Int,
            focusBirdId: Int? = null,
        ): FailureReplay {
            require(untilTick >= 0) { "Cannot replay a negative tick" }
            var state = Simulation.start(spec, options)
            val states = mutableListOf(state)
            val allPhotos = mutableListOf<ReplayPhoto>()
            var lastFledId: Int? = null
            var lastFledTick: Int? = null
            var selectedTick: Int? = null
            var guard = 0
            while (state.tick < untilTick && state.phase != RoundPhase.FINISHED && guard < 10_000_000) {
                val nextTick = state.tick + 1
                val step = Simulation.step(state, log.actionsAt(nextTick))
                state = step.state
                states += state
                for (event in step.events) {
                    when (event) {
                        is GameEvent.PhotoResult -> allPhotos += ReplayPhoto(
                            tick = state.tick,
                            aimX = event.photo.aimX,
                            aimY = event.photo.aimY,
                            frame = PhotoResolver.frame(spec, options),
                        )
                        is GameEvent.BirdResolved -> {
                            if (!event.result.photographed) {
                                lastFledId = event.result.birdId
                                lastFledTick = event.result.tick
                            }
                            if (focusBirdId != null && event.result.birdId == focusBirdId) {
                                selectedTick = event.result.tick
                            }
                        }
                        else -> Unit
                    }
                }
                guard++
            }
            // A chosen photo or the last fled bird defines the end of the evidence. If there is
            // neither (HUD/pause before any failure), show the last three seconds of play.
            val end = when {
                focusBirdId != null && selectedTick != null -> selectedTick
                focusBirdId == null && lastFledTick != null -> lastFledTick
                else -> state.tick
            }
            val start = (end - WINDOW_TICKS).coerceAtLeast(0)
            val frames = states.filter { it.tick in start..end }
            val focus = focusBirdId ?: lastFledId ?: frames.lastOrNull()?.birds?.firstOrNull()?.id ?: -1
            val trace = frames.mapNotNull { frame ->
                frame.birds.firstOrNull { it.id == focus }?.let {
                    FailureTracePoint(frame.tick, focus, it.x, it.y)
                }
            }
            val photos = allPhotos.filter { it.tick in start..end }
            return FailureReplay(spec, options, start, end, focus, frames, trace, photos)
        }
    }
}

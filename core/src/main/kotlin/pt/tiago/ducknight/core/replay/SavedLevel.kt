package pt.tiago.ducknight.core.replay

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import pt.tiago.ducknight.core.engine.GameState
import pt.tiago.ducknight.core.engine.RoundPhase
import pt.tiago.ducknight.core.engine.Simulation
import pt.tiago.ducknight.core.hash.StateHasher
import pt.tiago.ducknight.core.level.GameOptions
import pt.tiago.ducknight.core.level.LevelSpec

/**
 * Everything needed to continue a level after the app dies: the exact variant, the accessibility
 * options, the input log, the last tick and the hash the simulation had at that tick.
 */
@Serializable
data class SavedLevel(
    val spec: LevelSpec,
    val options: GameOptions,
    val tick: Int,
    val stateHash: Long,
    val log: InputLog = InputLog(),
)

/** Replays a [SavedLevel] with the current engine, exactly as the app would have. */
object Replay {
    fun run(spec: LevelSpec, options: GameOptions, log: InputLog, untilTick: Int): GameState {
        require(untilTick >= 0) { "Cannot replay a negative tick" }
        var state = Simulation.start(spec, options)
        while (state.tick < untilTick && state.phase != RoundPhase.FINISHED) {
            val nextTick = state.tick + 1
            state = Simulation.step(state, log.actionsAt(nextTick)).state
        }
        return state
    }

    fun runWithHash(spec: LevelSpec, options: GameOptions, log: InputLog, untilTick: Int): GameStateWithHash {
        val state = run(spec, options, log, untilTick)
        return GameStateWithHash(state, StateHasher.hash(state))
    }
}

data class GameStateWithHash(
    val state: GameState,
    val hash: Long,
)

sealed interface ResumeOutcome {
    data class Restored(val state: pt.tiago.ducknight.core.engine.GameState) : ResumeOutcome
    data class HashMismatch(val expected: Long, val actual: Long) : ResumeOutcome
    data class Invalid(val reason: String) : ResumeOutcome
}

object ResumeService {
    private const val MAX_TICK = 10_000_000

    fun restore(saved: SavedLevel): ResumeOutcome {
        if (saved.tick < 0) return ResumeOutcome.Invalid("Negative tick")
        if (saved.tick > MAX_TICK) return ResumeOutcome.Invalid("Tick too large")
        val replayed = Replay.runWithHash(saved.spec, saved.options, saved.log, saved.tick)
        return if (replayed.hash == saved.stateHash) {
            ResumeOutcome.Restored(replayed.state)
        } else {
            ResumeOutcome.HashMismatch(saved.stateHash, replayed.hash)
        }
    }
}

/** JSON for the saved level. Encoding defaults keeps the format stable across versions. */
object SavedLevelCodec {
    private val json = Json {
        encodeDefaults = true
        explicitNulls = false
        ignoreUnknownKeys = false
    }

    fun encode(saved: SavedLevel): String = json.encodeToString(SavedLevel.serializer(), saved)

    fun decode(text: String): SavedLevel? = runCatching {
        json.decodeFromString(SavedLevel.serializer(), text)
    }.getOrNull()
}

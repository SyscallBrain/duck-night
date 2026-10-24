package pt.tiago.ducknight.core.engine

import pt.tiago.ducknight.core.flight.BirdState
import pt.tiago.ducknight.core.level.GameOptions
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.photo.PhotoFrame
import pt.tiago.ducknight.core.photo.PhotoResult

enum class RoundPhase { RUNNING, FINISHED }

/** One bird in the round result. A bird is either photographed or fled, never both. */
data class BirdResult(
    val birdId: Int,
    val speciesId: String,
    val photographed: Boolean,
    val quality: Int,
    val points: Int,
    val tick: Int,
)

/** The "Roll developed" summary. */
data class RoundResult(
    val passed: Boolean,
    val stars: Int,
    val photographed: Int,
    val fled: Int,
    val score: Int,
    val need: Int,
    val birds: Int,
    val photosTaken: Int,
    val bestQuality: Int,
    val okPhotos: Int,
    val goodPhotos: Int,
    val perfectPhotos: Int,
    val groupPhotos: Int,
    val durationTicks: Int,
    val protectedPhotos: Int = 0,
    val bossPages: Int = 0,
    val platesBroken: Int = 0,
)

/**
 * Complete simulation state. It contains the level, the accessibility options and every counter
 * that affects the result, so the hash covers everything a certificate depends on.
 */
data class GameState(
    val spec: LevelSpec,
    val options: GameOptions,
    val tick: Int,
    val phase: RoundPhase,
    val birds: List<BirdState>,
    val results: List<BirdResult>,
    val nextBirdIndex: Int,
    val photosUsed: Int,
    val score: Int,
    val photosTaken: Int,
    val birdsPhotographed: Int,
    val birdsFled: Int,
    val bestQuality: Int,
    val okPhotos: Int,
    val goodPhotos: Int,
    val perfectPhotos: Int,
    val groupPhotos: Int,
    val spawnRngState: Long,
    val weatherRngState: Long,
    val nextSpawnTick: Int,
    val summary: RoundResult? = null,
    /** State of the extra mechanics; null for a plain CLASSIC level. */
    val extras: RoundExtras? = null,
)

data class StepResult(val state: GameState, val events: List<GameEvent>)

/** Events consumed by the app for animation, sound and vibration. */
sealed interface GameEvent {
    data class BirdSpawned(val bird: BirdState) : GameEvent
    data class SnapTaken(val aimX: Int, val aimY: Int, val frame: PhotoFrame) : GameEvent
    data class PhotoResult(val photo: pt.tiago.ducknight.core.photo.PhotoResult) : GameEvent
    data class BirdStartled(val birdId: Int) : GameEvent
    data class BirdFled(val birdId: Int) : GameEvent
    data class BirdHidden(val birdId: Int) : GameEvent
    data class BirdRevealed(val birdId: Int) : GameEvent
    data class FilmEmpty(val birdId: Int) : GameEvent
    data class NeedsAdvance(val birdId: Int) : GameEvent
    data class Advanced(val birdId: Int) : GameEvent
    data class ComboChanged(val combo: Int, val multiplierPercent: Int) : GameEvent
    data class BossPhase(val phase: Int, val pages: Int) : GameEvent

    /** The Thief Crow took a page back from the album. */
    data class BossStole(val pages: Int) : GameEvent

    /** Thunder: a lightning flash breaks the V. */
    data class Lightning(val untilTick: Int) : GameEvent
    data class ClayBroken(val clayId: Int, val points: Int) : GameEvent
    data class CorkMiss(val pairId: Int) : GameEvent
    data class BirdResolved(val birdId: Int, val result: BirdResult) : GameEvent
    data class RoundEnded(val result: RoundResult) : GameEvent
}

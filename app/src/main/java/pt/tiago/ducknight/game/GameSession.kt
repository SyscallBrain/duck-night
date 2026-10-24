package pt.tiago.ducknight.game

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import pt.tiago.ducknight.audio.GameAudio
import pt.tiago.ducknight.audio.Haptics
import pt.tiago.ducknight.core.engine.GameEvent
import pt.tiago.ducknight.core.engine.GameState
import pt.tiago.ducknight.core.engine.InputAction
import pt.tiago.ducknight.core.engine.RoundPhase
import pt.tiago.ducknight.core.engine.Simulation
import pt.tiago.ducknight.core.engine.StepResult
import pt.tiago.ducknight.core.hash.StateHasher
import pt.tiago.ducknight.core.level.GameOptions
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.photo.PhotoOutcome
import pt.tiago.ducknight.core.replay.FailureReplay
import pt.tiago.ducknight.core.replay.InputLog
import pt.tiago.ducknight.core.replay.ResumeOutcome
import pt.tiago.ducknight.core.replay.ResumeService
import pt.tiago.ducknight.core.replay.SavedLevel
import pt.tiago.ducknight.data.SavedLevelStore
import pt.tiago.ducknight.progress.PhotoRecord
import pt.tiago.ducknight.progress.RoundExtras
import kotlin.math.abs

enum class DogCueKind { PHOTO, LAUGH, SHRUG, PROTECTED }

data class DogCue(
    val kind: DogCueKind,
    val quality: Int,
    val untilTick: Int,
    val worldX: Float,
)

data class Floater(
    val text: String,
    val x: Float,
    val y: Float,
    val startTick: Int,
)

data class Feather(
    val x: Float,
    val y: Float,
    val vx: Float,
    val vy: Float,
    val startTick: Int,
)

/** The read-only view the Canvas needs, shared by the live round and the failure replay. */
interface GameSceneState {
    val state: GameState
    val pinkUntilTick: Int
    val dogCue: DogCue?
    val lastAimX: Int?
    val lastAimY: Int?
    val aimUntilTick: Int
    val floater: Floater?
    fun visibleFeathers(): List<Feather>

    /** The Thief Crow took a page: the field shows it until this tick. */
    val bossStolenUntilTick: Int get() = 0

    /** Where the dog stands, in world units; null when it only appears for its reactions (replay). */
    val dogX: Float? get() = null
}

/**
 * Owns one active round: engine state, input queue, saved-level writes and the short-lived visual
 * cues the renderer consumes. The Compose loop calls [tick] at a fixed 60 Hz.
 */
private const val DOG_HOME_X = 24f
private const val DOG_MIN_X = 30f
private const val DOG_MAX_X = 176f
private const val DOG_SPEED = 4f

class GameSession(
    val spec: LevelSpec,
    private val options: GameOptions = GameOptions(),
    private val store: SavedLevelStore? = null,
    private val audio: GameAudio? = null,
    private val haptics: Haptics? = null,
    private val dogLaugh: Boolean = true,
    private val vibrationEnabled: Boolean = true,
    private val camera: String = "box",
    private val themeName: String = "LAGOON",
    val reducedMotion: Boolean = false,
    restored: SavedLevel? = null,
) : GameSceneState {
    override var state by mutableStateOf(Simulation.start(spec, options))
        private set
    var paused by mutableStateOf(false)
        private set
    override var dogCue by mutableStateOf<DogCue?>(null)
        private set
    override var lastAimX by mutableStateOf<Int?>(null)
        private set
    override var lastAimY by mutableStateOf<Int?>(null)
        private set
    override var aimUntilTick by mutableStateOf(0)
        private set
    override var pinkUntilTick by mutableStateOf(0)
        private set
    var flashUntilNanos by mutableStateOf(0L)
        private set
    override var floater by mutableStateOf<Floater?>(null)
        private set
    override var bossStolenUntilTick by mutableStateOf(0)
        private set

    /** The dog trots in from the left edge, waits in its corner and runs to every photo. */
    override var dogX by mutableStateOf<Float?>(-24f)
        private set

    private val pending = mutableListOf<InputAction>()
    private var log = InputLog()
    private val feathers = mutableListOf<Feather>()
    private val photos = mutableListOf<PhotoRecord>()
    private val seen = linkedSetOf<String>()
    private var bestGroup = 0
    private var goodStreak = 0
    private var bestGoodStreak = 0

    init {
        if (restored != null) restore(restored)
        state.birds.forEach { seen += it.species.id }
    }

    fun photoRecords(): List<PhotoRecord> = photos.toList()

    /** Builds the last-three-seconds replay from this round's exact input log. */
    fun buildReplay(focusBirdId: Int? = null): FailureReplay =
        FailureReplay.build(spec, options, log, state.tick, focusBirdId)

    fun seenSpecies(): List<String> = seen.toList()

    /** The largest group photo and the longest run of good or perfect photos in this round. */
    fun roundExtras(protectLevel: Boolean): RoundExtras =
        RoundExtras(bestGroup = bestGroup, goodStreak = bestGoodStreak, protectLevel = protectLevel)

    override fun visibleFeathers(): List<Feather> {
        val tick = state.tick
        feathers.removeAll { tick - it.startTick > 72 }
        return feathers
    }

    fun queueSnap(worldX: Int, worldY: Int) {
        if (paused || state.phase != RoundPhase.RUNNING) return
        val action = InputAction.Snap(worldX, worldY)
        log = log.append(state.tick + 1, action)
        pending += action
        lastAimX = worldX
        lastAimY = worldY
        aimUntilTick = state.tick + 15
    }

    fun queueAdvance() {
        if (paused || state.phase != RoundPhase.RUNNING) return
        val action = InputAction.Advance
        log = log.append(state.tick + 1, action)
        pending += action
    }

    fun tick() {
        if (paused || state.phase != RoundPhase.RUNNING) return
        val actions = pending.toList()
        pending.clear()
        val result: StepResult = Simulation.step(state, actions)
        state = result.state
        state.birds.forEach { seen += it.species.id }
        handle(result.events)
        moveDog()
        if (state.tick % 120 == 0) save()
    }

    private fun moveDog() {
        val cue = dogCue?.takeIf { state.tick <= it.untilTick }
        val target = cue?.worldX?.coerceIn(DOG_MIN_X, DOG_MAX_X) ?: DOG_HOME_X
        val current = dogX ?: target
        dogX = current + (target - current).coerceIn(-DOG_SPEED, DOG_SPEED)
    }

    fun pause() {
        if (state.phase != RoundPhase.RUNNING) return
        paused = true
        save()
    }

    fun resume() {
        if (state.phase == RoundPhase.RUNNING) paused = false
    }

    fun restart() {
        store?.clear()
        state = Simulation.start(spec, options)
        paused = false
        log = InputLog()
        pending.clear()
        feathers.clear()
        photos.clear()
        seen.clear()
        bestGroup = 0
        goodStreak = 0
        bestGoodStreak = 0
        dogCue = null
        dogX = -24f
        floater = null
        pinkUntilTick = 0
        lastAimX = null
        lastAimY = null
    }

    fun save() {
        val target = store ?: return
        if (state.phase != RoundPhase.RUNNING) return
        runCatching {
            target.save(
                SavedLevel(
                    spec = spec,
                    options = options,
                    tick = state.tick,
                    stateHash = StateHasher.hash(state),
                    log = log,
                ),
            )
        }
    }

    private fun restore(saved: SavedLevel) {
        when (val outcome = ResumeService.restore(saved)) {
            is ResumeOutcome.Restored -> {
                state = outcome.state
                // An action queued for the tick after the snapshot must not leak into a later
                // failure replay: the live resumed round dropped it with `pending`.
                log = InputLog(saved.log.entries.filter { it.tick <= saved.tick })
                // The photos taken before the app was closed still belong to the bird book.
                for (result in state.results) {
                    seen += result.speciesId
                    if (result.photographed) {
                        photos += PhotoRecord(
                            speciesId = result.speciesId,
                            quality = result.quality,
                            wingPhase = 0,
                            relativeX = 0,
                            relativeY = 0,
                            place = spec.place,
                            theme = themeName,
                            timestamp = System.currentTimeMillis(),
                        )
                    }
                }
                paused = true
            }
            is ResumeOutcome.HashMismatch,
            is ResumeOutcome.Invalid,
            -> {
                store?.clear()
            }
        }
    }

    private fun handle(events: List<GameEvent>) {
        for (event in events) {
            when (event) {
                is GameEvent.PhotoResult -> {
                    if (event.photo.hit) bestGroup = maxOf(bestGroup, event.photo.groupSize)
                    goodStreak = when (event.photo.outcome) {
                        PhotoOutcome.GOOD, PhotoOutcome.PERFECT -> goodStreak + 1
                        else -> 0
                    }
                    bestGoodStreak = maxOf(bestGoodStreak, goodStreak)
                    if (!reducedMotion) flashUntilNanos = System.nanoTime() + 90_000_000L
                    if (camera == "reflex") audio?.shutterWithFlash() else audio?.shutter()
                    if (vibrationEnabled) haptics?.shutter()
                    when (event.photo.outcome) {
                        PhotoOutcome.PERFECT -> {
                            audio?.perfect()
                            if (vibrationEnabled) haptics?.perfectPhoto()
                        }
                        PhotoOutcome.GOOD -> {
                            audio?.good()
                            if (vibrationEnabled) haptics?.goodPhoto()
                        }
                        PhotoOutcome.PROTECTED -> {
                            audio?.protected()
                            floater = Floater("−${event.photo.penalty}", event.photo.aimX.toFloat(), event.photo.aimY.toFloat(), state.tick)
                        }
                        PhotoOutcome.MISS -> audio?.empty()
                        else -> Unit
                    }
                    if (event.photo.hit) {
                        floater = Floater(
                            "+${event.photo.points}",
                            event.photo.aimX.toFloat(),
                            event.photo.aimY.toFloat(),
                            state.tick,
                        )
                        for (birdId in event.photo.birdIds) {
                            val bird = state.birds.firstOrNull { it.id == birdId } ?: continue
                            photos += PhotoRecord(
                                speciesId = bird.species.id,
                                quality = event.photo.quality,
                                wingPhase = bird.wingPhase,
                                relativeX = (bird.x / 256 - event.photo.aimX),
                                relativeY = (bird.y / 256 - event.photo.aimY),
                                place = spec.place,
                                theme = themeName,
                                timestamp = System.currentTimeMillis(),
                            )
                        }
                    }
                }
                is GameEvent.BirdStartled -> {
                    val bird = state.birds.firstOrNull { it.id == event.birdId }
                    if (bird != null && !reducedMotion) {
                        val bx = bird.x / 256f
                        val by = bird.y / 256f
                        repeat(8) { index ->
                            val angle = index * 0.78f
                            feathers += Feather(
                                x = bx,
                                y = by,
                                vx = kotlin.math.cos(angle) * (0.25f + index * 0.03f),
                                vy = kotlin.math.sin(angle) * 0.25f - 0.2f,
                                startTick = state.tick,
                            )
                        }
                    }
                }
                is GameEvent.BirdFled -> {
                    if (!reducedMotion) pinkUntilTick = state.tick + 36
                    audio?.fled()
                }
                is GameEvent.BirdResolved -> {
                    val bird = state.birds.firstOrNull { it.id == event.result.birdId }
                    val wx = bird?.let { it.x / 256f } ?: 120f
                    if (event.result.photographed) {
                        dogCue = DogCue(DogCueKind.PHOTO, event.result.quality, state.tick + 90, wx)
                        audio?.bark()
                    } else if (dogLaugh) {
                        dogCue = DogCue(DogCueKind.LAUGH, 0, state.tick + 84, wx)
                        audio?.laugh()
                    } else {
                        dogCue = DogCue(DogCueKind.SHRUG, 0, state.tick + 84, wx)
                    }
                }
                is GameEvent.RoundEnded -> {
                    store?.clear()
                    if (event.result.passed) audio?.passed() else audio?.failed()
                }
                is GameEvent.BossStole -> {
                    bossStolenUntilTick = state.tick + 90
                    audio?.laugh()
                }
                is GameEvent.NeedsAdvance -> Unit
                is GameEvent.Advanced -> audio?.advance()
                is GameEvent.FilmEmpty -> audio?.empty()
                else -> Unit
            }
        }
    }
}

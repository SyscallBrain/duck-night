package pt.tiago.ducknight.core.engine

import pt.tiago.ducknight.core.fair.ClayPigeon
import pt.tiago.ducknight.core.fair.ClayStatus
import pt.tiago.ducknight.core.fixed.CEILING
import pt.tiago.ducknight.core.fixed.SIDE_MARGIN
import pt.tiago.ducknight.core.fixed.TrigTable
import pt.tiago.ducknight.core.fixed.VEGETATION_Y
import pt.tiago.ducknight.core.fixed.WORLD_HEIGHT
import pt.tiago.ducknight.core.fixed.WORLD_WIDTH
import pt.tiago.ducknight.core.fixed.clamp
import pt.tiago.ducknight.core.fixed.distance
import pt.tiago.ducknight.core.fixed.fpsToFxPerTick
import pt.tiago.ducknight.core.fixed.fx
import pt.tiago.ducknight.core.fixed.toUnits
import pt.tiago.ducknight.core.flight.BirdState
import pt.tiago.ducknight.core.flight.BirdStatus
import pt.tiago.ducknight.core.flight.BossStyle
import pt.tiago.ducknight.core.flight.Flight
import pt.tiago.ducknight.core.level.GameOptions
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.level.LevelType
import pt.tiago.ducknight.core.level.Modifier
import pt.tiago.ducknight.core.level.SpeciesWeight
import pt.tiago.ducknight.core.level.TypeRules
import pt.tiago.ducknight.core.photo.PhotoOutcome
import pt.tiago.ducknight.core.photo.PhotoResolver
import pt.tiago.ducknight.core.random.SplitMix64
import pt.tiago.ducknight.core.species.FlightPattern
import pt.tiago.ducknight.core.species.Species
import pt.tiago.ducknight.core.species.SpeciesCatalog
import pt.tiago.ducknight.core.weather.CoverBox
import pt.tiago.ducknight.core.weather.FogBank

/**
 * The single simulation. CLASSIC is the original path; the other level types and
 * the modifiers are layered on the same state so that replay, hashing and certificates stay valid.
 */
object Simulation {
    const val PHOTO_EXHAUST_FLEE_TICKS = 27
    const val SPAWN_GAP_TICKS = 30
    private const val TIMED_GAP_TICKS = 10
    private const val PROTECTED_GAP_TICKS = 240
    private const val NIGHT_RADIUS = 46
    private const val NIGHT_TRAIL_TICKS = 24
    const val CLAY_PAIR_GAP_TICKS = 45
    private const val CLAY_GRAVITY = 18
    private const val BOSS_THREE_STAR_QUALITY = 80

    /**
     * Launch speed of a clay: its arc peaks near y = 40 and stays on screen for the whole flight of
     * 2.2–3.5 s. At 420 u/s it left the top of the field in 0.6 s.
     */
    private const val CLAY_LAUNCH_UPS = 300
    /**
     * Cork tolerance at full size, shrinking with the clay. 8 units was 1–3 mm on a phone, too small
     * for a finger; 18 matches the drawn clay (radius 13 × scale) plus a finger's margin.
     */
    private const val CLAY_TOLERANCE_UNITS = 18

    fun start(spec: LevelSpec, options: GameOptions = GameOptions()): GameState {
        val attemptSeed = SplitMix64.split(spec.seed, spec.attemptIndex.toLong())
        val initial = GameState(
            spec = spec,
            options = options,
            tick = 0,
            phase = RoundPhase.RUNNING,
            birds = emptyList(),
            results = emptyList(),
            nextBirdIndex = 0,
            photosUsed = 0,
            score = 0,
            photosTaken = 0,
            birdsPhotographed = 0,
            birdsFled = 0,
            bestQuality = 0,
            okPhotos = 0,
            goodPhotos = 0,
            perfectPhotos = 0,
            groupPhotos = 0,
            spawnRngState = SplitMix64.split(attemptSeed, 10L),
            weatherRngState = SplitMix64.split(attemptSeed, 11L),
            nextSpawnTick = 0,
            extras = buildExtras(spec, attemptSeed),
        )
        val started = when (spec.type) {
            LevelType.CLAY -> spawnClayPair(initial)
            LevelType.BOSS -> spawnBoss(initial)
            LevelType.DOUBLE -> spawnDouble(initial)
            LevelType.FLOCK -> spawnFlock(initial)
            else -> spawnNext(initial)
        }
        return refreshVisibility(started)
    }

    /** The lever is part of the level, or of the Royal Snow Goose. */
    private fun manualAdvance(state: GameState): Boolean =
        Modifier.MANUAL_ADVANCE in state.spec.modifiers || bossStyle(state) == BossStyle.SNOW_GOOSE

    /** Good photos in a row raise the multiplier with golden ducks, and with the Golden Eider. */
    private fun goldenCombo(state: GameState): Boolean =
        Modifier.GOLDEN in state.spec.modifiers || bossStyle(state) == BossStyle.GOLDEN_EIDER

    private fun bossStyle(state: GameState): BossStyle =
        if (state.spec.type == LevelType.BOSS) BossStyle.of(state.spec.species.first().id) else BossStyle.PLAIN

    private fun buildExtras(spec: LevelSpec, attemptSeed: Long): RoundExtras? {
        if (spec.rules == null && spec.modifiers.isEmpty()) return null
        val rules = spec.rules ?: TypeRules()
        val weather = SplitMix64(SplitMix64.split(attemptSeed, 11L))
        val sign = if (weather.nextInt(2) == 0) 1 else -1
        val wind = if (Modifier.WIND in spec.modifiers) {
            sign * fpsToFxPerTick(rules.windStrength)
        } else {
            0
        }
        val fog = if (Modifier.FOG in spec.modifiers) {
            rules.fogBanks.ifEmpty { listOf(FogBank(60, 40), FogBank(150, 50)) }
        } else {
            emptyList()
        }
        val cover = if (Modifier.COVER in spec.modifiers) {
            rules.coverBoxes.ifEmpty {
                listOf(CoverBox(20, 190, 42, 44), CoverBox(178, 196, 44, 38))
            }
        } else {
            emptyList()
        }
        return RoundExtras(
            rules = rules,
            filmLeft = if (spec.type == LevelType.FILM) rules.filmFrames else null,
            timedEndTick = if (spec.type == LevelType.TIMED) rules.timedTicks else null,
            windFx = wind,
            windNextTick = 60,
            windRngState = weather.state,
            fogBanks = fog,
            coverBoxes = cover,
            boss = if (spec.type == LevelType.BOSS) BossState() else null,
            corksLeft = if (spec.type == LevelType.CLAY) rules.corksPerPair else 0,
            nextProtectedSpawnTick = if (
                spec.type == LevelType.PROTECT || Modifier.PROTECTED in spec.modifiers
            ) {
                30
            } else {
                0
            },
        )
    }

    fun step(state: GameState, inputs: List<InputAction>): StepResult {
        if (state.phase == RoundPhase.FINISHED) return StepResult(state, emptyList())
        val events = mutableListOf<GameEvent>()
        var current = state.copy(tick = state.tick + 1)
        current = updateWind(current)
        for (input in inputs) current = applyInput(current, input, events)
        current = updateNightLight(current)
        current = if (state.spec.type == LevelType.CLAY) {
            advanceClays(current, events)
        } else {
            var next = advanceBirds(current, events)
            next = advanceBoss(current, next, events)
            next = spawnProtected(next, events)
            next = spawnIfNeeded(next, events)
            next
        }
        current = finishIfNeeded(current, events)
        return StepResult(refreshVisibility(current), events)
    }

    private fun updateWind(state: GameState): GameState {
        val extras = state.extras ?: return state
        if (Modifier.WIND !in state.spec.modifiers) return state
        if (state.tick < extras.windNextTick) return state
        val rng = SplitMix64(extras.windRngState)
        val sign = if (rng.nextInt(2) == 0) 1 else -1
        val wind = sign * fpsToFxPerTick(extras.rules.windStrength)
        return state.copy(
            extras = extras.copy(
                windFx = wind,
                windNextTick = state.tick + 60,
                windRngState = rng.state,
            ),
        )
    }

    private fun applyInput(state: GameState, input: InputAction, events: MutableList<GameEvent>): GameState =
        when (input) {
            is InputAction.Snap -> if (state.spec.type == LevelType.CLAY) {
                claySnap(state, input, events)
            } else {
                birdSnap(state, input, events)
            }
            InputAction.Advance -> {
                val extras = state.extras
                if (extras?.needsAdvance == true) {
                    events += GameEvent.Advanced(state.birds.firstOrNull()?.id ?: -1)
                    state.copy(extras = extras.copy(needsAdvance = false))
                } else {
                    state
                }
            }
            else -> state
        }

    private fun birdSnap(state: GameState, snap: InputAction.Snap, events: MutableList<GameEvent>): GameState {
        if (state.phase != RoundPhase.RUNNING) return state
        val activeBirds = state.birds.filter { it.status == BirdStatus.FLYING && !it.species.protected }
        if (activeBirds.isEmpty()) return state
        val extras = state.extras
        if (extras?.needsAdvance == true) return state
        if (extras?.filmLeft != null && extras.filmLeft <= 0) return state
        if (state.spec.type == LevelType.TIMED && extras != null && state.tick < extras.cooldownUntil) return state
        if (state.spec.type == LevelType.BOSS && state.photosUsed >= state.spec.photosPerBird) return state

        val frame = PhotoResolver.frame(state.spec, state.options)
        events += GameEvent.SnapTaken(snap.x, snap.y, frame)
        val photo = PhotoResolver.resolve(
            birds = state.birds,
            aimX = snap.x,
            aimY = snap.y,
            spec = state.spec,
            options = state.options,
            visible = { bird ->
                !bird.hidden || (
                    Modifier.NIGHT in state.spec.modifiers &&
                        distance(bird.x, bird.y, fx(snap.x), fx(snap.y)) <= fx(NIGHT_RADIUS)
                    )
            },
        )
        val usedBefore = state.photosUsed
        val photosUsed = usedBefore + 1
        var photosTaken = state.photosTaken + 1
        var extrasAfter = extras
        if (extrasAfter != null) {
            if (extrasAfter.filmLeft != null) {
                val left = (extrasAfter.filmLeft - 1).coerceAtLeast(0)
                extrasAfter = extrasAfter.copy(filmLeft = left)
                if (left == 0) events += GameEvent.FilmEmpty(activeBirds.first().id)
            }
            if (state.spec.type == LevelType.TIMED) {
                extrasAfter = extrasAfter.copy(cooldownUntil = state.tick + extrasAfter.rules.photoCooldownTicks)
            }
            if (manualAdvance(state)) {
                extrasAfter = extrasAfter.copy(needsAdvance = true)
            }
            if (state.spec.type == LevelType.FLOCK) {
                extrasAfter = extrasAfter.copy(formationBroken = true)
            }
            if (Modifier.NIGHT in state.spec.modifiers) {
                val nearest = state.birds
                    .filter { it.status == BirdStatus.FLYING && !it.species.protected }
                    .minByOrNull { distance(it.x, it.y, fx(snap.x), fx(snap.y)) }
                extrasAfter = extrasAfter.copy(
                    nightX = snap.x,
                    nightY = snap.y,
                    nightFollowId = nearest?.id ?: -1,
                    nightFollowUntil = state.tick + NIGHT_TRAIL_TICKS,
                )
            }
        }
        if (Modifier.NIGHT in state.spec.modifiers) {
            // The snap uses the aim as the lamp centre, before the trail moves it.
            events += GameEvent.PhotoResult(photo)
        } else {
            events += GameEvent.PhotoResult(photo)
        }

        if (photo.outcome == PhotoOutcome.PROTECTED) {
            if (extrasAfter != null) {
                extrasAfter = extrasAfter.copy(protectedPhotoCount = extrasAfter.protectedPhotoCount + 1)
            }
            val score = state.score + photo.points
            if (manualAdvance(state) && extrasAfter != null) {
                events += GameEvent.NeedsAdvance(activeBirds.first().id)
            }
            return state.copy(
                score = score,
                photosUsed = photosUsed,
                photosTaken = photosTaken,
                extras = extrasAfter,
            )
        }

        if (!photo.hit) {
            var birds = state.birds
            var comboReset = false
            if (extrasAfter?.filmLeft == null && photosUsed >= state.spec.photosPerBird) {
                val escapeAt = state.tick + PHOTO_EXHAUST_FLEE_TICKS
                birds = birds.map {
                    if (it.status == BirdStatus.FLYING && !it.species.protected) it.copy(escapeAtTick = escapeAt) else it
                }
                events += GameEvent.FilmEmpty(activeBirds.first().id)
            }
            if (extrasAfter != null && goldenCombo(state)) {
                comboReset = extrasAfter.combo > 0
                extrasAfter = extrasAfter.copy(combo = 0, multiplierPercent = 100)
                if (comboReset) events += GameEvent.ComboChanged(0, 100)
            }
            if (manualAdvance(state) && extrasAfter != null) {
                events += GameEvent.NeedsAdvance(activeBirds.first().id)
            }
            var next = state.copy(
                birds = birds,
                photosUsed = photosUsed,
                photosTaken = photosTaken,
                extras = extrasAfter,
            )
            if (state.spec.type == LevelType.FLOCK) next = breakFormation(next)
            return next
        }

        // A normal target was hit. The protected count may still apply to a mixed photo.
        var score = state.score
        var birds = state.birds
        val results = state.results.toMutableList()
        var photographed = state.birdsPhotographed
        var ok = state.okPhotos
        var good = state.goodPhotos
        var perfect = state.perfectPhotos
        var points = photo.points
        var multiplier = 100
        if (extrasAfter != null && goldenCombo(state)) {
            val goodShot = photo.outcome == PhotoOutcome.GOOD || photo.outcome == PhotoOutcome.PERFECT
            if (goodShot) {
                val combo = (extrasAfter.combo + 1).coerceAtMost(2)
                multiplier = when (combo) {
                    0 -> 100
                    1 -> 150
                    else -> 200
                }
                extrasAfter = extrasAfter.copy(combo = combo, multiplierPercent = multiplier)
                events += GameEvent.ComboChanged(combo, multiplier)
            } else {
                extrasAfter = extrasAfter.copy(combo = 0, multiplierPercent = 100)
                events += GameEvent.ComboChanged(0, 100)
            }
            points = photo.points * multiplier / 100
        }
        score += points
        if (photo.protectedCount > 0 && extrasAfter != null) {
            extrasAfter = extrasAfter.copy(
                protectedPhotoCount = extrasAfter.protectedPhotoCount + photo.protectedCount,
            )
        }
        val photoForEvent = photo.copy(points = points)
        // Replace the base PhotoResult emitted above with the multiplier-adjusted points. A combo
        // event may follow it, so it is found by type rather than taken as the last event.
        val photoEvent = events.indexOfLast { it is GameEvent.PhotoResult }
        events[photoEvent] = GameEvent.PhotoResult(photoForEvent)

        if (state.spec.type == LevelType.BOSS) {
            val boss = extrasAfter?.boss ?: BossState()
            val style = BossStyle.of(state.spec.species.first().id)
            val bossIds = birds.filter { it.bossPhase > 0 && it.status == BirdStatus.FLYING }.map { it.id }
            val bossInPhoto = photo.birdIds.any { it in bossIds }
            // A page needs a good photo of the legendary bird; the Thunder Flock wants a group instead.
            // No page while the legendary bird is out of sight (resting after a page, or under water).
            val resting = birds.any { it.id in bossIds && it.diveUntilTick > state.tick }
            val page = !resting && photo.quality >= 60 && if (style == BossStyle.THUNDER_FLOCK) {
                photo.groupSize >= BossStyle.THUNDER_GROUP
            } else {
                bossInPhoto
            }
            val pages = (boss.pages + if (page) 1 else 0).coerceAtMost(extrasAfter?.rules?.goalPages ?: 3)
            val phase = (pages + 1).coerceAtMost(extrasAfter?.rules?.bossPhases ?: 3)
            extrasAfter = extrasAfter?.copy(
                boss = boss.copy(
                    phase = phase,
                    pages = pages,
                    photos = boss.photos + 1,
                    lastPageTick = if (page) state.tick else boss.lastPageTick,
                ),
            )
            if (page) events += GameEvent.BossPhase(phase, pages)
            val complete = pages >= (extrasAfter?.rules?.goalPages ?: 3)
            birds = birds.map { if (it.id in bossIds) it.copy(bossPhase = phase) else it }
            if (page && !complete) {
                // After each page the legendary bird drops out of sight for a moment and comes back
                // with the next phase, so every phase of the album is really played.
                // Its escorts go with it, so there is nothing left to waste photos on.
                birds = birds.map {
                    if (it.status == BirdStatus.FLYING) it.copy(diveUntilTick = state.tick + BossStyle.PAGE_REST_TICKS) else it
                }
            }
            if (page && style == BossStyle.TWIN_TEALS && bossIds.size == 2) {
                // The twins swap places as soon as one of them is photographed.
                val first = birds.first { it.id == bossIds[0] }
                val second = birds.first { it.id == bossIds[1] }
                birds = birds.map {
                    when (it.id) {
                        first.id -> it.copy(x = second.x, y = second.y)
                        second.id -> it.copy(x = first.x, y = first.y)
                        else -> it
                    }
                }
            }
            if (complete) {
                val mainId = photo.birdIds.firstOrNull { it in bossIds } ?: bossIds.firstOrNull()
                birds = birds.map {
                    when {
                        it.id in bossIds -> Flight.startle(it).copy(quality = photo.quality, photosUsed = photosUsed, bossPhase = phase)
                        // Escorts leave with their leader; they never count as fled birds.
                        it.status == BirdStatus.FLYING -> Flight.flee(it, Flight.FLEE_SPEED_UPS)
                        else -> it
                    }
                }
                val bossBird = state.birds.firstOrNull { it.id == mainId }
                if (bossBird != null) {
                    val result = BirdResult(
                        bossBird.id, bossBird.species.id, true, photo.quality, points, state.tick,
                    )
                    results += result
                    photographed++
                    events += GameEvent.BirdStartled(bossBird.id)
                    events += GameEvent.BirdResolved(bossBird.id, result)
                }
            }
            if (manualAdvance(state) && extrasAfter != null) {
                events += GameEvent.NeedsAdvance(activeBirds.first().id)
            }
            if (state.spec.type == LevelType.FLOCK) state.copy()
            return state.copy(
                birds = birds,
                results = results,
                score = score,
                photosUsed = photosUsed,
                photosTaken = photosTaken,
                birdsPhotographed = photographed,
                bestQuality = maxOf(state.bestQuality, photo.quality),
                okPhotos = ok + if (photo.outcome == PhotoOutcome.OK) 1 else 0,
                goodPhotos = good + if (photo.outcome == PhotoOutcome.GOOD) 1 else 0,
                perfectPhotos = perfect + if (photo.outcome == PhotoOutcome.PERFECT) 1 else 0,
                extras = extrasAfter,
            )
        }

        val mainId = photo.mainBirdId
        for (birdId in photo.birdIds) {
            val bird = birds.firstOrNull { it.id == birdId } ?: continue
            val startled = Flight.startle(bird).copy(quality = photo.quality, photosUsed = photosUsed)
            val birdPoints = if (birdId == mainId) points else 0
            val result = BirdResult(
                birdId = birdId,
                speciesId = bird.species.id,
                photographed = true,
                quality = photo.quality,
                points = birdPoints,
                tick = state.tick,
            )
            birds = birds.map { if (it.id == birdId) startled else it }
            results += result
            photographed++
            events += GameEvent.BirdStartled(birdId)
            events += GameEvent.BirdResolved(birdId, result)
        }
        when (photo.outcome) {
            PhotoOutcome.OK -> ok++
            PhotoOutcome.GOOD -> good++
            PhotoOutcome.PERFECT -> perfect++
            else -> Unit
        }
        if (manualAdvance(state) && extrasAfter != null) {
            events += GameEvent.NeedsAdvance(activeBirds.first().id)
        }
        var next = state.copy(
            birds = birds,
            results = results,
            score = score,
            photosUsed = photosUsed,
            photosTaken = photosTaken,
            birdsPhotographed = photographed,
            bestQuality = maxOf(state.bestQuality, photo.quality),
            okPhotos = ok,
            goodPhotos = good,
            perfectPhotos = perfect,
            groupPhotos = state.groupPhotos + if (photo.groupSize > 1 && photo.protectedCount == 0) 1 else 0,
            extras = extrasAfter,
        )
        if (state.spec.type == LevelType.FLOCK) next = breakFormation(next)
        return next
    }

    private fun breakFormation(state: GameState): GameState {
        if (state.extras == null) return state
        val birds = state.birds.map {
            if (it.pattern == FlightPattern.FORMATION) {
                it.copy(pattern = FlightPattern.ZIGZAG, leaderId = -1)
            } else {
                it
            }
        }
        return state.copy(
            birds = birds,
            extras = state.extras.copy(formationBroken = true),
        )
    }

    private fun updateNightLight(state: GameState): GameState {
        val extras = state.extras ?: return state
        if (Modifier.NIGHT !in state.spec.modifiers) return state
        if (extras.nightFollowId < 0 || state.tick > extras.nightFollowUntil) {
            return if (extras.nightFollowId >= 0) {
                state.copy(extras = extras.copy(nightFollowId = -1))
            } else {
                state
            }
        }
        val bird = state.birds.firstOrNull { it.id == extras.nightFollowId } ?: return state
        return state.copy(
            extras = extras.copy(nightX = toUnits(bird.x), nightY = toUnits(bird.y)),
        )
    }

    private fun advanceBirds(state: GameState, events: MutableList<GameEvent>): GameState {
        if (state.extras == null) return advanceClassicBirds(state, events)
        if (state.birds.isEmpty()) return state
        val results = state.results.toMutableList()
        var fledCount = state.birdsFled
        val kept = mutableListOf<BirdState>()
        for (bird in state.birds) {
            var current = bird
            val beforeHidden = current.hidden
            if (current.crossing) {
                if (current.status == BirdStatus.STARTLED) {
                    current = current.copy(statusTicks = current.statusTicks + 1)
                    if (current.statusTicks >= Flight.STARTLE_TICKS) {
                        current = current.copy(status = BirdStatus.GONE)
                        if (!current.species.protected && results.none { it.birdId == current.id }) {
                            val result = fledResult(current, state.tick)
                            results += result
                            fledCount++
                            events += GameEvent.BirdResolved(current.id, result)
                        }
                    }
                }
                if (current.status == BirdStatus.GONE) continue
                val leader = current.leaderId.takeIf { it >= 0 }?.let { id -> state.birds.firstOrNull { it.id == id } }
                current = if (leader != null && !state.extras.formationBroken) {
                    current.copy(
                        x = leader.x + current.formationOffsetX,
                        y = leader.y + current.formationOffsetY,
                        vx = leader.vx,
                        vy = leader.vy,
                        ageTicks = current.ageTicks + 1,
                        wingPhase = (current.wingPhase + 614) and (TrigTable.STEPS - 1),
                    )
                } else {
                    current.copy(
                        x = current.x + current.vx,
                        y = current.y + current.vy,
                        ageTicks = current.ageTicks + 1,
                        wingPhase = (current.wingPhase + 614) and (TrigTable.STEPS - 1),
                    )
                }
                if (current.x < -fx(current.species.radius) || current.x > fx(WORLD_WIDTH + current.species.radius)) {
                    current = current.copy(status = BirdStatus.GONE)
                    if (!current.species.protected && results.none { it.birdId == current.id }) {
                        val result = fledResult(current, state.tick)
                        results += result
                        fledCount++
                        events += GameEvent.BirdResolved(current.id, result)
                    }
                }
                if (current.status != BirdStatus.GONE) kept += current
                continue
            }
            if (current.pattern == FlightPattern.DIVE_SURFACE && current.diveUntilTick > state.tick) {
                current = current.copy(
                    ageTicks = current.ageTicks + 1,
                    wingPhase = (current.wingPhase + 614) and (TrigTable.STEPS - 1),
                    hidden = true,
                )
                kept += current
                continue
            }
            if (current.pattern == FlightPattern.DIVE_SURFACE && current.diveUntilTick > 0) {
                val rng = SplitMix64(current.rngState)
                current = current.copy(
                    hidden = false,
                    diveUntilTick = 0,
                    pattern = FlightPattern.ZIGZAG,
                    x = fx(40 + rng.nextInt(160)),
                    y = fx(206),
                    rngState = rng.state,
                )
                events += GameEvent.BirdRevealed(current.id)
            }
            val leader = current.leaderId.takeIf { it >= 0 }?.let { id -> state.birds.firstOrNull { it.id == id } }
            if (current.leaderId >= 0 && leader != null && state.extras.formationBroken.not()) {
                current = current.copy(
                    x = clamp(
                        leader.x + current.formationOffsetX,
                        fx(SIDE_MARGIN),
                        fx(WORLD_WIDTH - SIDE_MARGIN),
                    ),
                    y = clamp(
                        leader.y + current.formationOffsetY,
                        fx(CEILING),
                        fx(VEGETATION_Y),
                    ),
                    vx = leader.vx,
                    vy = leader.vy,
                    ageTicks = current.ageTicks + 1,
                    wingPhase = (current.wingPhase + 614) and (TrigTable.STEPS - 1),
                )
            } else {
                if (current.status == BirdStatus.FLYING &&
                    current.escapeAtTick > 0 &&
                    state.tick >= current.escapeAtTick
                ) {
                    current = Flight.flee(current, Flight.TIMEOUT_FLEE_SPEED_UPS)
                    events += GameEvent.BirdFled(current.id)
                    val result = fledResult(current, state.tick)
                    results += result
                    fledCount++
                    events += GameEvent.BirdResolved(current.id, result)
                } else {
                    val before = current.status
                    current = Flight.step(current, state.spec, current.id)
                    if (before == BirdStatus.FLYING && current.status == BirdStatus.FLEEING) {
                        events += GameEvent.BirdFled(current.id)
                        val result = fledResult(current, state.tick)
                        results += result
                        fledCount++
                        events += GameEvent.BirdResolved(current.id, result)
                    } else if (before == BirdStatus.STARTLED && current.status == BirdStatus.FLEEING) {
                        events += GameEvent.BirdFled(current.id)
                    }
                }
            }
            val wind = state.extras.windFx
            if (wind != 0 && current.status == BirdStatus.FLYING) {
                current = current.copy(
                    x = clamp(current.x + wind, fx(SIDE_MARGIN), fx(WORLD_WIDTH - SIDE_MARGIN)),
                )
            }
            if (current.status != BirdStatus.GONE) {
                current = current.copy(hidden = computeHidden(current, state))
                if (beforeHidden != current.hidden) {
                    events += if (current.hidden) GameEvent.BirdHidden(current.id) else GameEvent.BirdRevealed(current.id)
                }
                kept += current
            }
        }
        var nextSpawn = state.nextSpawnTick
        val noTargets = kept.none { !it.species.protected }
        if (noTargets && state.birds.isNotEmpty() && state.nextBirdIndex < state.spec.birds &&
            state.spec.type != LevelType.CLAY && state.spec.type != LevelType.BOSS
        ) {
            nextSpawn = state.tick + if (state.spec.type == LevelType.TIMED) TIMED_GAP_TICKS else SPAWN_GAP_TICKS
        }
        return state.copy(
            birds = kept,
            results = results,
            birdsFled = fledCount,
            nextSpawnTick = nextSpawn,
        )
    }

    /** The original CLASSIC path, unchanged: no modifiers and no extra state. */
    private fun advanceClassicBirds(state: GameState, events: MutableList<GameEvent>): GameState {
        if (state.birds.isEmpty()) return state
        val results = state.results.toMutableList()
        var fledCount = state.birdsFled
        val kept = mutableListOf<BirdState>()
        for (bird in state.birds) {
            var current = bird
            if (current.status == BirdStatus.FLYING &&
                current.escapeAtTick > 0 &&
                state.tick >= current.escapeAtTick
            ) {
                current = Flight.flee(current, Flight.TIMEOUT_FLEE_SPEED_UPS)
                events += GameEvent.BirdFled(current.id)
                val result = fledResult(current, state.tick)
                results += result
                fledCount++
                events += GameEvent.BirdResolved(current.id, result)
            } else {
                val before = current.status
                current = Flight.step(current, state.spec, current.id)
                if (before == BirdStatus.FLYING && current.status == BirdStatus.FLEEING) {
                    events += GameEvent.BirdFled(current.id)
                    val result = fledResult(current, state.tick)
                    results += result
                    fledCount++
                    events += GameEvent.BirdResolved(current.id, result)
                } else if (before == BirdStatus.STARTLED && current.status == BirdStatus.FLEEING) {
                    events += GameEvent.BirdFled(current.id)
                }
            }
            if (current.status != BirdStatus.GONE) kept += current
        }
        var nextSpawn = state.nextSpawnTick
        if (kept.isEmpty() && state.birds.isNotEmpty() && state.nextBirdIndex < state.spec.birds) {
            nextSpawn = state.tick + SPAWN_GAP_TICKS
        }
        return state.copy(
            birds = kept,
            results = results,
            birdsFled = fledCount,
            nextSpawnTick = nextSpawn,
        )
    }

    private fun spawnIfNeeded(state: GameState, events: MutableList<GameEvent>): GameState {
        if (state.phase != RoundPhase.RUNNING) return state
        if (state.spec.type == LevelType.CLAY || state.spec.type == LevelType.BOSS) return state
        if (state.extras == null) {
            if (state.birds.isNotEmpty()) return state
            if (state.nextBirdIndex >= state.spec.birds) return state
            if (state.tick < state.nextSpawnTick) return state
            val next = spawnNext(state)
            events += GameEvent.BirdSpawned(next.birds.single())
            return next
        }
        val hasTarget = state.birds.any { !it.species.protected && it.status != BirdStatus.GONE }
        if (hasTarget) return state
        if (state.spec.type == LevelType.TIMED) {
            if (state.tick < (state.extras.timedEndTick ?: Int.MAX_VALUE) && state.tick >= state.nextSpawnTick) {
                val next = spawnNext(state)
                events += GameEvent.BirdSpawned(next.birds.single())
                return next
            }
            return state
        }
        if (state.nextBirdIndex >= state.spec.birds) return state
        if (state.tick < state.nextSpawnTick) return state
        val next = when (state.spec.type) {
            LevelType.DOUBLE -> spawnDouble(state)
            LevelType.FLOCK -> spawnFlock(state)
            else -> spawnNext(state)
        }
        next.birds.forEach { events += GameEvent.BirdSpawned(it) }
        return next
    }

    private fun spawnNext(state: GameState): GameState {
        val next = spawnBird(state, state.nextBirdIndex, null, null)
        return next.copy(birds = listOf(next.birds.single()), photosUsed = 0)
    }

    private fun spawnBird(
        state: GameState,
        id: Int,
        speciesOverride: Species?,
        patternOverride: FlightPattern?,
    ): GameState {
        val rng = SplitMix64(state.spawnRngState)
        val species = speciesOverride ?: chooseSpecies(state.spec.species, rng)
        val patterns = state.spec.allowedPatterns()
        val pattern = when {
            patternOverride != null -> patternOverride
            species.dives && Modifier.DIVE in state.spec.modifiers -> FlightPattern.DIVE_SURFACE
            else -> patterns[rng.nextInt(patterns.size)]
        }
        val spawn = Flight.spawn(
            id = id,
            species = species,
            pattern = pattern,
            spec = state.spec,
            birdIndex = id,
            spawnRngState = rng.state,
        )
        val diveRng = if (species.dives && Modifier.DIVE in state.spec.modifiers) {
            SplitMix64(spawn.spawnRngState)
        } else {
            null
        }
        val bird = if (diveRng != null) {
            spawn.bird.copy(
                hidden = true,
                diveUntilTick = state.tick + 60 + diveRng.nextInt(121),
                y = fx(VEGETATION_Y),
            )
        } else {
            spawn.bird
        }
        return state.copy(
            birds = listOf(bird),
            nextBirdIndex = id + 1,
            spawnRngState = diveRng?.state ?: spawn.spawnRngState,
        )
    }

    private fun spawnDouble(state: GameState): GameState {
        val first = state.nextBirdIndex
        var next = spawnBird(state, first, null, null)
        val firstBird = next.birds.single()
        next = spawnBird(next, first + 1, null, null)
        val secondBird = next.birds.single()
        return next.copy(
            birds = listOf(firstBird, secondBird),
            nextBirdIndex = first + 2,
            photosUsed = 0,
        )
    }

    private fun spawnFlock(state: GameState): GameState {
        val first = state.nextBirdIndex
        val size = (state.extras?.rules?.flockSize ?: 5).coerceAtMost(state.spec.birds - first)
        if (size <= 0) return state
        var next = spawnBird(state, first, null, FlightPattern.GLIDE)
        val leaderSpawn = next.birds.single()
        val leader = leaderSpawn.copy(
            crossing = true,
            x = fx(SIDE_MARGIN),
            y = fx(120),
            vx = leaderSpawn.baseSpeed,
            vy = 0,
            heading = 0,
        )
        next = next.copy(birds = listOf(leader))
        var rngState = next.spawnRngState
        val birds = mutableListOf(leader)
        for (i in 1 until size) {
            val rng = SplitMix64(rngState)
            val spawn = Flight.spawn(
                id = first + i,
                species = leader.species,
                pattern = FlightPattern.FORMATION,
                spec = state.spec,
                birdIndex = first + i,
                spawnRngState = rng.state,
            )
            rngState = spawn.spawnRngState
            val offsetX = -i * 10
            val offsetY = when {
                i % 4 == 1 -> -8
                i % 4 == 3 -> 8
                else -> 0
            }
            birds += spawn.bird.copy(
                x = clamp(fx(120 + offsetX), fx(SIDE_MARGIN), fx(WORLD_WIDTH - SIDE_MARGIN)),
                y = clamp(fx(206 + offsetY), fx(CEILING), fx(VEGETATION_Y)),
                leaderId = leader.id,
                formationOffsetX = fx(offsetX),
                formationOffsetY = fx(offsetY),
                crossing = true,
                vx = leader.vx,
                vy = leader.vy,
            )
        }
        next = next.copy(
            birds = birds,
            nextBirdIndex = first + size,
            spawnRngState = rngState,
            photosUsed = 0,
        )
        return next
    }

    private fun spawnBoss(state: GameState): GameState {
        val bossSpecies = SpeciesCatalog.byId(state.spec.species.first().id)
        val style = BossStyle.of(bossSpecies.id)
        var next = spawnBird(state, 0, bossSpecies, FlightPattern.BOSS)
        val boss = next.birds.single().copy(bossPhase = 1)
        var birds = listOf(boss)
        if (style == BossStyle.TWIN_TEALS) {
            // The second twin starts mirrored across the field and flies its own course.
            val twin = spawnBird(next, 1, bossSpecies, FlightPattern.BOSS)
            next = twin
            birds = birds + twin.birds.single().copy(bossPhase = 1, x = fx(WORLD_WIDTH) - boss.x)
        }
        next = next.copy(
            birds = birds,
            nextBirdIndex = birds.size,
            extras = next.extras?.copy(
                boss = BossState(
                    phase = 1,
                    pages = 0,
                    photos = 0,
                    nextStormTick = if (style == BossStyle.THUNDER_FLOCK) BossStyle.stormGap(1) else 0,
                ),
            ),
        )
        if (style == BossStyle.THUNDER_FLOCK) next = addEscorts(next, boss, bossSpecies, THUNDER_ESCORTS)
        return next
    }

    /** Slots of the V behind the leader, in world units: left and right wings, two deep. */
    private val escortSlots = listOf(-14 to 10, 14 to 10, -28 to 20, 28 to 20, -42 to 30)

    private const val THUNDER_ESCORTS = 5
    private const val WIND_ESCORTS = 4

    private fun addEscorts(state: GameState, leader: BirdState, species: Species, count: Int): GameState {
        val escorts = (0 until count).map { index ->
            val (dx, dy) = escortSlots[index]
            leader.copy(
                id = state.nextBirdIndex + index,
                species = species,
                pattern = FlightPattern.GLIDE,
                x = clamp(leader.x + fx(dx), fx(SIDE_MARGIN), fx(WORLD_WIDTH - SIDE_MARGIN)),
                y = clamp(leader.y + fx(dy), fx(CEILING), fx(VEGETATION_Y)),
                bossPhase = 0,
                leaderId = ESCORT,
                formationOffsetX = fx(dx),
                formationOffsetY = fx(dy),
                rngState = SplitMix64.split(leader.rngState, index + 1L),
                nextTurnTick = leader.ageTicks + 40 + index * 9,
            )
        }
        return state.copy(birds = state.birds + escorts, nextBirdIndex = state.nextBirdIndex + count)
    }

    /** Marks a boss escort: it follows its leader's slot but is moved freely by [Flight]. */
    private const val ESCORT = -2

    /**
     * What the legendary birds do that a single bird cannot decide alone: the crow stealing a page,
     * the grebe diving and surfacing elsewhere, the V of the Thunder Flock and the Wind Goose's flock.
     */
    private fun advanceBoss(before: GameState, state: GameState, events: MutableList<GameEvent>): GameState {
        if (state.spec.type != LevelType.BOSS || state.phase != RoundPhase.RUNNING) return state
        val extras = state.extras ?: return state
        var boss = extras.boss ?: return state
        val style = BossStyle.of(state.spec.species.first().id)
        var birds = state.birds
        val goal = extras.rules.goalPages

        if (style == BossStyle.THIEF_CROW && boss.pages in 1 until goal &&
            state.tick - boss.lastPageTick >= BossStyle.CROW_STEAL_TICKS
        ) {
            val pages = boss.pages - 1
            boss = boss.copy(pages = pages, phase = pages + 1, lastPageTick = state.tick)
            birds = birds.map { if (it.bossPhase > 0) it.copy(bossPhase = pages + 1) else it }
            events += GameEvent.BossStole(pages)
        }

        if (style.forPhase(boss.phase) != BossStyle.ROYAL_GREBE) {
            // Only the grebe surfaces somewhere else; the others just come back into view.
            birds = birds.map { if (it.diveUntilTick in 1..state.tick) it.copy(diveUntilTick = 0) else it }
        }
        if (style.forPhase(boss.phase) == BossStyle.ROYAL_GREBE) {
            birds = birds.map { bird ->
                if (bird.bossPhase == 0 || bird.status != BirdStatus.FLYING) return@map bird
                val previous = before.birds.firstOrNull { it.id == bird.id } ?: bird
                when {
                    bird.diveUntilTick > state.tick ->
                        // Under water: it stays where it went down.
                        bird.copy(x = previous.x, y = previous.y, vx = 0, vy = 0)
                    bird.diveUntilTick > 0 -> {
                        // Surfaces farther away with every page.
                        val reach = fx(50 + 25 * bird.bossPhase)
                        val span = fx(WORLD_WIDTH - 2 * SIDE_MARGIN)
                        val shifted = (previous.x - fx(SIDE_MARGIN) + reach) % span + fx(SIDE_MARGIN)
                        bird.copy(x = shifted, y = fx(VEGETATION_Y - 26), diveUntilTick = 0, heading = 3072 - 300)
                    }
                    bird.ageTicks % (BossStyle.GREBE_FLY_TICKS + BossStyle.grebeDiveTicks(bird.bossPhase)) ==
                        BossStyle.GREBE_FLY_TICKS -> {
                        events += GameEvent.BirdHidden(bird.id)
                        bird.copy(
                            y = fx(VEGETATION_Y),
                            diveUntilTick = state.tick + BossStyle.grebeDiveTicks(bird.bossPhase),
                        )
                    }
                    else -> bird
                }
            }
        }

        if (style == BossStyle.WIND_GOOSE && boss.phase >= 3 && !boss.escorts) {
            val leader = birds.firstOrNull { it.bossPhase > 0 && it.status == BirdStatus.FLYING }
            if (leader != null) {
                val withEscorts = addEscorts(state.copy(birds = birds), leader, SpeciesCatalog.byId("greylag"), WIND_ESCORTS)
                birds = withEscorts.birds
                boss = boss.copy(escorts = true)
                return withEscorts.copy(extras = extras.copy(boss = boss)).let { followLeader(it) }
            }
        }

        if (style == BossStyle.THUNDER_FLOCK && state.tick >= boss.nextStormTick) {
            boss = boss.copy(
                stormUntil = state.tick + BossStyle.STORM_TICKS,
                nextStormTick = state.tick + BossStyle.stormGap(boss.phase),
            )
            events += GameEvent.Lightning(boss.stormUntil)
        }
        val next = state.copy(birds = birds, extras = extras.copy(boss = boss))
        return if (next.tick < boss.stormUntil) next else followLeader(next)
    }

    /** Escorts drift back to their slot in the V instead of jumping into it. */
    private fun followLeader(state: GameState): GameState {
        val leader = state.birds.firstOrNull { it.bossPhase > 0 && it.status == BirdStatus.FLYING } ?: return state
        return state.copy(
            birds = state.birds.map { bird ->
                if (bird.leaderId != ESCORT || bird.status != BirdStatus.FLYING) return@map bird
                val targetX = clamp(leader.x + bird.formationOffsetX, fx(SIDE_MARGIN), fx(WORLD_WIDTH - SIDE_MARGIN))
                val targetY = clamp(leader.y + bird.formationOffsetY, fx(CEILING), fx(VEGETATION_Y))
                bird.copy(
                    x = bird.x + (targetX - bird.x) / 6,
                    y = bird.y + (targetY - bird.y) / 6,
                    vx = leader.vx,
                    vy = leader.vy,
                    // Escorts never time out while their leader is still in the sky.
                    ageTicks = minOf(bird.ageTicks, leader.ageTicks),
                )
            },
        )
    }

    private fun spawnProtected(state: GameState, events: MutableList<GameEvent>): GameState {
        val extras = state.extras ?: return state
        if (state.spec.type != LevelType.PROTECT && Modifier.PROTECTED !in state.spec.modifiers) return state
        if (extras.protectedSpawned >= extras.rules.protectedBirds) return state
        if (state.tick < extras.nextProtectedSpawnTick) return state
        val protectedPool = state.spec.species
            .map { SpeciesCatalog.byId(it.id) }
            .filter { it.protected }
        val speciesRng = SplitMix64(state.spawnRngState)
        val protectedSpecies = if (protectedPool.isEmpty()) {
            SpeciesCatalog.byId("heron")
        } else {
            protectedPool[speciesRng.nextInt(protectedPool.size)]
        }
        val rng = SplitMix64(speciesRng.state)
        val fromLeft = rng.nextInt(2) == 0
        val spawn = Flight.spawn(
            id = 10_000 + extras.protectedSpawned,
            species = protectedSpecies,
            pattern = FlightPattern.GLIDE,
            spec = state.spec,
            birdIndex = 0,
            spawnRngState = rng.state,
        )
        val y = 45 + rng.nextInt(60)
        val speed = fpsToFxPerTick((protectedSpecies.speedClass.speedUps * 70) / 100)
        val bird = spawn.bird.copy(
            x = if (fromLeft) -fx(protectedSpecies.radius) else fx(WORLD_WIDTH + protectedSpecies.radius),
            y = fx(y),
            vx = if (fromLeft) speed else -speed,
            vy = 0,
            heading = if (fromLeft) 0 else 2048,
            speed = speed,
            baseSpeed = speed,
            crossing = true,
        )
        events += GameEvent.BirdSpawned(bird)
        return state.copy(
            birds = state.birds + bird,
            spawnRngState = spawn.spawnRngState,
            extras = extras.copy(
                protectedSpawned = extras.protectedSpawned + 1,
                nextProtectedSpawnTick = state.tick + PROTECTED_GAP_TICKS,
            ),
        )
    }

    private fun spawnClayPair(state: GameState): GameState {
        val extras = state.extras ?: return state
        val rng = SplitMix64(state.spawnRngState)
        val pair = extras.clayPairsSpawned
        val flight = 150 + rng.nextInt(61)
        val scale = 1000
        val left = ClayPigeon(
            id = pair * 2, pairId = pair, x = fx(16), y = fx(220),
            vx = fpsToFxPerTick(60), vy = -fpsToFxPerTick(CLAY_LAUNCH_UPS),
            scalePermille = scale, ageTicks = 0, flightTicks = flight,
            status = ClayStatus.FLYING, fromLeft = true,
        )
        val right = left.copy(
            id = pair * 2 + 1, x = fx(WORLD_WIDTH - 16), vx = -fpsToFxPerTick(60), fromLeft = false,
        )
        return state.copy(
            spawnRngState = rng.state,
            extras = extras.copy(
                clays = listOf(left, right),
                clayPairsSpawned = pair + 1,
                corksLeft = extras.rules.corksPerPair,
                nextClaySpawnTick = 0,
            ),
        )
    }

    private fun advanceClays(state: GameState, events: MutableList<GameEvent>): GameState {
        val extras = state.extras ?: return state
        var missed = 0
        val clays = extras.clays.map { clay ->
            if (clay.status != ClayStatus.FLYING) return@map clay
            val age = clay.ageTicks + 1
            val vy = clay.vy + CLAY_GRAVITY
            val x = clay.x + clay.vx
            val y = clay.y + vy
            val scale = 1000 - 650 * age / clay.flightTicks
            val status = if (age >= clay.flightTicks || y > fx(WORLD_HEIGHT)) {
                missed++
                ClayStatus.MISSED
            } else {
                ClayStatus.FLYING
            }
            clay.copy(x = x, y = y, vy = vy, ageTicks = age, scalePermille = scale.coerceAtLeast(350), status = status)
        }
        var next = state.copy(extras = extras.copy(clays = clays))
        if (missed > 0) next = next.copy(birdsFled = next.birdsFled + missed)
        if (clays.none { it.status == ClayStatus.FLYING }) {
            val pairsSpawned = extras.clayPairsSpawned
            val nextSpawn = extras.nextClaySpawnTick
            if (pairsSpawned < extras.rules.clayPairs) {
                if (nextSpawn <= 0) {
                    next = next.copy(
                        extras = next.extras!!.copy(nextClaySpawnTick = state.tick + CLAY_PAIR_GAP_TICKS),
                    )
                } else if (state.tick >= nextSpawn) {
                    next = spawnClayPair(next)
                }
            }
        }
        return next
    }

    private fun claySnap(state: GameState, snap: InputAction.Snap, events: MutableList<GameEvent>): GameState {
        val extras = state.extras ?: return state
        if (extras.needsAdvance) return state
        val flying = extras.clays.filter { it.status == ClayStatus.FLYING }
        if (flying.isEmpty()) return state
        val frameScale = state.spec.frameScalePercent
        val aimFx = fx(snap.x)
        val aimFy = fx(snap.y)
        var best: ClayPigeon? = null
        var bestDistance = Int.MAX_VALUE
        for (clay in flying) {
            val tolerance = fx(
                CLAY_TOLERANCE_UNITS * clay.scalePermille * frameScale / (1000 * 100),
            )
            val d = distance(clay.x, clay.y, aimFx, aimFy)
            if (d <= tolerance && d < bestDistance) {
                best = clay
                bestDistance = d
            }
        }
        var photosTaken = state.photosTaken + 1
        var corks = extras.corksLeft - 1
        var next = state
        if (best == null) {
            events += GameEvent.CorkMiss(flying.first().pairId)
            next = state.copy(photosTaken = photosTaken)
        } else {
            val points = 500 + 500 * (1000 - best.scalePermille) / 1000
            val clays = extras.clays.map {
                if (it.id == best.id) it.copy(status = ClayStatus.BROKEN) else it
            }
            events += GameEvent.ClayBroken(best.id, points)
            next = state.copy(
                score = state.score + points,
                photosTaken = photosTaken,
                birdsPhotographed = state.birdsPhotographed + 1,
                bestQuality = state.bestQuality,
                extras = extras.copy(clays = clays),
            )
        }
        var extrasAfter = next.extras!!.copy(corksLeft = corks)
        if (corks <= 0 || extrasAfter.clays.none { it.status == ClayStatus.FLYING }) {
            val missed = extrasAfter.clays.count { it.status == ClayStatus.FLYING }
            val clays = extrasAfter.clays.map {
                if (it.status == ClayStatus.FLYING) it.copy(status = ClayStatus.MISSED) else it
            }
            extrasAfter = extrasAfter.copy(clays = clays, nextClaySpawnTick = state.tick + CLAY_PAIR_GAP_TICKS)
            next = next.copy(birdsFled = next.birdsFled + missed)
        }
        if (manualAdvance(state)) {
            extrasAfter = extrasAfter.copy(needsAdvance = true)
            events += GameEvent.NeedsAdvance(-1)
        }
        return next.copy(
            photosTaken = photosTaken,
            extras = extrasAfter,
        )
    }

    private fun finishIfNeeded(state: GameState, events: MutableList<GameEvent>): GameState {
        if (state.phase != RoundPhase.RUNNING) return state
        val extras = state.extras
        if (extras == null) {
            if (state.nextBirdIndex < state.spec.birds) return state
            if (state.birds.isNotEmpty()) return state
            if (state.results.size < state.spec.birds) return state
            val summary = summary(state)
            events += GameEvent.RoundEnded(summary)
            return state.copy(phase = RoundPhase.FINISHED, summary = summary)
        }
        if (state.spec.type == LevelType.CLAY) {
            if (extras.clayPairsSpawned < extras.rules.clayPairs) return state
            if (extras.clays.any { it.status == ClayStatus.FLYING }) return state
        } else if (state.spec.type == LevelType.BOSS) {
            val boss = extras.boss ?: BossState()
            val timeUp = state.tick >= (extras.timedEndTick ?: state.spec.timeoutTicks)
            val photosUp = state.photosUsed >= state.spec.photosPerBird
            if (boss.pages < extras.rules.goalPages && !timeUp && !photosUp) return state
            if (boss.pages >= extras.rules.goalPages && state.birds.isNotEmpty()) return state
        } else if (state.spec.type == LevelType.TIMED) {
            if (state.tick < (extras.timedEndTick ?: Int.MAX_VALUE)) return state
        } else {
            if (state.nextBirdIndex < state.spec.birds) return state
            if (state.birds.any { !it.species.protected }) return state
            val targets = state.results.count { it.photographed } + state.results.count { !it.photographed }
            if (targets < state.spec.birds) return state
        }
        val summary = summary(state)
        events += GameEvent.RoundEnded(summary)
        return state.copy(phase = RoundPhase.FINISHED, summary = summary)
    }

    private fun summary(state: GameState): RoundResult {
        val extras = state.extras
        val photographed: Int
        val fled: Int
        var passed: Boolean
        var stars: Int
        var birds: Int
        val protectedPhotos = extras?.protectedPhotoCount ?: 0
        val bossPages = extras?.boss?.pages ?: 0
        val plates = if (state.spec.type == LevelType.CLAY) state.birdsPhotographed else 0
        when (state.spec.type) {
            LevelType.CLAY -> {
                photographed = state.birdsPhotographed
                fled = state.birdsFled
                birds = (extras?.rules?.clayPairs ?: state.spec.birds / 2) * 2
                passed = photographed >= state.spec.need
            }
            LevelType.BOSS -> {
                photographed = bossPages
                fled = if (bossPages < 3) 1 else 0
                birds = 3
                passed = bossPages >= (extras?.rules?.goalPages ?: 3)
            }
            LevelType.TIMED -> {
                photographed = state.results.count { it.photographed }
                fled = state.results.count { !it.photographed }
                birds = maxOf(photographed + fled, 1)
                passed = photographed >= state.spec.need
            }
            else -> {
                photographed = state.results.count { it.photographed }
                fled = state.results.count { !it.photographed }
                birds = state.spec.birds
                passed = photographed >= state.spec.need &&
                    (
                        (state.spec.type != LevelType.PROTECT && Modifier.PROTECTED !in state.spec.modifiers) ||
                            protectedPhotos <= 1
                        )
            }
        }
        stars = if (!passed) {
            0
        } else {
            var s = 1 + (if (state.score >= state.spec.stars.two) 1 else 0) +
                (if (state.score >= state.spec.stars.three) 1 else 0)
            if (state.spec.type == LevelType.BOSS) {
                val photos = extras?.boss?.photos ?: state.photosUsed
                s = 1
                if (photos <= 9) s = 2
                // A very good photo (80+), not a perfect one: a finger rarely reaches 90 on a boss.
                if (photos <= 6 && state.bestQuality >= BOSS_THREE_STAR_QUALITY) s = 3
            }
            s
        }
        return RoundResult(
            passed = passed,
            stars = stars,
            photographed = photographed,
            fled = fled,
            score = state.score,
            need = state.spec.need,
            birds = birds,
            photosTaken = state.photosTaken,
            bestQuality = state.bestQuality,
            okPhotos = state.okPhotos,
            goodPhotos = state.goodPhotos,
            perfectPhotos = state.perfectPhotos,
            groupPhotos = state.groupPhotos,
            durationTicks = state.tick,
            protectedPhotos = protectedPhotos,
            bossPages = bossPages,
            platesBroken = plates,
        )
    }

    private fun fledResult(bird: BirdState, tick: Int): BirdResult = BirdResult(
        birdId = bird.id,
        speciesId = bird.species.id,
        photographed = false,
        quality = 0,
        points = 0,
        tick = tick,
    )

    private fun refreshVisibility(state: GameState): GameState {
        if (state.extras == null) return state
        return state.copy(
            birds = state.birds.map { if (it.status == BirdStatus.GONE) it else it.copy(hidden = computeHidden(it, state)) },
        )
    }

    private fun computeHidden(bird: BirdState, state: GameState): Boolean {
        val extras = state.extras ?: return false
        if (bird.diveUntilTick > state.tick) return true
        if (bird.bossPhase > 0 && bossHidden(bird)) return true
        val x = toUnits(bird.x)
        val y = toUnits(bird.y)
        if (Modifier.FOG in state.spec.modifiers && extras.fogBanks.any { it.covers(y) }) return true
        if (Modifier.COVER in state.spec.modifiers && extras.coverBoxes.any { it.covers(x, y) }) return true
        if (Modifier.NIGHT in state.spec.modifiers) {
            val light = fx(extras.nightX)
            val lightY = fx(extras.nightY)
            if (distance(bird.x, bird.y, light, lightY) > fx(NIGHT_RADIUS)) return true
        }
        return false
    }

    /** Mist, snow and the ghost hide the legendary bird for part of every cycle. */
    private fun bossHidden(bird: BirdState): Boolean {
        val phase = bird.bossPhase.coerceIn(1, 3)
        return when (BossStyle.of(bird.species.id).forPhase(phase)) {
            BossStyle.MIST_GOOSE -> bird.ageTicks % BossStyle.MIST_CYCLE >= BossStyle.mistVisibleTicks(phase)
            BossStyle.SNOW_GOOSE ->
                bird.ageTicks % BossStyle.SNOW_CYCLE >= BossStyle.SNOW_CYCLE - BossStyle.SNOW_HIDDEN
            BossStyle.GHOST ->
                bird.nextTurnTick - bird.ageTicks > BossStyle.GHOST_TELL_TICKS && bird.ageTicks >= bird.slowUntilTick
            else -> false
        }
    }

    private fun chooseSpecies(weights: List<SpeciesWeight>, rng: SplitMix64): Species {
        val targets = weights.filterNot { SpeciesCatalog.byId(it.id).protected }
        val pool = targets.ifEmpty { weights }
        val total = pool.sumOf { it.weight }
        var pick = rng.nextInt(total)
        for (weight in pool) {
            if (pick < weight.weight) return SpeciesCatalog.byId(weight.id)
            pick -= weight.weight
        }
        return SpeciesCatalog.byId(pool.last().id)
    }
}

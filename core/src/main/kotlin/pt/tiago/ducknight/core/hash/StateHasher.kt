package pt.tiago.ducknight.core.hash

import pt.tiago.ducknight.core.SIM_VERSION
import pt.tiago.ducknight.core.engine.BirdResult
import pt.tiago.ducknight.core.engine.GameState
import pt.tiago.ducknight.core.engine.RoundResult
import pt.tiago.ducknight.core.flight.BirdState
import pt.tiago.ducknight.core.level.GameOptions
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.random.SplitMix64

/**
 * Stable 64-bit hash of a [GameState] over every field, in a fixed order.
 * Saved levels and certificates depend on it; any change to the field list must bump SIM_VERSION.
 */
object StateHasher {
    fun hash(state: GameState): Long {
        val h = Accumulator()
        h.int(SIM_VERSION)
        spec(h, state.spec)
        options(h, state.options)
        h.int(state.tick)
        h.int(state.phase.ordinal)
        h.int(state.birds.size)
        state.birds.forEach { bird(h, it) }
        h.int(state.results.size)
        state.results.forEach { result(h, it) }
        h.int(state.nextBirdIndex)
        h.int(state.photosUsed)
        h.int(state.score)
        h.int(state.photosTaken)
        h.int(state.birdsPhotographed)
        h.int(state.birdsFled)
        h.int(state.bestQuality)
        h.int(state.okPhotos)
        h.int(state.goodPhotos)
        h.int(state.perfectPhotos)
        h.int(state.groupPhotos)
        h.long(state.spawnRngState)
        h.long(state.weatherRngState)
        h.int(state.nextSpawnTick)
        state.summary?.let {
            h.boolean(true)
            summary(h, it)
        } ?: h.boolean(false)
        state.extras?.let { extras(h, it) }
        return h.value
    }

    private fun extras(h: Accumulator, extras: pt.tiago.ducknight.core.engine.RoundExtras) {
        rules(h, extras.rules)
        extras.filmLeft?.let { h.int(1); h.int(it) } ?: h.int(0)
        extras.timedEndTick?.let { h.int(1); h.int(it) } ?: h.int(0)
        h.int(extras.cooldownUntil)
        h.int(extras.protectedPhotoCount)
        h.boolean(extras.needsAdvance)
        h.int(extras.combo)
        h.int(extras.multiplierPercent)
        h.int(extras.windFx)
        h.int(extras.windNextTick)
        h.long(extras.windRngState)
        h.int(extras.fogBanks.size)
        extras.fogBanks.forEach {
            h.int(it.y)
            h.int(it.height)
            h.int(it.speed)
            h.int(it.phase)
        }
        h.int(extras.coverBoxes.size)
        extras.coverBoxes.forEach {
            h.int(it.x)
            h.int(it.y)
            h.int(it.width)
            h.int(it.height)
        }
        h.int(extras.nightX)
        h.int(extras.nightY)
        h.int(extras.nightFollowId)
        h.int(extras.nightFollowUntil)
        extras.boss?.let {
            h.int(1)
            h.int(it.phase)
            h.int(it.pages)
            h.int(it.photos)
            h.int(it.lastPageTick)
            h.int(it.stormUntil)
            h.int(it.nextStormTick)
            h.boolean(it.escorts)
        } ?: h.int(0)
        h.int(extras.clays.size)
        extras.clays.forEach {
            h.int(it.id)
            h.int(it.pairId)
            h.int(it.x)
            h.int(it.y)
            h.int(it.vx)
            h.int(it.vy)
            h.int(it.scalePermille)
            h.int(it.ageTicks)
            h.int(it.flightTicks)
            h.int(it.status.ordinal)
            h.boolean(it.fromLeft)
        }
        h.int(extras.clayPairsSpawned)
        h.int(extras.corksLeft)
        h.int(extras.nextClaySpawnTick)
        h.int(extras.protectedSpawned)
        h.int(extras.nextProtectedSpawnTick)
        h.boolean(extras.formationBroken)
    }

    private fun spec(h: Accumulator, spec: LevelSpec) {
        h.string(spec.id)
        h.int(spec.place)
        h.int(spec.ordinal)
        h.int(spec.type.ordinal)
        h.int(spec.difficulty.ordinal)
        h.long(spec.seed)
        h.int(spec.attemptIndex)
        h.int(spec.birds)
        h.int(spec.photosPerBird)
        h.int(spec.need)
        h.int(spec.timeoutTicks)
        h.int(spec.speedPercent)
        h.int(spec.rampPercent)
        h.int(spec.frameScalePercent)
        h.int(spec.species.size)
        spec.species.forEach {
            h.string(it.id)
            h.int(it.weight)
        }
        h.int(spec.patterns.size)
        spec.patterns.forEach { h.int(it.ordinal) }
        h.int(spec.stars.two)
        h.int(spec.stars.three)
        // Newer fields are hashed only when they are used, preserving every original golden.
        if (spec.modifiers.isNotEmpty()) {
            val ordered = spec.modifiers.sortedBy { it.ordinal }
            h.int(ordered.size)
            ordered.forEach { h.int(it.ordinal) }
        }
        spec.rules?.let {
            h.int(1)
            rules(h, it)
        }
    }

    private fun rules(h: Accumulator, rules: pt.tiago.ducknight.core.level.TypeRules) {
        h.int(rules.flockSize)
        h.int(rules.flocks)
        h.int(rules.filmFrames)
        h.int(rules.timedTicks)
        h.int(rules.photoCooldownTicks)
        h.int(rules.protectedBirds)
        h.int(rules.clayPairs)
        h.int(rules.corksPerPair)
        h.int(rules.bossPhases)
        h.int(rules.bossPhotoLimit)
        h.int(rules.bossTimeTicks)
        h.int(rules.goalPages)
        h.int(rules.fogBanks.size)
        rules.fogBanks.forEach {
            h.int(it.y)
            h.int(it.height)
            h.int(it.speed)
            h.int(it.phase)
        }
        h.int(rules.coverBoxes.size)
        rules.coverBoxes.forEach {
            h.int(it.x)
            h.int(it.y)
            h.int(it.width)
            h.int(it.height)
        }
        h.int(rules.windStrength)
    }

    private fun options(h: Accumulator, options: GameOptions) {
        h.int(options.tolerancePercent)
        h.boolean(options.biggerBirds)
        h.boolean(options.aimAssist)
    }

    private fun bird(h: Accumulator, bird: BirdState) {
        h.int(bird.id)
        h.string(bird.species.id)
        h.int(bird.pattern.ordinal)
        h.int(bird.x)
        h.int(bird.y)
        h.int(bird.vx)
        h.int(bird.vy)
        h.int(bird.heading)
        h.int(bird.speed)
        h.int(bird.baseSpeed)
        h.int(bird.ageTicks)
        h.int(bird.nextTurnTick)
        h.int(bird.slowUntilTick)
        h.boolean(bird.dartBurst)
        h.int(bird.wingPhase)
        h.int(bird.status.ordinal)
        h.int(bird.statusTicks)
        h.long(bird.rngState)
        h.int(bird.photosUsed)
        h.boolean(bird.resolved)
        h.int(bird.quality)
        h.int(bird.escapeAtTick)
        if (bird.hidden) h.boolean(true)
        if (bird.leaderId != -1) {
            h.int(bird.leaderId)
            h.int(bird.formationOffsetX)
            h.int(bird.formationOffsetY)
        }
        if (bird.bossPhase != 0) h.int(bird.bossPhase)
        if (bird.diveUntilTick != 0) h.int(bird.diveUntilTick)
        if (bird.crossing) h.boolean(true)
    }

    private fun result(h: Accumulator, result: BirdResult) {
        h.int(result.birdId)
        h.string(result.speciesId)
        h.boolean(result.photographed)
        h.int(result.quality)
        h.int(result.points)
        h.int(result.tick)
    }

    private fun summary(h: Accumulator, summary: RoundResult) {
        h.boolean(summary.passed)
        h.int(summary.stars)
        h.int(summary.photographed)
        h.int(summary.fled)
        h.int(summary.score)
        h.int(summary.need)
        h.int(summary.birds)
        h.int(summary.photosTaken)
        h.int(summary.bestQuality)
        h.int(summary.okPhotos)
        h.int(summary.goodPhotos)
        h.int(summary.perfectPhotos)
        h.int(summary.groupPhotos)
        h.int(summary.durationTicks)
        if (summary.protectedPhotos != 0) h.int(summary.protectedPhotos)
        if (summary.bossPages != 0) h.int(summary.bossPages)
        if (summary.platesBroken != 0) h.int(summary.platesBroken)
    }

    /** Order-sensitive fold of 64-bit words through the SplitMix64 finalizer. */
    private class Accumulator {
        var value: Long = 0x4455_434B_4E49_4748L
            private set

        fun long(v: Long) {
            value = SplitMix64.mix(value * 1_000_003L xor v)
        }

        fun int(v: Int) = long(v.toLong())

        fun boolean(v: Boolean) = int(if (v) 1 else 0)

        fun string(text: String) {
            int(text.length)
            text.forEach { int(it.code) }
        }
    }
}

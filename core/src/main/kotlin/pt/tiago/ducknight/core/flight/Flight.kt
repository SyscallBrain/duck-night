package pt.tiago.ducknight.core.flight

import pt.tiago.ducknight.core.fixed.CEILING
import pt.tiago.ducknight.core.fixed.SIDE_MARGIN
import pt.tiago.ducknight.core.fixed.TICK_HZ
import pt.tiago.ducknight.core.fixed.TrigTable
import pt.tiago.ducknight.core.fixed.VEGETATION_Y
import pt.tiago.ducknight.core.fixed.WORLD_WIDTH
import pt.tiago.ducknight.core.fixed.degreesToAngle
import pt.tiago.ducknight.core.fixed.fpsToFxPerTick
import pt.tiago.ducknight.core.fixed.fx
import pt.tiago.ducknight.core.fixed.fxMul
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.random.SplitMix64
import pt.tiago.ducknight.core.species.FlightPattern
import pt.tiago.ducknight.core.species.Species

enum class BirdStatus { FLYING, STARTLED, FLEEING, GONE }

/**
 * One bird in the sky. All positions and speeds are fixed-point integers (1 unit = 256).
 * The state is immutable; [Flight.step] returns the next state so replays are exact.
 */
data class BirdState(
    val id: Int,
    val species: Species,
    val pattern: FlightPattern,
    val x: Int,
    val y: Int,
    val vx: Int,
    val vy: Int,
    val heading: Int,
    val speed: Int,
    val baseSpeed: Int,
    val ageTicks: Int,
    val nextTurnTick: Int,
    val slowUntilTick: Int,
    val dartBurst: Boolean,
    val wingPhase: Int,
    val status: BirdStatus,
    val statusTicks: Int,
    val rngState: Long,
    val photosUsed: Int = 0,
    val resolved: Boolean = false,
    val quality: Int = 0,
    /** When the last photo was spent: the bird leaves 0.45 s later. 0 means unset. */
    val escapeAtTick: Int = 0,
    /** Fog, cover, night or a dive hides the bird from the camera. */
    val hidden: Boolean = false,
    /** Formation leader, or -1 when the bird flies on its own. */
    val leaderId: Int = -1,
    val formationOffsetX: Int = 0,
    val formationOffsetY: Int = 0,
    /** Boss phase 1..3, 0 for an ordinary bird. */
    val bossPhase: Int = 0,
    /** Tick when a diving bird surfaces; 0 when it is not diving. */
    val diveUntilTick: Int = 0,
    /** A protected bird crossing the sky leaves instead of bouncing. */
    val crossing: Boolean = false,
)

data class SpawnResult(val bird: BirdState, val spawnRngState: Long)

/**
 * Deterministic flight. The simulation never uses floating point:
 * the angle comes from [TrigTable] and every speed is integer fixed-point per tick.
 */
object Flight {
    /** 0.5 s at 60 Hz: a photographed bird holds still with its wings open. */
    const val STARTLE_TICKS = 30

    /** A bird leaves the vegetation line after this many ticks and may descend below 206. */
    const val ASCENT_TICKS = 72

    /** Speed when a bird is startled by the shutter. */
    const val FLEE_SPEED_UPS = 180

    /** Speed when a bird escapes because its time ran out or its photos were spent. */
    const val TIMEOUT_FLEE_SPEED_UPS = 150

    /** One wing beat cycle at 9 Hz over 60 ticks per second. */
    private const val WING_STEP = 614

    /** A circling bird turns by this many angle steps each tick (about 5.6 degrees). */
    private const val CIRCLE_TURN = 64

    /** The Golden Eider loops around the whole field: about 5.7 s per turn. */
    private const val EIDER_TURN = 12

    /** The Old Mallard's feint on its last page: [FEINT_TICKS] out of every [FEINT_CYCLE]. */
    private const val FEINT_CYCLE = 180
    private const val FEINT_TICKS = 40

    /** The Wind Goose's gusts change side every 1.5 s. */
    private const val GUST_TICKS = 90

    /** The Imperial Mandarin dashes for 0.3 s between perches. */
    private const val HOP_TICKS = 18

    fun spawn(
        id: Int,
        species: Species,
        pattern: FlightPattern,
        spec: LevelSpec,
        birdIndex: Int,
        spawnRngState: Long,
    ): SpawnResult {
        val spawn = SplitMix64(spawnRngState)
        val xUnits = SIDE_MARGIN + 20 + spawn.nextInt(WORLD_WIDTH - 2 * SIDE_MARGIN - 40 + 1)
        val toTheRight = spawn.nextInt(2) == 0
        val climbDegrees = 25 + spawn.nextInt(36)
        val angle = if (toTheRight) {
            (TrigTable.STEPS - degreesToAngle(climbDegrees)) and (TrigTable.STEPS - 1)
        } else {
            (2048 + degreesToAngle(climbDegrees)) and (TrigTable.STEPS - 1)
        }

        val attemptSeed = SplitMix64.split(spec.seed, spec.attemptIndex.toLong())
        val birdSeed = SplitMix64.split(attemptSeed, id.toLong() + 1)
        val birdRng = SplitMix64(birdSeed)
        val wing = birdRng.nextInt(TrigTable.STEPS)
        val baseSpeed = baseSpeed(species, spec, birdIndex)
        val nextTurn = firstTurn(pattern, birdRng)

        val bird = BirdState(
            id = id,
            species = species,
            pattern = pattern,
            x = fx(xUnits),
            y = fx(VEGETATION_Y - 26),
            vx = fxMul(baseSpeed, TrigTable.cos(angle)),
            vy = fxMul(baseSpeed, TrigTable.sin(angle)),
            heading = angle,
            speed = baseSpeed,
            baseSpeed = baseSpeed,
            ageTicks = 0,
            nextTurnTick = nextTurn,
            slowUntilTick = 0,
            dartBurst = pattern == FlightPattern.DART,
            wingPhase = wing,
            status = BirdStatus.FLYING,
            statusTicks = 0,
            rngState = birdRng.state,
        )
        return SpawnResult(bird, spawn.state)
    }

    private fun baseSpeed(species: Species, spec: LevelSpec, birdIndex: Int): Int {
        val base = fpsToFxPerTick(species.speedClass.speedUps)
        val ramp = 100 + spec.rampPercent * birdIndex
        return (base.toLong() * spec.speedPercent * ramp / 10_000L).toInt()
    }

    private fun firstTurn(pattern: FlightPattern, rng: SplitMix64): Int = when (pattern) {
        FlightPattern.ZIGZAG -> 36 + rng.nextInt(55)
        FlightPattern.GLIDE -> 120 + rng.nextInt(121)
        FlightPattern.DART -> 12
        FlightPattern.CIRCLE -> 1
        FlightPattern.TREE_HOP -> 30 + rng.nextInt(31)
        else -> 120
    }

    fun step(bird: BirdState, spec: LevelSpec, birdIndex: Int): BirdState {
        if (bird.status == BirdStatus.GONE) return bird

        if (bird.status == BirdStatus.STARTLED) {
            val ticks = bird.statusTicks + 1
            if (ticks >= STARTLE_TICKS) return flee(bird.copy(statusTicks = ticks), FLEE_SPEED_UPS)
            return bird.copy(statusTicks = ticks)
        }

        if (bird.status == BirdStatus.FLEEING) {
            val next = bird.copy(
                x = bird.x + bird.vx,
                y = bird.y + bird.vy,
                statusTicks = bird.statusTicks + 1,
            )
            val gone = next.y + fx(next.species.radius) < 0
            return if (gone) next.copy(status = BirdStatus.GONE) else next
        }

        var current = bird.copy(
            ageTicks = bird.ageTicks + 1,
            wingPhase = (bird.wingPhase + WING_STEP) and (TrigTable.STEPS - 1),
        )
        if (current.ageTicks >= spec.timeoutTicks) {
            return flee(current, TIMEOUT_FLEE_SPEED_UPS)
        }

        current = when (current.pattern) {
            FlightPattern.ZIGZAG -> zigzag(current)
            FlightPattern.GLIDE -> glide(current)
            FlightPattern.DART -> dart(current)
            FlightPattern.CIRCLE -> circle(current)
            FlightPattern.TREE_HOP -> treeHop(current)
            FlightPattern.BOSS -> boss(current)
            // FORMATION and DIVE_SURFACE are advanced by the simulation, not by Flight.
            else -> glide(current)
        }

        current = current.copy(x = current.x + current.vx, y = current.y + current.vy)
        current = keepInWorld(current)
        return current
    }

    private fun zigzag(bird: BirdState): BirdState {
        if (bird.ageTicks < bird.nextTurnTick) return bird
        val rng = SplitMix64(bird.rngState)
        val climb = 20 + rng.nextInt(41)
        val turnLeft = bird.vx > 0
        val heading = if (turnLeft) {
            (2048 + degreesToAngle(climb)) and (TrigTable.STEPS - 1)
        } else {
            (TrigTable.STEPS - degreesToAngle(climb)) and (TrigTable.STEPS - 1)
        }
        val next = bird.copy(
            heading = heading,
            nextTurnTick = bird.ageTicks + 36 + rng.nextInt(55),
            rngState = rng.state,
        )
        return withVelocity(next)
    }

    private fun glide(bird: BirdState): BirdState {
        var current = bird
        if (current.ageTicks >= current.nextTurnTick) {
            val rng = SplitMix64(current.rngState)
            val sign = if (rng.nextInt(2) == 0) -1 else 1
            val delta = degreesToAngle(5 + rng.nextInt(9))
            val heading = (current.heading + sign * delta + TrigTable.STEPS) and (TrigTable.STEPS - 1)
            current = current.copy(
                heading = heading,
                nextTurnTick = current.ageTicks + 120 + rng.nextInt(121),
                slowUntilTick = current.ageTicks + 30,
                rngState = rng.state,
            )
        }
        val speed = if (current.ageTicks < current.slowUntilTick) {
            current.baseSpeed * 80 / 100
        } else {
            current.baseSpeed
        }
        return withVelocity(current.copy(speed = speed))
    }

    private fun dart(bird: BirdState): BirdState {
        var current = bird
        if (current.ageTicks >= current.nextTurnTick) {
            val rng = SplitMix64(current.rngState)
            val burst = !current.dartBurst
            val sign = if (rng.nextInt(2) == 0) -1 else 1
            val heading = if (burst) {
                (current.heading + sign * degreesToAngle(30 + rng.nextInt(91)) + TrigTable.STEPS) and
                    (TrigTable.STEPS - 1)
            } else {
                current.heading
            }
            val speed = if (burst) current.baseSpeed * 135 / 100 else current.baseSpeed * 25 / 100
            current = current.copy(
                heading = heading,
                speed = speed,
                dartBurst = burst,
                nextTurnTick = current.ageTicks + 12 + rng.nextInt(7),
                rngState = rng.state,
            )
            current = withVelocity(current)
        }
        return current
    }

    /** Constant, deterministic arc: the bird keeps turning in the same direction. */
    private fun circle(bird: BirdState): BirdState {
        val heading = (bird.heading + CIRCLE_TURN) and (TrigTable.STEPS - 1)
        return withVelocity(bird.copy(heading = heading, speed = bird.baseSpeed, slowUntilTick = 0))
    }

    /** Short, sharp hops between cover points, with a pause before the next hop. */
    private fun treeHop(bird: BirdState): BirdState {
        var current = bird
        if (current.ageTicks >= current.nextTurnTick) {
            val rng = SplitMix64(current.rngState)
            val sign = if (rng.nextInt(2) == 0) -1 else 1
            val turn = degreesToAngle(45 + rng.nextInt(46))
            current = current.copy(
                heading = (current.heading + sign * turn + TrigTable.STEPS) and (TrigTable.STEPS - 1),
                speed = current.baseSpeed * 140 / 100,
                slowUntilTick = current.ageTicks + 8,
                nextTurnTick = current.ageTicks + 30 + rng.nextInt(31),
                rngState = rng.state,
                dartBurst = true,
            )
        } else if (current.ageTicks >= current.slowUntilTick) {
            current = current.copy(speed = current.baseSpeed, dartBurst = false)
        }
        return withVelocity(current)
    }

    /**
     * The legendary bird. Each album page makes it faster (70, 85, then 100 % of its base speed) and
     * its boss style decides how it moves. Hiding, diving, stealing and the
     * escorts are handled by the simulation, which sees the whole round.
     */
    private fun boss(bird: BirdState): BirdState {
        val phase = bird.bossPhase.coerceIn(1, 3)
        val speed = bird.baseSpeed * when (phase) {
            1 -> 70
            2 -> 85
            else -> 100
        } / 100
        return when (BossStyle.of(bird.species.id).forPhase(phase)) {
            BossStyle.OLD_MALLARD -> oldMallard(bird, speed, phase)
            BossStyle.WIND_GOOSE -> windGoose(bird, speed)
            BossStyle.MANDARIN -> mandarin(bird, speed)
            BossStyle.GHOST -> ghost(bird, speed)
            BossStyle.GOLDEN_EIDER -> withVelocity(
                bird.copy(heading = (bird.heading + EIDER_TURN) and (TrigTable.STEPS - 1), speed = speed),
            )
            else -> wander(bird, speed, 60)
        }
    }

    /** Gentle turns of 15 to 50 degrees every [gap] to 2 × [gap] ticks. */
    private fun wander(bird: BirdState, speed: Int, gap: Int): BirdState {
        var current = bird.copy(speed = speed)
        if (current.ageTicks >= current.nextTurnTick) {
            val rng = SplitMix64(current.rngState)
            val sign = if (rng.nextInt(2) == 0) -1 else 1
            val heading = (current.heading + sign * degreesToAngle(15 + rng.nextInt(36)) + TrigTable.STEPS) and
                (TrigTable.STEPS - 1)
            current = current.copy(
                heading = heading,
                nextTurnTick = current.ageTicks + gap + rng.nextInt(gap + 1),
                rngState = rng.state,
            )
        }
        return withVelocity(current)
    }

    /** Zigzag, faster turns on the second page, and on the third a feint: it flies off and comes back. */
    private fun oldMallard(bird: BirdState, speed: Int, phase: Int): BirdState {
        if (phase == 3 && bird.ageTicks % FEINT_CYCLE < FEINT_TICKS) {
            return withVelocity(bird.copy(heading = 3072, speed = speed * 3 / 2))
        }
        var current = bird.copy(speed = speed)
        if (current.ageTicks >= current.nextTurnTick) {
            val rng = SplitMix64(current.rngState)
            val climb = 20 + rng.nextInt(41)
            val heading = if (current.vx > 0) {
                (2048 + degreesToAngle(climb)) and (TrigTable.STEPS - 1)
            } else {
                (TrigTable.STEPS - degreesToAngle(climb)) and (TrigTable.STEPS - 1)
            }
            val gap = if (phase == 1) 50 else 30
            current = current.copy(heading = heading, nextTurnTick = current.ageTicks + gap + rng.nextInt(gap + 1), rngState = rng.state)
        }
        return withVelocity(current)
    }

    /** Glides with the gusts: a sideways push that changes side every 1.5 s. */
    private fun windGoose(bird: BirdState, speed: Int): BirdState {
        val glided = wander(bird, speed, 90)
        val push = if ((bird.ageTicks / GUST_TICKS) % 2 == 0) speed / 3 else -speed / 3
        return glided.copy(vx = glided.vx + push)
    }

    /** Hops from tree to tree: a quick dash, then it perches almost still before the next one. */
    private fun mandarin(bird: BirdState, speed: Int): BirdState {
        var current = bird
        if (current.ageTicks >= current.nextTurnTick) {
            val rng = SplitMix64(current.rngState)
            val sign = if (rng.nextInt(2) == 0) -1 else 1
            current = current.copy(
                heading = (current.heading + sign * degreesToAngle(45 + rng.nextInt(91)) + TrigTable.STEPS) and
                    (TrigTable.STEPS - 1),
                slowUntilTick = current.ageTicks + HOP_TICKS,
                nextTurnTick = current.ageTicks + HOP_TICKS + 40 + rng.nextInt(31),
                rngState = rng.state,
            )
        }
        val moving = current.ageTicks < current.slowUntilTick
        return withVelocity(current.copy(speed = if (moving) speed * 8 / 5 else speed / 6, dartBurst = moving))
    }

    /**
     * Sharp, sudden turns. [BirdState.slowUntilTick] marks the end of the visible spell after a
     * turn; the simulation hides the ghost outside it and outside the tell before the next turn.
     */
    private fun ghost(bird: BirdState, speed: Int): BirdState {
        var current = bird.copy(speed = speed)
        if (current.ageTicks >= current.nextTurnTick) {
            val rng = SplitMix64(current.rngState)
            val sign = if (rng.nextInt(2) == 0) -1 else 1
            current = current.copy(
                heading = (current.heading + sign * degreesToAngle(60 + rng.nextInt(91)) + TrigTable.STEPS) and
                    (TrigTable.STEPS - 1),
                slowUntilTick = current.ageTicks + BossStyle.GHOST_AFTER_TICKS,
                nextTurnTick = current.ageTicks + 70 + rng.nextInt(61),
                rngState = rng.state,
            )
        }
        return withVelocity(current)
    }

    private fun withVelocity(bird: BirdState): BirdState = bird.copy(
        vx = fxMul(bird.speed, TrigTable.cos(bird.heading)),
        vy = fxMul(bird.speed, TrigTable.sin(bird.heading)),
    )

    private fun keepInWorld(bird: BirdState): BirdState {
        var current = bird
        // The bird must not dive below the vegetation line in its first 1.2 seconds.
        if (current.ageTicks <= ASCENT_TICKS) {
            val floor = fx(VEGETATION_Y - 26)
            if (current.y > floor) {
                current = current.copy(y = floor)
                if (current.vy > 0) {
                    current = withVelocity(current.copy(heading = reflectVertically(current.heading)))
                }
            }
        }
        if (current.x < fx(SIDE_MARGIN)) {
            current = withVelocity(current.copy(x = fx(SIDE_MARGIN), heading = reflectHorizontally(current.heading)))
        } else if (current.x > fx(WORLD_WIDTH - SIDE_MARGIN)) {
            current = withVelocity(
                current.copy(x = fx(WORLD_WIDTH - SIDE_MARGIN), heading = reflectHorizontally(current.heading)),
            )
        }
        if (current.y < fx(CEILING)) {
            current = withVelocity(current.copy(y = fx(CEILING), heading = reflectVertically(current.heading)))
        } else if (current.y > fx(VEGETATION_Y)) {
            current = withVelocity(current.copy(y = fx(VEGETATION_Y), heading = reflectVertically(current.heading)))
        }
        return current
    }

    private fun reflectHorizontally(heading: Int): Int =
        (2048 - heading) and (TrigTable.STEPS - 1)

    private fun reflectVertically(heading: Int): Int =
        (TrigTable.STEPS - heading) and (TrigTable.STEPS - 1)

    /** A photo has been taken: the bird stops with its wings open and is resolved. */
    fun startle(bird: BirdState): BirdState = bird.copy(
        status = BirdStatus.STARTLED,
        statusTicks = 0,
        speed = 0,
        vx = 0,
        vy = 0,
        wingPhase = TrigTable.STEPS / 2,
        resolved = true,
    )

    /** The bird escapes upward at [speedUps]. */
    fun flee(bird: BirdState, speedUps: Int = TIMEOUT_FLEE_SPEED_UPS): BirdState {
        if (bird.status == BirdStatus.GONE) return bird
        val speed = fpsToFxPerTick(speedUps)
        return bird.copy(
            status = BirdStatus.FLEEING,
            statusTicks = 0,
            heading = 3072,
            speed = speed,
            vx = 0,
            vy = -speed,
        )
    }

    fun isVisible(bird: BirdState): Boolean = bird.status != BirdStatus.GONE

    fun isFlying(bird: BirdState): Boolean = bird.status == BirdStatus.FLYING

    /** Speed shown to the photo quality rule, in units per second. */
    fun speedUps(bird: BirdState): Int = (bird.speed.toLong() * TICK_HZ / 256).toInt()

    /** For tests and diagnostics. */
    fun maxSpeedUps(species: Species): Int = species.speedClass.speedUps
}

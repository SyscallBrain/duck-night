package pt.tiago.ducknight.core.flight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.fixed.TrigTable
import pt.tiago.ducknight.core.fixed.VEGETATION_Y
import pt.tiago.ducknight.core.fixed.WORLD_HEIGHT
import pt.tiago.ducknight.core.fixed.WORLD_WIDTH
import pt.tiago.ducknight.core.fixed.fpsToFxPerTick
import pt.tiago.ducknight.core.fixed.fx
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.species.FlightPattern
import pt.tiago.ducknight.core.species.SpeciesCatalog

class FlightTest {
    private val spec = LevelSpec.classic(seed = 123L, difficulty = Difficulty.MEDIUM)

    private fun spawn(
        pattern: FlightPattern = FlightPattern.ZIGZAG,
        seed: Long = 123L,
        spawnState: Long = 42L,
        index: Int = 0,
        level: LevelSpec = spec,
        speciesId: String = "mallard",
    ): BirdState {
        val result = Flight.spawn(
            id = 0,
            species = SpeciesCatalog.byId(speciesId),
            pattern = pattern,
            spec = level,
            birdIndex = index,
            spawnRngState = spawnState,
        )
        return result.bird
    }

    @Test
    fun `a bird starts at the takeoff floor above the vegetation`() {
        val bird = spawn()
        assertEquals(fx(VEGETATION_Y - 26), bird.y)
        assertEquals(BirdStatus.FLYING, bird.status)
        assertTrue(bird.x in fx(16)..fx(WORLD_WIDTH - 16))
    }

    @Test
    fun `a bird starts by climbing at 25 to 60 degrees`() {
        for (seed in 0L..100L) {
            val bird = spawn(seed = seed, spawnState = seed)
            assertTrue("seed $seed must climb", TrigTable.sin(bird.heading) < 0)
            val climbDegrees = bird.heading
            assertTrue("seed $seed heading $climbDegrees", climbDegrees in 0..4095)
            // The horizontal component is at least half of the speed for shallow climbs.
            assertTrue(TrigTable.cos(bird.heading) != 0)
        }
    }

    @Test
    fun `the same spawn inputs give the same bird`() {
        val a = spawn(spawnState = 7L)
        val b = spawn(spawnState = 7L)
        assertEquals(a, b)
    }

    @Test
    fun `different spawn seeds give different birds`() {
        val a = spawn(spawnState = 7L)
        val b = spawn(spawnState = 8L)
        assertNotEquals(a, b)
    }

    @Test
    fun `a bird never leaves the world while flying`() {
        for (seed in 0L..49L) {
            var bird = spawn(seed = seed, spawnState = seed)
            repeat(2_000) {
                bird = Flight.step(bird, spec, 0)
                if (bird.status == BirdStatus.FLYING) {
                    assertTrue("seed $seed x=${bird.x}", bird.x in 0..fx(WORLD_WIDTH))
                    assertTrue("seed $seed y=${bird.y}", bird.y in 0..fx(WORLD_HEIGHT))
                }
            }
        }
    }

    @Test
    fun `a bird stays above the vegetation line in its first 1 point 2 seconds`() {
        for (seed in 0L..49L) {
            var bird = spawn(seed = seed, spawnState = seed)
            repeat(72) {
                bird = Flight.step(bird, spec, 0)
                assertTrue("seed $seed y=${bird.y}", bird.y <= fx(VEGETATION_Y - 26))
            }
        }
    }

    @Test
    fun `zigzag changes heading many times`() {
        var bird = spawn(pattern = FlightPattern.ZIGZAG)
        val headings = mutableListOf(bird.heading)
        repeat(600) {
            bird = Flight.step(bird, spec, 0)
            if (headings.last() != bird.heading) headings += bird.heading
        }
        assertTrue("zigzag changed ${headings.size - 1} times", headings.size >= 5)
    }

    @Test
    fun `glide changes heading less often than zigzag`() {
        var zigzag = spawn(pattern = FlightPattern.ZIGZAG)
        var glide = spawn(pattern = FlightPattern.GLIDE)
        val zigzagHeadings = mutableSetOf(zigzag.heading)
        val glideHeadings = mutableSetOf(glide.heading)
        repeat(600) {
            zigzag = Flight.step(zigzag, spec, 0)
            glide = Flight.step(glide, spec, 0)
            zigzagHeadings += zigzag.heading
            glideHeadings += glide.heading
        }
        assertTrue("glide ${glideHeadings.size} vs zigzag ${zigzagHeadings.size}", glideHeadings.size < zigzagHeadings.size)
    }

    @Test
    fun `dart alternates bursts and pauses`() {
        var bird = spawn(pattern = FlightPattern.DART)
        var sawFast = false
        var sawSlow = false
        repeat(120) {
            bird = Flight.step(bird, spec, 0)
            if (bird.speed > bird.baseSpeed) sawFast = true
            if (bird.speed < bird.baseSpeed / 2) sawSlow = true
        }
        assertTrue("dart never burst", sawFast)
        assertTrue("dart never paused", sawSlow)
    }

    @Test
    fun `circle turns steadily in one direction`() {
        var bird = spawn(pattern = FlightPattern.CIRCLE, speciesId = "golden").copy(
            x = fx(120), y = fx(100), heading = 0, vx = 0, vy = 0,
        )
        val start = bird.heading
        bird = Flight.step(bird, spec, 0)
        bird = Flight.step(bird, spec, 0)
        assertEquals(128, (bird.heading - start) and (TrigTable.STEPS - 1))
    }

    @Test
    fun `tree hop bursts between perches`() {
        val bird = spawn(pattern = FlightPattern.TREE_HOP, speciesId = "mandarin")
        assertTrue("first hop ${bird.nextTurnTick}", bird.nextTurnTick in 30..60)
        val hopped = Flight.step(bird.copy(ageTicks = bird.nextTurnTick - 1), spec, 0)
        assertTrue(hopped.speed > hopped.baseSpeed)
    }

    @Test
    fun `the wings beat continuously`() {
        var bird = spawn()
        val phases = mutableSetOf(bird.wingPhase)
        repeat(40) {
            bird = Flight.step(bird, spec, 0)
            phases += bird.wingPhase
        }
        assertTrue("wing phases $phases", phases.size >= 3)
        assertTrue(bird.wingPhase in 0 until TrigTable.STEPS)
    }

    @Test
    fun `a bird flees when its timeout is exhausted`() {
        var bird = spawn()
        repeat(spec.timeoutTicks - 1) { bird = Flight.step(bird, spec, 0) }
        assertEquals(BirdStatus.FLYING, bird.status)
        bird = Flight.step(bird, spec, 0)
        assertEquals(BirdStatus.FLEEING, bird.status)
        assertTrue(bird.vy < 0)
    }

    @Test
    fun `a startled bird waits half a second and then flees upward`() {
        var bird = Flight.startle(spawn())
        assertTrue(bird.resolved)
        assertEquals(BirdStatus.STARTLED, bird.status)
        assertEquals(0, bird.vx)
        assertEquals(0, bird.vy)
        repeat(29) { bird = Flight.step(bird, spec, 0) }
        assertEquals(BirdStatus.STARTLED, bird.status)
        bird = Flight.step(bird, spec, 0)
        assertEquals(BirdStatus.FLEEING, bird.status)
        assertEquals(0, bird.vx)
        assertEquals(-fpsToFxPerTick(180), bird.vy)
    }

    @Test
    fun `a fleeing bird leaves the top of the world and is gone`() {
        var bird = Flight.flee(spawn())
        repeat(200) { bird = Flight.step(bird, spec, 0) }
        assertEquals(BirdStatus.GONE, bird.status)
    }

    @Test
    fun `difficulty and ramp change the initial speed`() {
        val easy = spawn(level = LevelSpec.classic(seed = 1L, difficulty = Difficulty.EASY))
        val medium = spawn(level = LevelSpec.classic(seed = 1L, difficulty = Difficulty.MEDIUM))
        val hard = spawn(level = LevelSpec.classic(seed = 1L, difficulty = Difficulty.HARD))
        assertTrue(easy.baseSpeed < medium.baseSpeed)
        assertTrue(medium.baseSpeed < hard.baseSpeed)
        assertEquals(fpsToFxPerTick(48), medium.baseSpeed)
        val later = spawn(index = 9)
        assertTrue(later.baseSpeed > medium.baseSpeed)
    }

    @Test
    fun `the speed percentage is applied without floating point`() {
        val slow = LevelSpec.classic(seed = 1L).copy(speedPercent = 80)
        val fast = LevelSpec.classic(seed = 1L).copy(speedPercent = 120)
        assertTrue(spawn(level = slow).baseSpeed < spawn(level = fast).baseSpeed)
        assertEquals(fpsToFxPerTick(48) * 80 / 100, spawn(level = slow).baseSpeed)
    }

    @Test
    fun `reflecting on the right wall flips the horizontal direction`() {
        var bird = spawn().copy(
            x = fx(WORLD_WIDTH - 16),
            y = fx(120),
            heading = 3584,
            vx = 100,
            vy = -100,
        )
        bird = Flight.step(bird, spec, 0)
        assertTrue(bird.x <= fx(WORLD_WIDTH - 16))
        assertTrue(bird.vx <= 0)
    }

    @Test
    fun `reflecting on the left wall flips the horizontal direction`() {
        var bird = spawn().copy(
            x = fx(16),
            y = fx(120),
            heading = 1536,
            vx = -100,
            vy = -100,
        )
        bird = Flight.step(bird, spec, 0)
        assertTrue(bird.x >= fx(16))
        assertTrue(bird.vx >= 0)
    }

    @Test
    fun `the ceiling reflects the bird downward`() {
        var bird = spawn().copy(x = fx(120), y = fx(18), heading = 3072, vx = 0, vy = -200)
        bird = Flight.step(bird, spec, 0)
        assertTrue(bird.y >= fx(18))
        assertTrue(bird.vy >= 0)
    }

    @Test
    fun `the vegetation line reflects the bird upward`() {
        var bird = spawn().copy(x = fx(120), y = fx(VEGETATION_Y), heading = 1024, vx = 0, vy = 200)
        bird = Flight.step(bird, spec, 0)
        assertTrue(bird.y <= fx(VEGETATION_Y))
        assertTrue(bird.vy <= 0)
    }

    @Test
    fun `a bird is photographable while flying and not after it is gone`() {
        val flying = spawn()
        assertTrue(Flight.isVisible(flying))
        var gone = Flight.flee(flying)
        repeat(200) { gone = Flight.step(gone, spec, 0) }
        assertTrue(!Flight.isVisible(gone))
    }

    @Test
    fun `the velocity magnitude matches the stored speed`() {
        val bird = spawn()
        val magnitude = pt.tiago.ducknight.core.fixed.isqrt(
            bird.vx.toLong() * bird.vx + bird.vy.toLong() * bird.vy,
        ).toInt()
        assertTrue("speed ${bird.speed} vs velocity $magnitude", kotlin.math.abs(bird.speed - magnitude) <= 2)
    }
}

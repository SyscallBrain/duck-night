package pt.tiago.ducknight.core.photo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.fixed.TrigTable
import pt.tiago.ducknight.core.fixed.fx
import pt.tiago.ducknight.core.flight.BirdState
import pt.tiago.ducknight.core.flight.BirdStatus
import pt.tiago.ducknight.core.flight.Flight
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.core.level.GameOptions
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.species.SpeciesCatalog

class PhotoTest {
    private val spec = LevelSpec.classic(seed = 1L, difficulty = Difficulty.MEDIUM)
    private val mallard = SpeciesCatalog.byId("mallard")

    private fun bird(
        id: Int = 1,
        x: Int = 120,
        y: Int = 100,
        speed: Int = 0,
        wingPhase: Int = 0,
        status: BirdStatus = BirdStatus.FLYING,
        speciesId: String = "mallard",
    ): BirdState {
        val species = SpeciesCatalog.byId(speciesId)
        return BirdState(
            id = id,
            species = species,
            pattern = species.patterns.first(),
            x = fx(x),
            y = fx(y),
            vx = 0,
            vy = 0,
            heading = 3072,
            speed = speed,
            baseSpeed = species.speedClass.speedUps,
            ageTicks = 100,
            nextTurnTick = 200,
            slowUntilTick = 0,
            dartBurst = false,
            wingPhase = wingPhase,
            status = status,
            statusTicks = 0,
            rngState = 1L,
        )
    }

    @Test
    fun `the medium frame is 26 by 20 units`() {
        val frame = PhotoResolver.frame(spec, GameOptions())
        assertEquals(26, frame.widthUnits)
        assertEquals(20, frame.heightUnits)
    }

    @Test
    fun `easy uses a wider frame and hard a narrower one`() {
        val easy = PhotoResolver.frame(LevelSpec.classic(seed = 1L, difficulty = Difficulty.EASY), GameOptions())
        val hard = PhotoResolver.frame(LevelSpec.classic(seed = 1L, difficulty = Difficulty.HARD), GameOptions())
        assertEquals(33, easy.widthUnits)
        assertEquals(25, easy.heightUnits)
        assertEquals(22, hard.widthUnits)
        assertEquals(17, hard.heightUnits)
    }

    @Test
    fun `bigger birds widen the frame by 30 percent`() {
        val normal = PhotoResolver.frame(spec, GameOptions())
        val bigger = PhotoResolver.frame(spec, GameOptions(biggerBirds = true))
        assertEquals(34, bigger.widthUnits)
        assertEquals(26, bigger.heightUnits)
        assertTrue(bigger.widthUnits > normal.widthUnits)
    }

    @Test
    fun `tolerance changes the frame inside the documented range`() {
        val wide = PhotoResolver.frame(spec, GameOptions(tolerancePercent = 125))
        val narrow = PhotoResolver.frame(spec, GameOptions(tolerancePercent = 85))
        assertEquals(33, wide.widthUnits)
        assertEquals(22, narrow.widthUnits)
        assertTrue(wide.widthUnits > narrow.widthUnits)
    }

    @Test
    fun `a centered bird is photographed`() {
        val result = PhotoResolver.resolve(listOf(bird(x = 120, y = 100)), 120, 100, spec, GameOptions())
        assertTrue(result.hit)
        assertEquals(listOf(1), result.birdIds)
        assertEquals(1, result.mainBirdId)
        assertTrue(result.quality in 0..100)
    }

    @Test
    fun `a bird just inside the frame counts and one outside does not`() {
        val frame = PhotoResolver.frame(spec, GameOptions())
        val halfWidth = frame.widthUnits
        val inside = bird(id = 1, x = 120 + halfWidth - 4, y = 100)
        val outside = bird(id = 2, x = 120 + halfWidth + 4, y = 100)
        assertTrue(PhotoResolver.resolve(listOf(inside), 120, 100, spec, GameOptions()).hit)
        assertTrue(!PhotoResolver.resolve(listOf(outside), 120, 100, spec, GameOptions()).hit)
    }

    @Test
    fun `no birds means a miss with no points`() {
        val result = PhotoResolver.resolve(emptyList(), 120, 100, spec, GameOptions())
        assertTrue(!result.hit)
        assertEquals(0, result.quality)
        assertEquals(0, result.points)
        assertEquals(PhotoOutcome.MISS, result.outcome)
        assertNull(result.mainBirdId)
    }

    @Test
    fun `the bird closest to the center is the main bird`() {
        val near = bird(id = 1, x = 120, y = 102)
        val far = bird(id = 2, x = 120, y = 112)
        val result = PhotoResolver.resolve(listOf(far, near), 120, 100, spec, GameOptions())
        assertEquals(1, result.mainBirdId)
        assertEquals(2, result.birdIds.size)
    }

    @Test
    fun `a tie is broken by the smallest id`() {
        val high = bird(id = 7, x = 120, y = 104)
        val low = bird(id = 3, x = 120, y = 96)
        val result = PhotoResolver.resolve(listOf(high, low), 120, 100, spec, GameOptions())
        assertEquals(3, result.mainBirdId)
    }

    @Test
    fun `a gone bird is not in the photo`() {
        val gone = bird(status = BirdStatus.GONE)
        val result = PhotoResolver.resolve(listOf(gone), 120, 100, spec, GameOptions())
        assertTrue(!result.hit)
    }

    @Test
    fun `an already startled bird is not photographed twice`() {
        val startled = bird(status = BirdStatus.STARTLED)
        val result = PhotoResolver.resolve(listOf(startled), 120, 100, spec, GameOptions())
        assertTrue(!result.hit)
    }

    @Test
    fun `group photos count every bird inside the frame`() {
        val birds = listOf(
            bird(id = 1, x = 120, y = 100),
            bird(id = 2, x = 114, y = 104),
            bird(id = 3, x = 126, y = 96),
        )
        val result = PhotoResolver.resolve(birds, 120, 100, spec, GameOptions())
        assertEquals(3, result.birdIds.size)
        assertEquals(3, result.groupSize)
        assertTrue("group must add quality", result.quality >= 15)
    }

    @Test
    fun `the group bonus stops at 100`() {
        val birds = (1..10).map { bird(id = it, x = 118 + (it % 5), y = 98 + (it % 3)) }
        val result = PhotoResolver.resolve(birds, 120, 100, spec, GameOptions())
        assertTrue(result.quality <= 100)
    }

    @Test
    fun `the wing pose at the top adds ten points`() {
        val poseUp = PhotoResolver.resolve(listOf(bird(wingPhase = 2048)), 120, 100, spec, GameOptions())
        val poseDown = PhotoResolver.resolve(listOf(bird(wingPhase = 0)), 120, 100, spec, GameOptions())
        assertEquals(poseUp.quality, poseDown.quality + 10)
    }

    @Test
    fun `a faster bird is less sharp than a slow one`() {
        val slow = bird(speed = 0)
        val fast = bird(speed = 256, speciesId = "teal")
        val slowShot = PhotoResolver.resolve(listOf(slow), 120, 100, spec, GameOptions())
        val fastShot = PhotoResolver.resolve(listOf(fast), 120, 100, spec, GameOptions())
        assertTrue(slowShot.quality > fastShot.quality)
    }

    @Test
    fun `points use the species base and the quality`() {
        val result = PhotoResolver.resolve(listOf(bird()), 120, 100, spec, GameOptions())
        val expected = mallard.points * (50 + result.quality) / 100
        assertEquals(expected, result.points)
    }

    @Test
    fun `quality thresholds name the photo`() {
        assertEquals(PhotoOutcome.MISS, PhotoOutcome.fromQuality(0, hit = false))
        assertEquals(PhotoOutcome.OK, PhotoOutcome.fromQuality(59, hit = true))
        assertEquals(PhotoOutcome.GOOD, PhotoOutcome.fromQuality(60, hit = true))
        assertEquals(PhotoOutcome.GOOD, PhotoOutcome.fromQuality(89, hit = true))
        assertEquals(PhotoOutcome.PERFECT, PhotoOutcome.fromQuality(90, hit = true))
        assertEquals(PhotoOutcome.PERFECT, PhotoOutcome.fromQuality(100, hit = true))
    }

    @Test
    fun `aim assist is off by default`() {
        val off = PhotoResolver.resolve(listOf(bird(x = 140, y = 100)), 120, 100, spec, GameOptions())
        assertEquals(0, off.assistDx)
        assertEquals(0, off.assistDy)
    }

    @Test
    fun `aim assist moves at most ten units toward a nearby bird`() {
        val result = PhotoResolver.resolve(
            listOf(bird(x = 140, y = 100)),
            120,
            100,
            spec,
            GameOptions(aimAssist = true),
        )
        assertTrue(result.assistDx in 1..10)
        assertEquals(0, result.assistDy)
        assertEquals(120 + result.assistDx, result.aimX)
    }

    @Test
    fun `aim assist does not reach a bird outside its radius`() {
        val result = PhotoResolver.resolve(
            listOf(bird(x = 200, y = 100)),
            120,
            100,
            spec,
            GameOptions(aimAssist = true),
        )
        assertEquals(0, result.assistDx)
    }

    @Test
    fun `aim assist ignores a bird that cannot be photographed`() {
        val result = PhotoResolver.resolve(
            listOf(bird(x = 130, y = 100, status = BirdStatus.GONE)),
            120,
            100,
            spec,
            GameOptions(aimAssist = true),
        )
        assertEquals(0, result.assistDx)
    }

    @Test
    fun `a bird with wings open is in the pose window`() {
        assertTrue(PhotoResolver.poseBonus(TrigTable.STEPS * 40 / 100))
        assertTrue(PhotoResolver.poseBonus(TrigTable.STEPS * 50 / 100))
        assertTrue(!PhotoResolver.poseBonus(TrigTable.STEPS * 39 / 100))
        assertTrue(!PhotoResolver.poseBonus(TrigTable.STEPS * 61 / 100))
    }

    @Test
    fun `the resolver never reports a quality outside zero to one hundred`() {
        for (dx in -40..40 step 4) {
            for (dy in -30..30 step 5) {
                val result = PhotoResolver.resolve(listOf(bird(x = 120 + dx, y = 100 + dy)), 120, 100, spec, GameOptions())
                if (result.hit) assertTrue("dx=$dx dy=$dy q=${result.quality}", result.quality in 0..100)
            }
        }
    }

    @Test
    fun `a centered stationary bird with wings up is a perfect photo`() {
        val result = PhotoResolver.resolve(
            listOf(bird(speed = 0, wingPhase = TrigTable.STEPS / 2)),
            120,
            100,
            spec,
            GameOptions(),
        )
        assertEquals(100, result.quality)
        assertEquals(PhotoOutcome.PERFECT, result.outcome)
        assertEquals(500 * 150 / 100, result.points)
    }

    @Test
    fun `sharpness never falls below forty percent`() {
        val veryFast = bird(speed = 5_000, speciesId = "mallard")
        val result = PhotoResolver.resolve(listOf(veryFast), 120, 100, spec, GameOptions())
        // 55 % centering and 35 * 0.4 = 14 % sharpness, without the pose bonus.
        assertEquals(69, result.quality)
    }

    @Test
    fun `a bird farther from the center scores lower than a centered one`() {
        val centered = PhotoResolver.resolve(listOf(bird(x = 120, y = 100)), 120, 100, spec, GameOptions())
        val offCenter = PhotoResolver.resolve(listOf(bird(x = 136, y = 100)), 120, 100, spec, GameOptions())
        assertTrue(centered.quality > offCenter.quality)
    }

    @Test
    fun `a fleeing bird is not photographed`() {
        val fleeing = Flight.flee(bird())
        val result = PhotoResolver.resolve(listOf(fleeing), 120, 100, spec, GameOptions())
        assertTrue(!result.hit)
    }
}

package pt.tiago.ducknight.core.level

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.species.FlightPattern

class LevelSpecM2Test {
    @Test
    fun `double keeps the pair contract`() {
        val spec = LevelSpec.double(seed = 1L, birds = 10, need = 6)
        assertEquals(LevelType.DOUBLE, spec.type)
        assertEquals(10, spec.birds)
        assertEquals(3, spec.photosPerBird)
        assertEquals(6, spec.need)
    }

    @Test
    fun `flock fills its Vs`() {
        val spec = LevelSpec.flock(seed = 1L, flocks = 4, flockSize = 6, need = 10)
        assertEquals(LevelType.FLOCK, spec.type)
        assertEquals(24, spec.birds)
        assertEquals(6, spec.photosPerBird)
        assertTrue(spec.patterns.contains(FlightPattern.FORMATION))
    }

    @Test
    fun `film carries a shared roll`() {
        val spec = LevelSpec.film(seed = 1L, birds = 10, filmFrames = 14, need = 6)
        assertEquals(LevelType.FILM, spec.type)
        assertEquals(14, spec.rules?.filmFrames)
        assertTrue((spec.rules?.filmFrames ?: 0) < spec.birds * spec.photosPerBird)
    }

    @Test
    fun `timed carries its duration and cooldown`() {
        val spec = LevelSpec.timed(seed = 1L, seconds = 60, need = 8)
        assertEquals(LevelType.TIMED, spec.type)
        assertEquals(3_600, spec.rules?.timedTicks)
        assertEquals(21, spec.rules?.photoCooldownTicks)
        assertEquals(999, spec.photosPerBird)
    }

    @Test
    fun `protect carries the protected count`() {
        val spec = LevelSpec.protect(seed = 1L, birds = 8, protectedBirds = 2, need = 5)
        assertEquals(LevelType.PROTECT, spec.type)
        assertEquals(2, spec.rules?.protectedBirds)
    }

    @Test
    fun `clay carries pairs and corks`() {
        val spec = LevelSpec.clay(seed = 1L, pairs = 10, need = 10)
        assertEquals(LevelType.CLAY, spec.type)
        assertEquals(10, spec.rules?.clayPairs)
        assertEquals(3, spec.rules?.corksPerPair)
        assertEquals(20, spec.birds)
    }

    @Test
    fun `boss carries the album goal`() {
        val spec = LevelSpec.boss(seed = 1L, pages = 3)
        assertEquals(LevelType.BOSS, spec.type)
        assertEquals(3, spec.need)
        assertEquals(3, spec.rules?.goalPages)
        assertEquals("great_duck", spec.species.single().id)
        assertTrue(spec.patterns.contains(FlightPattern.BOSS))
    }

    @Test
    fun `modifiers are part of the variant`() {
        val spec = LevelSpec.classic(seed = 1L).copy(
            modifiers = setOf(Modifier.WIND, Modifier.FOG),
            rules = TypeRules(windStrength = 20),
        )
        assertEquals(setOf(Modifier.WIND, Modifier.FOG), spec.modifiers)
        assertEquals(20, spec.rules?.windStrength)
    }

    @Test
    fun `a double needs an even bird count`() {
        assertTrue(runCatching { LevelSpec.double(seed = 1L, birds = 9) }.isFailure)
    }

    @Test
    fun `a flock must fill its Vs`() {
        assertTrue(runCatching {
            LevelSpec.flock(seed = 1L, flocks = 3, flockSize = 5).copy(birds = 14)
        }.isFailure)
    }

    @Test
    fun `a boss need is an album size`() {
        assertTrue(runCatching { LevelSpec.boss(seed = 1L, pages = 3).copy(need = 4) }.isFailure)
    }

    @Test
    fun `a clay need must fit the plates`() {
        assertTrue(runCatching { LevelSpec.clay(seed = 1L, pairs = 2, need = 5) }.isFailure)
    }

    @Test
    fun `type rules reject impossible values`() {
        assertTrue(runCatching { TypeRules(flockSize = 0) }.isFailure)
        assertTrue(runCatching { TypeRules(filmFrames = 0) }.isFailure)
        assertTrue(runCatching { TypeRules(bossPhases = 2) }.isFailure)
        assertTrue(runCatching { TypeRules(goalPages = 4) }.isFailure)
        assertTrue(runCatching { TypeRules(windStrength = -1) }.isFailure)
    }

    @Test
    fun `the boss species is legendary`() {
        val species = pt.tiago.ducknight.core.species.SpeciesCatalog.byId("great_duck")
        assertEquals(pt.tiago.ducknight.core.species.SpeciesCategory.LEGENDARY, species.category)
        assertEquals(16, species.radius)
        assertEquals(FlightPattern.BOSS, species.patterns.single())
    }
}

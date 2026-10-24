package pt.tiago.ducknight.core.species

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpeciesTest {
    private val placeOne = SpeciesCatalog.placeOne

    @Test
    fun `place one has the four documented species`() {
        assertEquals(listOf("mallard", "teal", "gadwall", "wigeon"), placeOne.map { it.id })
    }

    @Test
    fun `every species has two names and a scientific name`() {
        for (species in placeOne) {
            assertTrue(species.id.isNotBlank())
            assertTrue(species.nameEn.isNotBlank())
            assertTrue(species.namePt.isNotBlank())
            assertTrue(species.scientific.isNotBlank())
        }
    }

    @Test
    fun `every species has at least one flight pattern`() {
        for (species in placeOne) assertTrue(species.id, species.patterns.isNotEmpty())
    }

    @Test
    fun `collision radii stay inside the documented range`() {
        for (species in placeOne) assertTrue(species.id, species.radius in 8..16)
    }

    @Test
    fun `speed classes carry the documented speeds and points`() {
        assertEquals(48, SpeedClass.S1.speedUps)
        assertEquals(500, SpeedClass.S1.points)
        assertEquals(62, SpeedClass.S2.speedUps)
        assertEquals(800, SpeedClass.S2.points)
        assertEquals(78, SpeedClass.S3.speedUps)
        assertEquals(1_000, SpeedClass.S3.points)
        assertEquals(95, SpeedClass.S4.speedUps)
        assertEquals(1_500, SpeedClass.S4.points)
        assertEquals(112, SpeedClass.S5.speedUps)
        assertEquals(2_000, SpeedClass.S5.points)
        assertEquals(104, SpeedClass.GOLDEN.speedUps)
        assertEquals(3_000, SpeedClass.GOLDEN.points)
    }

    @Test
    fun `the mallard is the slow starter`() {
        val mallard = SpeciesCatalog.byId("mallard")
        assertEquals(SpeedClass.S1, mallard.speedClass)
        assertEquals(500, mallard.points)
        assertEquals(SpeciesCategory.DABBLING, mallard.category)
        assertTrue(mallard.patterns.contains(FlightPattern.ZIGZAG))
        assertTrue(mallard.patterns.contains(FlightPattern.GLIDE))
    }

    @Test
    fun `teal darts and gadwall glides`() {
        assertTrue(SpeciesCatalog.byId("teal").patterns.contains(FlightPattern.DART))
        assertTrue(SpeciesCatalog.byId("gadwall").patterns.contains(FlightPattern.GLIDE))
        assertTrue(SpeciesCatalog.byId("wigeon").patterns.contains(FlightPattern.ZIGZAG))
    }

    @Test
    fun `no place one species is protected or a diver`() {
        for (species in placeOne) {
            assertTrue(species.id, !species.protected)
            assertTrue(species.id, !species.dives)
        }
    }

    @Test
    fun `ids are unique and lookups are exact`() {
        assertEquals(placeOne.size, placeOne.map { it.id }.toSet().size)
        assertEquals("mallard", SpeciesCatalog.byId("mallard").id)
        assertTrue(runCatching { SpeciesCatalog.byId("missing") }.isFailure)
    }

    @Test
    fun `names differ between the two languages`() {
        assertNotEquals(SpeciesCatalog.byId("mallard").nameEn, SpeciesCatalog.byId("mallard").namePt)
    }

    @Test
    fun `the catalog has the 48 species of the content spec`() {
        assertEquals(48, SpeciesCatalog.all.size)
        assertEquals(48, SpeciesCatalog.all.map { it.id }.toSet().size)
        for (species in SpeciesCatalog.all) {
            assertTrue(species.id, species.nameEn.isNotBlank())
            assertTrue(species.id, species.namePt.isNotBlank())
            assertTrue(species.id, species.scientific.isNotBlank())
            assertTrue(species.id, species.radius in 8..16)
            assertTrue(species.id, species.patterns.isNotEmpty())
        }
    }

    @Test
    fun `there are thirty targets, six protected birds and twelve bosses`() {
        val regular = listOf(
            "mallard", "teal", "gadwall", "wigeon", "shoveler", "pintail", "garganey",
            "greylag", "barnacle", "ruddy_shelduck", "shelduck", "coot", "pochard",
            "tufted_duck", "red_crested_pochard", "egyptian_goose", "snow_goose",
            "long_tailed_duck", "goldeneye", "great_crested_grebe", "red_breasted_merganser",
            "white_headed_duck", "mandarin", "wood_duck", "ferruginous_duck", "merganser",
            "scoter", "eider", "brent_goose", "golden",
        )
        val protectedIds = listOf(
            "white_stork", "heron", "little_egret", "spoonbill", "flamingo", "crane",
        )
        val bosses = listOf(
            "old_mallard", "twin_teal", "wind_goose", "mist_goose", "thief_crow",
            "royal_snow_goose", "royal_grebe", "imperial_mandarin", "ghost_duck",
            "thunder_flock", "golden_eider", "great_duck",
        )
        assertEquals(30, regular.size)
        assertEquals(6, protectedIds.size)
        assertEquals(12, bosses.size)
        assertEquals((regular + protectedIds + bosses).toSet(), SpeciesCatalog.all.map { it.id }.toSet())
        assertEquals(protectedIds.toSet(), SpeciesCatalog.all.filter { it.protected }.map { it.id }.toSet())
        assertEquals((1..12).toList(), bosses.map { SpeciesCatalog.byId(it).firstPlace }.sorted())
        for (id in bosses) {
            assertTrue(id, SpeciesCatalog.byId(id).patterns.contains(FlightPattern.BOSS))
        }
    }

    @Test
    fun `the Portuguese names follow the SPEA review`() {
        assertEquals("Zarro-comum", SpeciesCatalog.byId("pochard").namePt)
        assertEquals("Zarro-negrinha", SpeciesCatalog.byId("tufted_duck").namePt)
        assertEquals("Ganso-de-faces-pretas", SpeciesCatalog.byId("brent_goose").namePt)
        assertEquals("Olho-dourado", SpeciesCatalog.byId("goldeneye").namePt)
        assertEquals("Mergulhão-de-poupa", SpeciesCatalog.byId("great_crested_grebe").namePt)
        assertEquals("Negrola-comum", SpeciesCatalog.byId("scoter").namePt)
        assertEquals("Êider", SpeciesCatalog.byId("eider").namePt)
        assertEquals("Marrecos Gémeos", SpeciesCatalog.byId("twin_teal").namePt)
    }
}

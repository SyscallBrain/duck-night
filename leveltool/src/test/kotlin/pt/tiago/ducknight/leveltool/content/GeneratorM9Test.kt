package pt.tiago.ducknight.leveltool.content

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.level.LevelType
import pt.tiago.ducknight.core.level.Modifier
import java.nio.file.Files
import java.nio.file.Path

class GeneratorM9Test {
    private fun tempRoot(): Path = Files.createTempDirectory("duck-m9-generator")

    private fun delete(root: Path) {
        if (!Files.exists(root)) return
        Files.walk(root).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    private fun expectedMix(place: Int): Map<LevelType, Int> = when (place) {
        1 -> mapOf(LevelType.CLASSIC to 42, LevelType.FILM to 7, LevelType.BOSS to 1)
        2 -> mapOf(
            LevelType.CLASSIC to 16, LevelType.DOUBLE to 22, LevelType.FILM to 5,
            LevelType.CLAY to 6, LevelType.BOSS to 1,
        )
        3 -> mapOf(
            LevelType.CLASSIC to 10, LevelType.DOUBLE to 10, LevelType.FLOCK to 18,
            LevelType.FILM to 6, LevelType.CLAY to 5, LevelType.BOSS to 1,
        )
        4 -> mapOf(
            LevelType.CLASSIC to 12, LevelType.DOUBLE to 12, LevelType.FLOCK to 8,
            LevelType.FILM to 5, LevelType.TIMED to 8, LevelType.CLAY to 4,
            LevelType.BOSS to 1,
        )
        5 -> mapOf(
            LevelType.CLASSIC to 8, LevelType.DOUBLE to 8, LevelType.FLOCK to 6,
            LevelType.FILM to 4, LevelType.TIMED to 4, LevelType.PROTECT to 15,
            LevelType.CLAY to 4, LevelType.BOSS to 1,
        )
        12 -> mapOf(
            LevelType.CLASSIC to 8, LevelType.DOUBLE to 9, LevelType.FLOCK to 8,
            LevelType.FILM to 6, LevelType.TIMED to 6, LevelType.PROTECT to 8,
            LevelType.CLAY to 4, LevelType.BOSS to 1,
        )
        else -> mapOf(
            LevelType.CLASSIC to 9, LevelType.DOUBLE to 9, LevelType.FLOCK to 7,
            LevelType.FILM to 6, LevelType.TIMED to 5, LevelType.PROTECT to 9,
            LevelType.CLAY to 4, LevelType.BOSS to 1,
        )
    }

    @Test
    fun `the twelve places follow the content type mix`() {
        for (place in 1..12) {
            val types = Generator.typesFor(place)
            assertEquals("place $place length", 50, types.size)
            assertEquals("place $place boss", LevelType.BOSS, types.last())
            val actual = types.dropLast(1).groupingBy { it }.eachCount()
            val expected = expectedMix(place).filterKeys { it != LevelType.BOSS }
            assertEquals("place $place mix", expected, actual)
        }
    }

    @Test
    fun `the type order never repeats three times and never puts two clay levels together`() {
        for (place in 1..12) {
            val types = Generator.typesFor(place).dropLast(1)
            if (place == 1) {
                var longest = 1
                var run = 1
                for (index in 1 until types.size) {
                    run = if (types[index] == types[index - 1]) run + 1 else 1
                    longest = maxOf(longest, run)
                }
                assertTrue("place 1 run $longest", longest <= 6)
                continue
            }
            for (index in 2 until types.size) {
                assertTrue(
                    "place $place has three ${types[index]} in a row at ${index + 1}",
                    !(types[index] == types[index - 1] && types[index] == types[index - 2]),
                )
            }
            for (index in 1 until types.size) {
                assertTrue(
                    "place $place has clay at ${index + 1} after clay",
                    !(types[index] == LevelType.CLAY && types[index - 1] == LevelType.CLAY),
                )
            }
        }
    }

    @Test
    fun `place five introduces protect at levels one three and six`() {
        val types = Generator.typesFor(5)
        assertEquals(LevelType.PROTECT, types[0])
        assertEquals(LevelType.PROTECT, types[2])
        assertEquals(LevelType.PROTECT, types[5])
    }

    @Test
    fun `the place five protect pool has targets and protected birds`() {
        val root = tempRoot()
        try {
            val store = ContentStore(root)
            Generator.generate(store, place = 5, from = 1, to = 1)
            val spec = store.loadLevel("p05l001").spec(pt.tiago.ducknight.core.level.Difficulty.MEDIUM)
            assertTrue(spec.species.any { pt.tiago.ducknight.core.species.SpeciesCatalog.byId(it.id).protected })
            assertTrue(spec.species.any { !pt.tiago.ducknight.core.species.SpeciesCatalog.byId(it.id).protected })
        } finally {
            delete(root)
        }
    }

    @Test
    fun `every place has its documented boss species`() {
        val bosses = mapOf(
            1 to "old_mallard", 2 to "twin_teal", 3 to "wind_goose", 4 to "mist_goose",
            5 to "thief_crow", 6 to "royal_snow_goose", 7 to "royal_grebe",
            8 to "imperial_mandarin", 9 to "ghost_duck", 10 to "thunder_flock",
            11 to "golden_eider", 12 to "great_duck",
        )
        val root = tempRoot()
        try {
            val store = ContentStore(root)
            for (place in 1..12) {
                Generator.generate(store, place = place, from = 50, to = 50)
                val level = store.loadLevel("p%02dl050".format(place))
                assertEquals("place $place boss", bosses[place], level.variant(pt.tiago.ducknight.core.level.Difficulty.MEDIUM).species.single().id)
                assertEquals("boss-p%02d".format(place), level.family)
            }
        } finally {
            delete(root)
        }
    }

    @Test
    fun `place twelve still mixes mechanics in its introduction`() {
        val root = tempRoot()
        try {
            val store = ContentStore(root)
            Generator.generate(store, place = 12, from = 1, to = 6)
            for (ordinal in listOf(1, 3, 6)) {
                val spec = store.loadLevel("p12l%03d".format(ordinal))
                    .spec(pt.tiago.ducknight.core.level.Difficulty.MEDIUM)
                assertTrue("place 12 level $ordinal is bare: ${spec.modifiers}", spec.modifiers.isNotEmpty())
            }
        } finally {
            delete(root)
        }
    }

    @Test
    fun `place one includes the wigeon`() {
        val root = tempRoot()
        try {
            val store = ContentStore(root)
            Generator.generate(store, place = 1, from = 1, to = 1)
            val spec = store.loadLevel("p01l001").spec(pt.tiago.ducknight.core.level.Difficulty.MEDIUM)
            assertTrue(spec.species.any { it.id == "wigeon" })
        } finally {
            delete(root)
        }
    }

    @Test
    fun `place nine introduces night at levels one three and six`() {
        val root = tempRoot()
        try {
            val store = ContentStore(root)
            Generator.generate(store, place = 9, from = 1, to = 6)
            for (ordinal in listOf(1, 3, 6)) {
                val spec = store.loadLevel("p09l%03d".format(ordinal))
                    .spec(pt.tiago.ducknight.core.level.Difficulty.MEDIUM)
                assertTrue("place 9 level $ordinal night", Modifier.NIGHT in spec.modifiers)
            }
        } finally {
            delete(root)
        }
    }

    @Test
    fun `the place mechanics are present at levels one three and six`() {
        val root = tempRoot()
        try {
            val store = ContentStore(root)
            Generator.generate(store, place = 6, from = 1, to = 6)
            for (ordinal in listOf(1, 3, 6)) {
                val spec = store.loadLevel("p06l%03d".format(ordinal))
                    .spec(pt.tiago.ducknight.core.level.Difficulty.MEDIUM)
                assertTrue("place 6 level $ordinal snow", Modifier.SNOW in spec.modifiers)
                assertTrue("place 6 level $ordinal advance", Modifier.MANUAL_ADVANCE in spec.modifiers)
            }
        } finally {
            delete(root)
        }
    }

    @Test
    fun `night and fog never share a level and modifiers stay capped`() {
        val root = tempRoot()
        try {
            val store = ContentStore(root)
            for (place in 9..12) {
                Generator.generate(store, place = place, from = 1, to = 49)
                val max = if (place >= 10) 3 else 2
                for (ordinal in 1..49) {
                    val level = store.loadLevel("p%02dl%03d".format(place, ordinal))
                    val spec = level.spec(pt.tiago.ducknight.core.level.Difficulty.MEDIUM)
                    assertTrue("place $place level $ordinal", spec.modifiers.size <= max)
                    assertTrue(
                        "place $place level $ordinal night+fog",
                        !(Modifier.NIGHT in spec.modifiers && Modifier.FOG in spec.modifiers),
                    )
                    if (level.type == LevelType.CLAY) assertTrue(spec.modifiers.isEmpty())
                    if (level.type == LevelType.FLOCK || level.type == LevelType.FILM) {
                        assertTrue("place $place level $ordinal night flock", Modifier.NIGHT !in spec.modifiers)
                    }
                }
            }
        } finally {
            delete(root)
        }
    }
}

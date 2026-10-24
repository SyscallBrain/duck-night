package pt.tiago.ducknight.leveltool.content
import pt.tiago.ducknight.core.content.*

import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.core.level.LevelType
import java.nio.file.Files
import java.nio.file.Path

class JsonFilesTest {
    @Test
    fun `objects are written with sorted keys and a final newline`() {
        val element = kotlinx.serialization.json.buildJsonObject {
            put("z", kotlinx.serialization.json.JsonPrimitive(1))
            put("a", kotlinx.serialization.json.JsonPrimitive(2))
        }
        val text = JsonFiles.encode(element)
        assertTrue(text.endsWith("\n"))
        assertTrue(text.indexOf("\"a\"") < text.indexOf("\"z\""))
    }

    @Test
    fun `nested objects are sorted recursively`() {
        val element = kotlinx.serialization.json.buildJsonObject {
            put(
                "z",
                kotlinx.serialization.json.buildJsonObject {
                    put("b", kotlinx.serialization.json.JsonPrimitive(1))
                    put("a", kotlinx.serialization.json.JsonPrimitive(2))
                },
            )
        }
        val text = JsonFiles.encode(element)
        val parsed = JsonFiles.json.parseToJsonElement(text).jsonObject
        assertEquals(listOf("a", "b"), parsed["z"]!!.jsonObject.keys.toList())
    }

    @Test
    fun `the same value always encodes to the same bytes`() {
        val value = LevelFile(
            id = "p01l001", place = 1, ordinal = 1, type = LevelType.CLASSIC,
            family = "classic-open",
            variants = emptyMap(),
        )
        assertEquals(JsonFiles.encodeValue(value), JsonFiles.encodeValue(value))
    }

    @Test
    fun `sha256 is stable and hexadecimal`() {
        val bytes = "duck night".toByteArray()
        val hash = JsonFiles.sha256(bytes)
        assertEquals(64, hash.length)
        assertTrue(hash.all { it in "0123456789abcdef" })
        assertEquals(hash, JsonFiles.sha256(bytes))
    }
}

class ContentStoreTest {
    private fun tempRoot(): Path = Files.createTempDirectory("duck-content-test")

    @Test
    fun `generate writes levels, variants and a catalog`() {
        val root = tempRoot()
        try {
            val store = ContentStore(root)
            val levels = Generator.generate(store, place = 1, from = 1, to = 2)
            assertEquals(2, levels.size)
            assertEquals(3, levels.first().variants.size)
            val catalog = store.loadCatalog()
            assertEquals(listOf("p01l001", "p01l002"), catalog.levelIds)
            assertTrue(catalog.files.containsKey("levels/p01/p01l001.json"))
            assertTrue(catalog.files.containsKey("species.json"))
            assertTrue(Validator.validate(store, strict = false).isEmpty())
        } finally {
            delete(root)
        }
    }

    @Test
    fun `the level spec keeps the variant data`() {
        val root = tempRoot()
        try {
            val store = ContentStore(root)
            val level = Generator.generate(store, 1, 1, 1).single()
            val easy = level.spec(Difficulty.EASY)
            assertEquals(level.id, easy.id)
            assertEquals(80, easy.speedPercent)
            assertEquals(125, easy.frameScalePercent)
            assertEquals(5, easy.need)
            assertEquals(3, easy.photosPerBird)
            assertEquals(6_000, level.spec(Difficulty.HARD).stars.three)
        } finally {
            delete(root)
        }
    }

    @Test
    fun `the catalog hashes every published file`() {
        val root = tempRoot()
        try {
            val store = ContentStore(root)
            Generator.generate(store, 1, 1, 1)
            val catalog = store.loadCatalog()
            for ((relative, hash) in catalog.files) {
                assertEquals(relative, JsonFiles.sha256(store.root.resolve(relative)), hash)
            }
        } finally {
            delete(root)
        }
    }

    @Test
    fun `a missing variant is rejected`() {
        val root = tempRoot()
        try {
            val store = ContentStore(root)
            val level = Generator.generate(store, 1, 1, 1).single()
            val broken = level.copy(variants = level.variants - "HARD")
            assertTrue(runCatching { broken.spec(Difficulty.HARD) }.isFailure)
        } finally {
            delete(root)
        }
    }

    private fun delete(root: Path) {
        Files.walk(root).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }
}

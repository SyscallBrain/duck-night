package pt.tiago.ducknight.leveltool.content
import pt.tiago.ducknight.core.content.*

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.file.Files
import java.nio.file.Path

class PipelineIntegrationTest {
    private val store = ContentStore(Path.of("content"))

    @Test
    fun `the committed catalog validates strictly`() {
        val errors = Validator.validate(store, strict = true)
        assertTrue(errors.joinToString("\n"), errors.isEmpty())
    }

    @Test
    fun `every committed certificate replays with the current engine`() {
        val errors = Certifier.verify(store)
        assertTrue(errors.joinToString("\n"), errors.isEmpty())
    }

    @Test
    fun `there are six hundred levels and eighteen hundred certificates`() {
        assertEquals(600, store.levelFiles().size)
        assertEquals(1800, store.loadCertificates().size)
    }

    @Test
    fun `every certificate has a three star witness`() {
        for (certificate in store.loadCertificates()) {
            assertTrue(certificate.stars.three > certificate.stars.two)
            assertTrue(certificate.witness.inputLog.entries.isNotEmpty())
            assertTrue(certificate.witness.finalTick > 0)
        }
    }

    @Test
    fun `the daily pool has 365 entries`() {
        val pool = store.readDailyPool()
        assertEquals(365, pool.entries.size)
        assertTrue(pool.entries.all { it.rounds.size == 3 })
    }

    @Test
    fun `two exports are byte identical`() {
        val first = Files.createTempDirectory("duck-export-a")
        val second = Files.createTempDirectory("duck-export-b")
        try {
            val a = Exporter.export(store, first)
            val b = Exporter.export(store, second)
            assertEquals(a.files, b.files)
            assertEquals(a.bytes, b.bytes)
            assertEquals(files(first), files(second))
            for (relative in files(first)) {
                assertEquals(JsonFiles.sha256(first.resolve(relative)), JsonFiles.sha256(second.resolve(relative)))
            }
        } finally {
            delete(first)
            delete(second)
        }
    }

    @Test
    fun `export is faster than thirty seconds`() {
        val out = Files.createTempDirectory("duck-export-speed")
        try {
            val result = Exporter.export(store, out)
            assertTrue("${result.milliseconds} ms", result.milliseconds < 30_000)
        } finally {
            delete(out)
        }
    }

    @Test
    fun `the report mentions the levels and the bands`() {
        val out = Files.createTempFile("duck-report", ".md")
        try {
            Reporter.write(store, out)
            val text = Files.readString(out)
            assertTrue(text.contains("Levels | 600"))
            assertTrue(text.contains("Calibration"))
            assertTrue(text.contains("Outliers"))
            assertTrue(text.contains("above the bands"))
            assertTrue(text.contains("below the bands"))
            assertTrue(text.contains("zero-success"))
        } finally {
            Files.deleteIfExists(out)
        }
    }

    private fun files(root: Path): List<String> = Files.walk(root).use { stream ->
        stream.filter { Files.isRegularFile(it) }
            .map { root.relativize(it).toString().replace('\\', '/') }
            .sorted()
            .toList()
    }

    private fun delete(root: Path) {
        Files.walk(root).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }
}

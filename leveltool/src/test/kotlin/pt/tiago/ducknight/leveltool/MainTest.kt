package pt.tiago.ducknight.leveltool

import org.junit.Assert.assertTrue
import org.junit.Test

class MainTest {
    @Test
    fun `usage lists every command from the plan`() {
        val commands = listOf(
            "validate", "generate", "calibrate", "certify",
            "verify-certificates", "report", "export-assets", "daily-pool",
        )
        for (command in commands) {
            assertTrue("usage must document $command", USAGE.contains(command))
        }
    }

    @Test
    fun `usage never mentions a trademark of the original game`() {
        val forbidden = listOf("Duck Hunt", "Nintendo", "Zapper", "Family Game")
        for (name in forbidden) {
            assertTrue("usage must not contain $name", !USAGE.contains(name, ignoreCase = true))
        }
        // "witness" contains "nes", so the console acronym needs word boundaries.
        assertTrue("usage must not contain NES", !Regex("\\bNES\\b", RegexOption.IGNORE_CASE).containsMatchIn(USAGE))
    }
}

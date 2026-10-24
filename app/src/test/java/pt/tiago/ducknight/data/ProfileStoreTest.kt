package pt.tiago.ducknight.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import pt.tiago.ducknight.progress.BirdRecord
import pt.tiago.ducknight.progress.LevelProgress
import pt.tiago.ducknight.progress.Profile
import java.nio.file.Files

class ProfileStoreTest {
    @Test
    fun `a profile saves and loads`() {
        val dir = Files.createTempDirectory("duck-profile").toFile()
        val store = ProfileStore(dir)
        val profile = Profile(
            levels = mapOf("p01l001:EASY" to LevelProgress(stars = 2)),
            birds = mapOf("mallard" to BirdRecord(seen = 3, photographed = 2, bestQuality = 80)),
        )
        store.save(profile)
        assertEquals(profile, store.load())
    }

    @Test
    fun `export and import preserve the whole profile`() {
        val dir = Files.createTempDirectory("duck-profile-export").toFile()
        val store = ProfileStore(dir)
        val profile = Profile(
            levels = mapOf("p01l002:HARD" to LevelProgress(stars = 3, bestScore = 9_000)),
            birds = mapOf("teal" to BirdRecord(seen = 4, photographed = 3, bestQuality = 90)),
            achievements = setOf("first_photo", "first_good"),
        )
        val text = store.export(profile)
        assertEquals(profile, store.import(text))
    }

    @Test
    fun `the backup recovers a broken primary`() {
        val dir = Files.createTempDirectory("duck-profile-bak").toFile()
        val store = ProfileStore(dir)
        val profile = Profile(levels = mapOf("p01l001:EASY" to LevelProgress(stars = 1)))
        store.save(profile)
        dir.resolve("profile.json").writeText("{broken")
        assertEquals(profile, store.load())
    }

    @Test
    fun `a malformed import is rejected`() {
        val dir = Files.createTempDirectory("duck-profile-bad").toFile()
        assertNull(ProfileStore(dir).import("{"))
    }
}

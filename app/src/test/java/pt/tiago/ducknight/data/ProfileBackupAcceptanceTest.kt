package pt.tiago.ducknight.data

import org.junit.Assert.assertEquals
import org.junit.Test

import pt.tiago.ducknight.progress.BirdRecord
import pt.tiago.ducknight.progress.LevelProgress
import pt.tiago.ducknight.progress.Profile
import java.nio.file.Files

/** M7 acceptance: unlock, bird book and achievements survive export -> reset -> import. */
class ProfileBackupAcceptanceTest {
    @Test
    fun `export then reset then import restores everything`() {
        val dir = Files.createTempDirectory("duck-backup-acceptance").toFile()
        val store = ProfileStore(dir)
        val original = Profile(
            levels = mapOf(
                "p01l001:EASY" to LevelProgress(stars = 3, bestScore = 7_500, attempts = 2),
                "p01l002:EASY" to LevelProgress(stars = 1, bestScore = 4_100, attempts = 1),
            ),
            birds = mapOf(
                "mallard" to BirdRecord(seen = 5, photographed = 3, bestQuality = 91),
                "teal" to BirdRecord(seen = 2, photographed = 1, bestQuality = 76),
            ),
            achievements = setOf("first_photo", "first_good"),
            dogs = setOf("farrusco", "pantufa"),
            selectedDog = "pantufa",
        )
        store.save(original)
        val backup = store.export(original)
        store.save(original.resetCampaign())
        assertEquals(0, store.load().levels.size)
        val imported = store.import(backup)!!
        store.save(imported)
        assertEquals(original, store.load())
    }
}

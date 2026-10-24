package pt.tiago.ducknight

import android.content.Context
import pt.tiago.ducknight.audio.GameAudio
import pt.tiago.ducknight.audio.Haptics
import pt.tiago.ducknight.audio.MusicPlayer
import pt.tiago.ducknight.catalog.ContentRepository
import pt.tiago.ducknight.data.ProfileStore
import pt.tiago.ducknight.data.SavedLevelStore
import pt.tiago.ducknight.data.SettingsRepository

class AppContainer(context: Context) {
    val content: ContentRepository by lazy { ContentRepository(context.assets) }
    val saved: SavedLevelStore = SavedLevelStore(context.filesDir)
    val settings: SettingsRepository by lazy { SettingsRepository.create(context) }
    val profile: ProfileStore by lazy { ProfileStore(context.filesDir.resolve("progress").apply { mkdirs() }) }
    val audio: GameAudio by lazy { GameAudio(context) }
    val haptics: Haptics by lazy { Haptics(context) }
    val music: MusicPlayer by lazy { MusicPlayer(context) }

    fun release() {
        audio.release()
        music.release()
    }
}

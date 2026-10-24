package pt.tiago.ducknight.audio

import android.content.Context
import android.media.MediaPlayer
import pt.tiago.ducknight.R

enum class MusicTrack(val resource: Int) {
    HOME(R.raw.ambient_home),
    PLACE_1(R.raw.ambient_p01),
    PLACE_2(R.raw.ambient_p02),
    PLACE_3(R.raw.ambient_p03),
    PLACE_4(R.raw.ambient_p04),
    PLACE_5(R.raw.ambient_p05),
    PLACE_6(R.raw.ambient_p06),
    PLACE_7(R.raw.ambient_p07),
    PLACE_8(R.raw.ambient_p08),
    PLACE_9(R.raw.ambient_p09),
    PLACE_10(R.raw.ambient_p10),
    PLACE_11(R.raw.ambient_p11),
    PLACE_12(R.raw.ambient_p12),
    FAIR(R.raw.ambient_fair),
    ;

    companion object {
        /** Each place has its own ambient track; the fair has the stall's. */
        fun forPlace(place: Int): MusicTrack = entries.firstOrNull { it.name == "PLACE_$place" } ?: PLACE_1
    }
}

/** One looping ambient track at a time. */
class MusicPlayer(private val context: Context) {
    private var player: MediaPlayer? = null
    private var current: MusicTrack? = null
    var volume: Float = 0.4f
        set(value) {
            field = value.coerceIn(0f, 1f)
            player?.setVolume(field, field)
        }

    fun play(track: MusicTrack) {
        if (current == track && player?.isPlaying == true) return
        player?.release()
        player = MediaPlayer.create(context, track.resource)?.apply {
            isLooping = true
            setVolume(volume, volume)
            start()
        }
        current = track
    }

    fun pause() {
        if (player?.isPlaying == true) player?.pause()
    }

    fun resume() {
        if (current != null && player?.isPlaying == false) player?.start()
    }

    fun release() {
        player?.release()
        player = null
        current = null
    }
}

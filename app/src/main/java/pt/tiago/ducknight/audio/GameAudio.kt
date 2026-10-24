package pt.tiago.ducknight.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import pt.tiago.ducknight.R

/** Small SoundPool wrapper. The shutter is loaded first and has priority. */
class GameAudio(context: Context) {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(6)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val ids: Map<Int, Int> = listOf(
        R.raw.shutter,
        R.raw.shutter_flash,
        R.raw.advance,
        R.raw.empty,
        R.raw.photo_good,
        R.raw.photo_perfect,
        R.raw.fled,
        R.raw.laugh,
        R.raw.bark,
        R.raw.protected_bird,
        R.raw.star,
        R.raw.pass,
        R.raw.fail,
    ).associateWith { pool.load(context, it, 1) }

    var volume: Float = 0.8f

    private fun play(resource: Int, level: Float = volume) {
        if (volume <= 0f) return
        ids[resource]?.let { pool.play(it, level, level, 1, 0, 1f) }
    }

    fun shutter() = play(R.raw.shutter, 1f)
    fun shutterWithFlash() = play(R.raw.shutter_flash, 1f)
    fun advance() = play(R.raw.advance)
    fun empty() = play(R.raw.empty)
    fun good() = play(R.raw.photo_good)
    fun perfect() = play(R.raw.photo_perfect)
    fun fled() = play(R.raw.fled)
    fun laugh() = play(R.raw.laugh)
    fun bark() = play(R.raw.bark)
    fun protected() = play(R.raw.protected_bird)
    fun star() = play(R.raw.star)
    fun passed() = play(R.raw.pass)
    fun failed() = play(R.raw.fail)

    fun release() {
        pool.release()
    }
}

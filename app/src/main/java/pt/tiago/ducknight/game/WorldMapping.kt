package pt.tiago.ducknight.game

import pt.tiago.ducknight.core.fixed.WORLD_HEIGHT
import pt.tiago.ducknight.core.fixed.WORLD_WIDTH
import kotlin.math.roundToInt

/**
 * Maps a touch in the game canvas to whole world units. The world is always 240 x 300; the
 * canvas is scaled to fill the field without distortion.
 */
object WorldMapping {
    fun worldX(px: Float, width: Float): Int {
        require(width > 0f)
        return (px / width * WORLD_WIDTH).roundToInt().coerceIn(0, WORLD_WIDTH)
    }

    fun worldY(py: Float, height: Float): Int {
        require(height > 0f)
        return (py / height * WORLD_HEIGHT).roundToInt().coerceIn(0, WORLD_HEIGHT)
    }
}

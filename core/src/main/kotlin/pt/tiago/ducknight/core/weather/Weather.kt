package pt.tiago.ducknight.core.weather

import kotlinx.serialization.Serializable

/** A horizontal bank of fog that hides birds inside its band. */
@Serializable
data class FogBank(
    val y: Int,
    val height: Int,
    val speed: Int = 0,
    val phase: Int = 0,
) {
    init {
        require(y in 0..300) { "Fog bank y outside the world" }
        require(height > 0) { "Fog bank needs a height" }
    }

    fun covers(birdY: Int): Boolean = birdY in y until (y + height)
}

/** A rectangle of trees or bushes that hides a bird behind it. */
@Serializable
data class CoverBox(
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
) {
    init {
        require(width > 0 && height > 0) { "Cover box needs a positive size" }
    }

    fun covers(birdX: Int, birdY: Int): Boolean =
        birdX in x until (x + width) && birdY in y until (y + height)
}

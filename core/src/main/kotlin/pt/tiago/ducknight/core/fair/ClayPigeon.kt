package pt.tiago.ducknight.core.fair

enum class ClayStatus { FLYING, BROKEN, MISSED }

/**
 * One clay plate of the fair. Positions and speeds are fixed point;
 * [scalePermille] goes from 1000 at launch to 350 when it is far away.
 */
data class ClayPigeon(
    val id: Int,
    val pairId: Int,
    val x: Int,
    val y: Int,
    val vx: Int,
    val vy: Int,
    val scalePermille: Int,
    val ageTicks: Int,
    val flightTicks: Int,
    val status: ClayStatus,
    val fromLeft: Boolean,
)

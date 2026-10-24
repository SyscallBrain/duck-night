package pt.tiago.ducknight.core.photo

import pt.tiago.ducknight.core.fixed.FX_ONE
import pt.tiago.ducknight.core.fixed.TrigTable
import pt.tiago.ducknight.core.fixed.clamp
import pt.tiago.ducknight.core.fixed.fx
import pt.tiago.ducknight.core.fixed.isqrt
import pt.tiago.ducknight.core.fixed.toUnits
import pt.tiago.ducknight.core.flight.BirdState
import pt.tiago.ducknight.core.flight.BirdStatus
import pt.tiago.ducknight.core.flight.Flight
import pt.tiago.ducknight.core.level.GameOptions
import pt.tiago.ducknight.core.level.LevelSpec

enum class PhotoOutcome {
    MISS,
    OK,
    GOOD,
    PERFECT,
    PROTECTED,
    ;

    companion object {
        fun fromQuality(quality: Int, hit: Boolean): PhotoOutcome = when {
            !hit -> MISS
            quality >= 90 -> PERFECT
            quality >= 60 -> GOOD
            else -> OK
        }
    }
}

/** Frame half-width and half-height in world units. */
data class PhotoFrame(val widthUnits: Int, val heightUnits: Int)

/**
 * Result of the shutter. [birdIds] lists every bird inside the frame; the main bird is the one
 * closest to the center and the one that scores. [aimX]/[aimY] are the aim actually used after
 * aim assist, and [assistDx]/[assistDy] are the assist that was applied (0 when disabled).
 */
data class PhotoResult(
    val hit: Boolean,
    val birdIds: List<Int>,
    val mainBirdId: Int?,
    val groupSize: Int,
    val quality: Int,
    val points: Int,
    val outcome: PhotoOutcome,
    val aimX: Int,
    val aimY: Int,
    val assistDx: Int,
    val assistDy: Int,
    /** Protected birds inside the frame; they never count as the target. */
    val protectedCount: Int = 0,
    /** Points removed by protected birds (500 alone, 250 mixed). */
    val penalty: Int = 0,
)

/**
 * Resolves a photo with integer arithmetic only.
 * The frame is a rectangle centered on the aim; visibility is decided by [Flight.isVisible].
 */
object PhotoResolver {
    private const val FRAME_HALF_WIDTH_UNITS = 26
    private const val BIGGER_BIRD_NUMERATOR = 13
    private const val NORMAL_BIRD_NUMERATOR = 10
    private const val ASSIST_UNITS = 10

    fun frame(spec: LevelSpec, options: GameOptions): PhotoFrame {
        val scale = spec.frameScalePercent * options.tolerancePercent
        val birdFactor = if (options.biggerBirds) BIGGER_BIRD_NUMERATOR else NORMAL_BIRD_NUMERATOR
        val numerator = FRAME_HALF_WIDTH_UNITS.toLong() * scale * birdFactor
        val denominator = 100L * 100L * NORMAL_BIRD_NUMERATOR
        val width = ((numerator + denominator / 2) / denominator).toInt()
        val height = (width * 3 + 2) / 4
        return PhotoFrame(width, height)
    }

    fun resolve(
        birds: List<BirdState>,
        aimX: Int,
        aimY: Int,
        spec: LevelSpec,
        options: GameOptions,
        visible: (BirdState) -> Boolean = { true },
    ): PhotoResult {
        val frame = frame(spec, options)
        val assist = if (options.aimAssist) {
            aimAssist(birds, aimX, aimY, frame, visible)
        } else {
            Assist(0, 0)
        }
        val usedX = aimX + assist.dx
        val usedY = aimY + assist.dy
        val aimFx = fx(usedX)
        val aimFy = fx(usedY)
        val halfWidthFx = fx(frame.widthUnits)
        val halfHeightFx = fx(frame.heightUnits)

        val inside = birds.filter { bird ->
            visible(bird) && bird.status == BirdStatus.FLYING && !bird.resolved &&
                insideFrame(bird, aimFx, aimFy, halfWidthFx, halfHeightFx)
        }
        val targets = inside.filter { !it.species.protected }
        val protectedBirds = inside.filter { it.species.protected }
        if (targets.isEmpty()) {
            if (protectedBirds.isNotEmpty()) {
                return PhotoResult(
                    hit = false,
                    birdIds = protectedBirds.map { it.id },
                    mainBirdId = null,
                    groupSize = 0,
                    quality = 0,
                    points = -500,
                    outcome = PhotoOutcome.PROTECTED,
                    aimX = usedX,
                    aimY = usedY,
                    assistDx = assist.dx,
                    assistDy = assist.dy,
                    protectedCount = protectedBirds.size,
                    penalty = 500,
                )
            }
            return PhotoResult(
                hit = false,
                birdIds = emptyList(),
                mainBirdId = null,
                groupSize = 0,
                quality = 0,
                points = 0,
                outcome = PhotoOutcome.MISS,
                aimX = usedX,
                aimY = usedY,
                assistDx = assist.dx,
                assistDy = assist.dy,
            )
        }

        val main = targets.minWithOrNull(compareBy<BirdState> { centerDistanceSquared(it, aimFx, aimFy) }.thenBy { it.id })!!
        val dx = kotlin.math.abs(main.x - aimFx)
        val dy = kotlin.math.abs(main.y - aimFy)
        val centerRatio = maxOf(
            (dx.toLong() * 1000 / halfWidthFx).toInt(),
            (dy.toLong() * 1000 / halfHeightFx).toInt(),
        ).coerceIn(0, 1000)
        val centering = 1000 - centerRatio

        val speed = Flight.speedUps(main)
        val maxSpeed = main.species.speedClass.speedUps
        var sharpness = clamp(1000 - 600 * speed / maxSpeed, 400, 1000)
        if (pt.tiago.ducknight.core.level.Modifier.RAIN in spec.modifiers) {
            sharpness = sharpness * 85 / 100
        }

        val pose = if (poseBonus(main.wingPhase)) 10 else 0
        val hasProtected = protectedBirds.isNotEmpty()
        val groupBonus = if (hasProtected) 0 else 15 * (targets.size - 1)
        val base = (55L * centering + 35L * sharpness + 500) / 1000
        val quality = (base + pose + groupBonus).coerceIn(0, 100).toInt()
        val normalPoints = main.species.points * (50 + quality) / 100
        val points = if (hasProtected) (normalPoints - 250).coerceAtLeast(0) else normalPoints

        return PhotoResult(
            hit = true,
            birdIds = targets.map { it.id },
            mainBirdId = main.id,
            groupSize = targets.size,
            quality = quality,
            points = points,
            outcome = PhotoOutcome.fromQuality(quality, hit = true),
            aimX = usedX,
            aimY = usedY,
            assistDx = assist.dx,
            assistDy = assist.dy,
            protectedCount = protectedBirds.size,
            penalty = if (hasProtected) 250 else 0,
        )
    }

    fun poseBonus(wingPhase: Int): Boolean {
        val lower = TrigTable.STEPS * 40 / 100
        val upper = TrigTable.STEPS * 60 / 100
        return wingPhase in lower..upper
    }

    private fun insideFrame(
        bird: BirdState,
        aimFx: Int,
        aimFy: Int,
        halfWidthFx: Int,
        halfHeightFx: Int,
    ): Boolean {
        if (!Flight.isVisible(bird)) return false
        val shrink = (3 * bird.species.radius + 5) / 10
        val innerX = fx(maxOf(0, halfWidthFx / FX_ONE - shrink))
        val innerY = fx(maxOf(0, halfHeightFx / FX_ONE - shrink))
        return kotlin.math.abs(bird.x - aimFx) <= innerX && kotlin.math.abs(bird.y - aimFy) <= innerY
    }

    private fun centerDistanceSquared(bird: BirdState, aimFx: Int, aimFy: Int): Long {
        val dx = bird.x.toLong() - aimFx
        val dy = bird.y.toLong() - aimFy
        return dx * dx + dy * dy
    }

    private data class Assist(val dx: Int, val dy: Int)

    private fun aimAssist(
        birds: List<BirdState>,
        aimX: Int,
        aimY: Int,
        frame: PhotoFrame,
        visible: (BirdState) -> Boolean,
    ): Assist {
        val candidates = birds.filter {
            it.status == BirdStatus.FLYING && !it.resolved && visible(it)
        }
        if (candidates.isEmpty()) return Assist(0, 0)
        var best: BirdState? = null
        var bestDistance = Long.MAX_VALUE
        for (bird in candidates) {
            val dx = toUnits(bird.x) - aimX
            val dy = toUnits(bird.y) - aimY
            val distance = isqrt(dx.toLong() * dx + dy.toLong() * dy)
            if (distance < bestDistance || (distance == bestDistance && best != null && bird.id < best.id)) {
                best = bird
                bestDistance = distance
            }
        }
        val bird = best ?: return Assist(0, 0)
        // The assist only reaches 1.6 frame half-widths, compared without floats.
        if (bestDistance * 10 > frame.widthUnits.toLong() * 16) return Assist(0, 0)
        val dx = toUnits(bird.x) - aimX
        val dy = toUnits(bird.y) - aimY
        if (bestDistance == 0L) return Assist(0, 0)
        val step = minOf(ASSIST_UNITS.toLong(), bestDistance)
        return Assist(
            dx = (dx * step / bestDistance).toInt(),
            dy = (dy * step / bestDistance).toInt(),
        )
    }
}

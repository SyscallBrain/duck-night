package pt.tiago.ducknight.ui.screens.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import pt.tiago.ducknight.core.engine.GameState
import pt.tiago.ducknight.core.flight.BirdStatus
import pt.tiago.ducknight.core.level.Modifier
import kotlin.math.max
import kotlin.math.sin

/** What sits on the horizon line. */
enum class Horizon { TREES, HILLS, PINES, REEDBED, SNOWY_RIDGE, FLAT, ROUND_TREES, HEATH, FJORD, NEST }

/** The middle ground between the horizon and the bank. */
enum class Ground { WATER, FIELD, SNOW, SALT, LEAF_LITTER, HEATH }

/** The vegetation in front of everything, which the birds rise from behind. */
enum class Foreground { REEDS, WHEAT, GRASS, SNOWY_ROCKS, POSTS, BUSHES, HEATHER }

enum class Celestial { SUN, MOON, PALE_SUN, NONE }

/**
 * One place's painted scene: sky, sun or moon, horizon, ground and foreground. Everything is drawn
 * in world units (240 × 300) so the same recipe fits any screen.
 */
data class SceneRecipe(
    val sky: List<Color>,
    val celestial: Celestial,
    val celestialX: Float,
    val celestialY: Float,
    val celestialRadius: Float,
    val celestialColor: Color,
    val horizon: Horizon,
    val horizonColor: Color,
    val ground: Ground,
    val groundTop: Color,
    val groundBottom: Color,
    val foreground: Foreground,
    val foregroundColor: Color,
    val stars: Boolean = false,
    val aurora: Boolean = false,
    val clouds: Color? = null,
    val fireflies: Boolean = false,
    val fallingLeaves: Boolean = false,
)

/** The bank at the bottom of the field; the dog stands on it. */
val BankColor = Color(0xFF0D0714)

fun sceneFor(place: Int): SceneRecipe = when (place) {
    2 -> SceneRecipe(
        sky = listOf(Color(0xFF2B1A3A), Color(0xFF8A3F4A), Color(0xFFE28A3C), Color(0xFFF7C25E)),
        celestial = Celestial.SUN, celestialX = 70f, celestialY = 150f, celestialRadius = 30f,
        celestialColor = Color(0xFFFFD98A),
        horizon = Horizon.HILLS, horizonColor = Color(0xFF3A2030),
        ground = Ground.FIELD, groundTop = Color(0xFFB9853A), groundBottom = Color(0xFF4A2C1A),
        foreground = Foreground.WHEAT, foregroundColor = Color(0xFF2A1810),
    )
    3 -> SceneRecipe(
        sky = listOf(Color(0xFF1E2438), Color(0xFF4A5470), Color(0xFFB7837A), Color(0xFFE9B48A)),
        celestial = Celestial.PALE_SUN, celestialX = 160f, celestialY = 110f, celestialRadius = 18f,
        celestialColor = Color(0xFFFFE2B8),
        horizon = Horizon.PINES, horizonColor = Color(0xFF141824),
        ground = Ground.FIELD, groundTop = Color(0xFF3E4A3A), groundBottom = Color(0xFF151A14),
        foreground = Foreground.GRASS, foregroundColor = Color(0xFF0B0F0B),
        clouds = Color(0xFF6B6E86),
    )
    4 -> SceneRecipe(
        sky = listOf(Color(0xFF0E1630), Color(0xFF233A62), Color(0xFF5A7896), Color(0xFF9DB4C4)),
        celestial = Celestial.PALE_SUN, celestialX = 186f, celestialY = 128f, celestialRadius = 14f,
        celestialColor = Color(0xFFDDE8F0),
        horizon = Horizon.REEDBED, horizonColor = Color(0xFF101A2A),
        ground = Ground.WATER, groundTop = Color(0xFF2A4058), groundBottom = Color(0xFF0A1220),
        foreground = Foreground.REEDS, foregroundColor = Color(0xFF060A12),
    )
    5 -> SceneRecipe(
        sky = listOf(Color(0xFF2A1838), Color(0xFF6E3C6A), Color(0xFFD9839A), Color(0xFFF4BFB4)),
        celestial = Celestial.SUN, celestialX = 54f, celestialY = 140f, celestialRadius = 20f,
        celestialColor = Color(0xFFFFD0C0),
        horizon = Horizon.HILLS, horizonColor = Color(0xFF3A2440),
        ground = Ground.FIELD, groundTop = Color(0xFF5A6A4A), groundBottom = Color(0xFF1C2018),
        foreground = Foreground.GRASS, foregroundColor = Color(0xFF12100E),
    )
    6 -> SceneRecipe(
        sky = listOf(Color(0xFF1A2440), Color(0xFF4A5A80), Color(0xFF9AA6C6), Color(0xFFD6D8E8)),
        celestial = Celestial.PALE_SUN, celestialX = 150f, celestialY = 96f, celestialRadius = 16f,
        celestialColor = Color(0xFFF2F4FF),
        horizon = Horizon.SNOWY_RIDGE, horizonColor = Color(0xFF3A4462),
        ground = Ground.SNOW, groundTop = Color(0xFFDDE4F0), groundBottom = Color(0xFF8C97B4),
        foreground = Foreground.SNOWY_ROCKS, foregroundColor = Color(0xFF2A3048),
    )
    7 -> SceneRecipe(
        sky = listOf(Color(0xFF1A2A3A), Color(0xFF3E6A7A), Color(0xFFD8A38A), Color(0xFFF6D2A8)),
        celestial = Celestial.SUN, celestialX = 120f, celestialY = 158f, celestialRadius = 22f,
        celestialColor = Color(0xFFFFE0B0),
        horizon = Horizon.FLAT, horizonColor = Color(0xFF26343E),
        ground = Ground.SALT, groundTop = Color(0xFFE6C6A8), groundBottom = Color(0xFF5A7C86),
        foreground = Foreground.POSTS, foregroundColor = Color(0xFF1A1E24),
    )
    8 -> SceneRecipe(
        sky = listOf(Color(0xFF261A28), Color(0xFF6A3A2A), Color(0xFFC0703A), Color(0xFFE8B070)),
        celestial = Celestial.SUN, celestialX = 176f, celestialY = 132f, celestialRadius = 18f,
        celestialColor = Color(0xFFFFCE8A),
        horizon = Horizon.ROUND_TREES, horizonColor = Color(0xFF3A1E14),
        ground = Ground.LEAF_LITTER, groundTop = Color(0xFF7A4020), groundBottom = Color(0xFF241008),
        foreground = Foreground.BUSHES, foregroundColor = Color(0xFF1A0C06),
        fallingLeaves = true,
    )
    9 -> SceneRecipe(
        sky = listOf(Color(0xFF05060F), Color(0xFF0E1430), Color(0xFF1C2448), Color(0xFF2E3460)),
        celestial = Celestial.MOON, celestialX = 176f, celestialY = 70f, celestialRadius = 16f,
        celestialColor = Color(0xFFF0EEDC),
        horizon = Horizon.HEATH, horizonColor = Color(0xFF0A0C18),
        ground = Ground.HEATH, groundTop = Color(0xFF2A1C3A), groundBottom = Color(0xFF0A0812),
        foreground = Foreground.HEATHER, foregroundColor = Color(0xFF060410),
        stars = true, fireflies = true,
    )
    10 -> SceneRecipe(
        sky = listOf(Color(0xFF14181C), Color(0xFF2A3236), Color(0xFF46524E), Color(0xFF6A7468)),
        celestial = Celestial.NONE, celestialX = 0f, celestialY = 0f, celestialRadius = 0f,
        celestialColor = Color.Transparent,
        horizon = Horizon.REEDBED, horizonColor = Color(0xFF101414),
        ground = Ground.WATER, groundTop = Color(0xFF34403C), groundBottom = Color(0xFF0C1010),
        foreground = Foreground.REEDS, foregroundColor = Color(0xFF060808),
        clouds = Color(0xFF3A4044),
    )
    11 -> SceneRecipe(
        sky = listOf(Color(0xFF040812), Color(0xFF0A1830), Color(0xFF14304A), Color(0xFF1E3E58)),
        celestial = Celestial.NONE, celestialX = 0f, celestialY = 0f, celestialRadius = 0f,
        celestialColor = Color.Transparent,
        horizon = Horizon.FJORD, horizonColor = Color(0xFF060A14),
        ground = Ground.WATER, groundTop = Color(0xFF1A3448), groundBottom = Color(0xFF040810),
        foreground = Foreground.SNOWY_ROCKS, foregroundColor = Color(0xFF080C14),
        stars = true, aurora = true,
    )
    12 -> SceneRecipe(
        sky = listOf(Color(0xFF0A0618), Color(0xFF2A1450), Color(0xFF5A2A6A), Color(0xFF1E5A6A)),
        celestial = Celestial.MOON, celestialX = 176f, celestialY = 64f, celestialRadius = 16f,
        celestialColor = Color(0xFFFFF1C8),
        horizon = Horizon.NEST, horizonColor = Color(0xFF0C0818),
        ground = Ground.WATER, groundTop = Color(0xFF3A2A5A), groundBottom = Color(0xFF080612),
        foreground = Foreground.REEDS, foregroundColor = Color(0xFF05030A),
        stars = true, fireflies = true,
    )
    else -> SceneRecipe(
        sky = listOf(Color(0xFF211433), Color(0xFF5E2A5C), Color(0xFFC9584A), Color(0xFFF3A35C)),
        celestial = Celestial.SUN, celestialX = 182f, celestialY = 92f, celestialRadius = 22f,
        celestialColor = Color(0xFFFFC478),
        horizon = Horizon.TREES, horizonColor = Color(0xFF1A1028),
        ground = Ground.WATER, groundTop = Color(0xFF4A2440), groundBottom = Color(0xFF120A1C),
        foreground = Foreground.REEDS, foregroundColor = BankColor,
    )
}

/** Sky, sun or moon, horizon and ground: everything behind the birds. */
fun DrawScope.drawSceneBack(recipe: SceneRecipe, sx: Float, sy: Float, tick: Int, still: Boolean) {
    val sky = recipe.sky
    drawRect(
        brush = Brush.verticalGradient(
            colorStops = arrayOf(0f to sky[0], 0.3f to sky[1], 0.5f to sky[2], 0.6f to sky[3]),
            endY = size.height,
        ),
    )
    if (recipe.stars) {
        for (index in 0 until 40) {
            val x = (index * 53 % 240) * sx
            val y = (index * 29 % 150) * sy
            val twinkle = if (still) 0.6f else 0.4f + 0.4f * ((tick / 20 + index) % 3) / 2f
            drawCircle(Color.White.copy(alpha = twinkle), radius = max(0.8f, 0.45f * sx), center = Offset(x, y))
        }
    }
    if (recipe.aurora) {
        for (band in 0 until 3) {
            val shift = if (still) 0f else (tick + band * 90) / 60f
            val path = Path().apply {
                moveTo(0f, (60 + band * 18) * sy)
                var x = 0f
                while (x <= 240f) {
                    lineTo(x * sx, (60 + band * 18 + 10 * sin(x / 30f + shift)) * sy)
                    x += 10f
                }
            }
            drawPath(
                path,
                (if (band == 1) Color(0xFF9A6AE0) else Color(0xFF5AE0A0)).copy(alpha = 0.32f),
                style = Stroke(width = 14f * sy),
            )
        }
    }
    when (recipe.celestial) {
        Celestial.SUN, Celestial.PALE_SUN -> {
            val center = Offset(recipe.celestialX * sx, recipe.celestialY * sy)
            val glow = if (recipe.celestial == Celestial.SUN) 0.2f else 0.1f
            drawCircle(recipe.celestialColor.copy(alpha = glow), radius = recipe.celestialRadius * 1.6f * sx, center = center)
            drawCircle(
                recipe.celestialColor.copy(alpha = if (recipe.celestial == Celestial.SUN) 1f else 0.7f),
                radius = recipe.celestialRadius * sx,
                center = center,
            )
        }
        Celestial.MOON -> {
            val center = Offset(recipe.celestialX * sx, recipe.celestialY * sy)
            drawCircle(recipe.celestialColor.copy(alpha = 0.1f), radius = recipe.celestialRadius * 1.5f * sx, center = center)
            drawCircle(recipe.celestialColor, radius = recipe.celestialRadius * sx, center = center)
            drawCircle(
                recipe.celestialColor.copy(alpha = 0.18f),
                radius = recipe.celestialRadius * 0.3f * sx,
                center = Offset(center.x - recipe.celestialRadius * 0.3f * sx, center.y - recipe.celestialRadius * 0.2f * sx),
            )
        }
        Celestial.NONE -> Unit
    }
    recipe.clouds?.let { cloud ->
        for (index in 0 until 4) {
            val drift = if (still) 0f else tick * (0.08f + index * 0.03f)
            val x = ((index * 70 + drift) % 300f) - 30f
            val y = 40f + index * 22f
            drawOval(cloud.copy(alpha = 0.55f), topLeft = Offset(x * sx, y * sy), size = Size(70f * sx, 14f * sy))
            drawOval(cloud.copy(alpha = 0.45f), topLeft = Offset((x + 18f) * sx, (y - 7f) * sy), size = Size(40f * sx, 14f * sy))
        }
    }
    drawHorizon(recipe, sx, sy)
    drawGround(recipe, sx, sy, tick, still)
}

private fun DrawScope.drawHorizon(recipe: SceneRecipe, sx: Float, sy: Float) {
    val color = recipe.horizonColor
    val base = 178f
    val path = Path().apply {
        moveTo(0f, (base + 10) * sy)
        when (recipe.horizon) {
            Horizon.TREES, Horizon.PINES -> {
                val tall = if (recipe.horizon == Horizon.PINES) 26f else 14f
                var x = 0f
                var index = 0
                while (x < 240f) {
                    val step = if (recipe.horizon == Horizon.PINES) 12f else 18f
                    lineTo((x + step * 0.5f) * sx, (base - tall + (index % 4) * 5f) * sy)
                    lineTo((x + step) * sx, base * sy)
                    x += step
                    index++
                }
            }
            Horizon.HILLS -> {
                var x = 0f
                while (x <= 240f) {
                    lineTo(x * sx, (base - 14f - 10f * sin(x / 38f) - 5f * sin(x / 13f)) * sy)
                    x += 6f
                }
            }
            Horizon.REEDBED -> {
                var x = 0f
                var index = 0
                while (x < 240f) {
                    lineTo(x * sx, (base - 4f - (index % 5) * 3f) * sy)
                    lineTo((x + 3f) * sx, (base + 2f) * sy)
                    x += 6f
                    index++
                }
            }
            Horizon.SNOWY_RIDGE, Horizon.FJORD -> {
                val peaks = if (recipe.horizon == Horizon.FJORD) {
                    listOf(0f to 90f, 40f to 150f, 70f to 120f, 170f to 120f, 200f to 80f, 240f to 110f)
                } else {
                    listOf(0f to 150f, 40f to 120f, 80f to 150f, 130f to 104f, 180f to 146f, 210f to 128f, 240f to 150f)
                }
                for ((x, y) in peaks) lineTo(x * sx, y * sy)
            }
            Horizon.FLAT -> lineTo(240f * sx, (base + 6f) * sy)
            Horizon.ROUND_TREES, Horizon.NEST -> {
                var x = -6f
                var index = 0
                while (x < 246f) {
                    val radius = 12f + (index % 3) * 4f
                    cubicTo(
                        x * sx, (base - radius * 2f) * sy,
                        (x + radius * 2f) * sx, (base - radius * 2f) * sy,
                        (x + radius * 2f) * sx, base * sy,
                    )
                    x += radius * 2f - 4f
                    index++
                }
            }
            Horizon.HEATH -> {
                var x = 0f
                while (x <= 240f) {
                    lineTo(x * sx, (base - 4f - 4f * sin(x / 22f)) * sy)
                    x += 8f
                }
            }
        }
        lineTo(240f * sx, (base + 10) * sy)
        lineTo(240f * sx, 300f * sy)
        lineTo(0f, 300f * sy)
        close()
    }
    drawPath(path, color)
    if (recipe.horizon == Horizon.SNOWY_RIDGE) {
        // Snow caps on the peaks.
        for ((x, y) in listOf(40f to 120f, 130f to 104f, 210f to 128f)) {
            val cap = Path().apply {
                moveTo((x - 12f) * sx, (y + 10f) * sy)
                lineTo(x * sx, y * sy)
                lineTo((x + 12f) * sx, (y + 10f) * sy)
                close()
            }
            drawPath(cap, Color(0xFFE8ECF6))
        }
    }
    if (recipe.horizon == Horizon.NEST) {
        // The great old tree that holds the nest: a leaning trunk, bare branches and a twig nest.
        val trunk = Path().apply {
            moveTo(106f * sx, 182f * sy)
            cubicTo(110f * sx, 160f * sy, 112f * sx, 140f * sy, 116f * sx, 118f * sy)
            lineTo(124f * sx, 118f * sy)
            cubicTo(126f * sx, 140f * sy, 128f * sx, 160f * sy, 136f * sx, 182f * sy)
            close()
        }
        drawPath(trunk, color)
        val branch = Stroke(width = 3f * sx)
        for ((from, to) in listOf(
            Offset(118f, 128f) to Offset(92f, 104f),
            Offset(92f, 104f) to Offset(80f, 108f),
            Offset(120f, 122f) to Offset(146f, 100f),
            Offset(146f, 100f) to Offset(160f, 104f),
            Offset(120f, 120f) to Offset(118f, 98f),
        )) {
            drawLine(color, Offset(from.x * sx, from.y * sy), Offset(to.x * sx, to.y * sy), strokeWidth = branch.width)
        }
        // The nest: a wide bowl of twigs with a few sticking out.
        drawOval(Color(0xFF2A1A10), topLeft = Offset(98f * sx, 112f * sy), size = Size(44f * sx, 12f * sy))
        for (twig in 0 until 9) {
            val x = 100f + twig * 5f
            drawLine(
                Color(0xFF4A3220),
                Offset(x * sx, (114f + twig % 3 * 3f) * sy),
                Offset((x + 7f) * sx, (110f + twig % 2 * 6f) * sy),
                strokeWidth = max(1f, 0.8f * sx),
            )
        }
    }
}

private fun DrawScope.drawGround(recipe: SceneRecipe, sx: Float, sy: Float, tick: Int, still: Boolean) {
    drawRect(
        brush = Brush.verticalGradient(listOf(recipe.groundTop, recipe.groundBottom), startY = 184f * sy, endY = 262f * sy),
        topLeft = Offset(0f, 184f * sy),
        size = Size(size.width, 78f * sy),
    )
    when (recipe.ground) {
        Ground.WATER, Ground.SALT -> {
            // Light on the water, under the sun or the moon when there is one.
            val light = recipe.celestialColor.takeIf { recipe.celestial != Celestial.NONE } ?: Color.White
            val center = if (recipe.celestial != Celestial.NONE) recipe.celestialX else 120f
            val shimmer = if (still) 0 else (tick / 12) % 3
            for (i in 1..7) {
                val half = 40f - i * 3f + shimmer
                drawLine(
                    light.copy(alpha = if (recipe.ground == Ground.SALT) 0.35f else 0.18f),
                    Offset((center - half) * sx, (190 + i * 6) * sy),
                    Offset((center + half) * sx, (190 + i * 6) * sy),
                    strokeWidth = max(1f, sx * 0.7f),
                )
            }
        }
        Ground.FIELD -> {
            for (row in 0 until 6) {
                drawLine(
                    recipe.groundBottom.copy(alpha = 0.35f),
                    Offset(0f, (192 + row * 10) * sy),
                    Offset(size.width, (189 + row * 10) * sy),
                    strokeWidth = max(1f, sx * 0.6f),
                )
            }
        }
        Ground.SNOW -> {
            for (index in 0 until 8) {
                drawOval(
                    Color.White.copy(alpha = 0.25f),
                    topLeft = Offset((index * 31 % 240) * sx, (196 + index * 7 % 40) * sy),
                    size = Size(26f * sx, 4f * sy),
                )
            }
        }
        Ground.LEAF_LITTER -> {
            for (index in 0 until 30) {
                drawCircle(
                    listOf(Color(0xFFC0602A), Color(0xFFE09A3A), Color(0xFF8A3418))[index % 3].copy(alpha = 0.7f),
                    radius = 1.6f * sx,
                    center = Offset((index * 37 % 240) * sx, (190 + index * 13 % 60) * sy),
                )
            }
        }
        Ground.HEATH -> Unit
    }
}

/** The vegetation in front of the birds, and the bank under the dog. */
fun DrawScope.drawSceneFront(recipe: SceneRecipe, sx: Float, sy: Float, tick: Int, still: Boolean, wind: Int) {
    val bank = Path().apply {
        moveTo(0f, 252f * sy)
        lineTo(size.width, 238f * sy)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(bank, BankColor)
    val color = recipe.foregroundColor
    val sway = if (still) 0f else sin(tick / 25f) * 1.2f + wind.coerceIn(-1, 1) * 2.5f
    when (recipe.foreground) {
        Foreground.REEDS -> repeat(22) { index ->
            val baseX = (index * 11 + (index % 3) * 4).toFloat()
            val baseY = 250f - index * 0.6f
            val height = 18f + (index * 7 % 5) * 5f
            val tip = Offset((baseX + ((index % 5) - 2) * 1.6f + sway) * sx, (baseY - height) * sy)
            drawLine(color, Offset(baseX * sx, (baseY + 4) * sy), tip, strokeWidth = max(1.5f, 1.3f * sx))
            if (index % 3 == 0) {
                drawLine(color, tip.copy(y = tip.y + 2 * sy), tip.copy(y = tip.y + 9 * sy), strokeWidth = max(3f, 3f * sx))
            }
        }
        Foreground.WHEAT -> repeat(34) { index ->
            val baseX = (index * 7 + (index % 2) * 3).toFloat()
            val baseY = 252f - index * 0.4f
            val height = 22f + (index * 5 % 4) * 4f
            val tip = Offset((baseX + sway * 1.4f) * sx, (baseY - height) * sy)
            drawLine(color, Offset(baseX * sx, baseY * sy), tip, strokeWidth = max(1f, 0.9f * sx))
            drawOval(Color(0xFF6A4A20), topLeft = Offset(tip.x - 1.6f * sx, tip.y - 2f * sy), size = Size(3.2f * sx, 8f * sy))
        }
        Foreground.GRASS -> repeat(40) { index ->
            val baseX = (index * 6).toFloat()
            val baseY = 252f - index * 0.35f
            val height = 8f + (index * 3 % 5) * 3f
            drawLine(
                color,
                Offset(baseX * sx, baseY * sy),
                Offset((baseX + 2f + sway) * sx, (baseY - height) * sy),
                strokeWidth = max(1f, 1f * sx),
            )
        }
        Foreground.SNOWY_ROCKS -> for ((x, w) in listOf(10f to 40f, 70f to 28f, 150f to 46f, 205f to 30f)) {
            drawOval(color, topLeft = Offset(x * sx, (238f) * sy), size = Size(w * sx, 20f * sy))
            drawOval(Color(0xFFDDE4F0).copy(alpha = 0.8f), topLeft = Offset((x + 4f) * sx, 236f * sy), size = Size((w - 8f) * sx, 6f * sy))
        }
        Foreground.POSTS -> for (x in listOf(20f, 64f, 108f, 152f, 196f)) {
            drawRect(color, topLeft = Offset(x * sx, 226f * sy), size = Size(3.5f * sx, 22f * sy))
            drawLine(color.copy(alpha = 0.6f), Offset(x * sx, 232f * sy), Offset((x + 44f) * sx, 232f * sy), strokeWidth = max(1f, 0.6f * sx))
        }
        Foreground.BUSHES -> for (index in 0 until 7) {
            val x = index * 36f - 6f
            drawOval(color, topLeft = Offset(x * sx, (232f + index % 2 * 4f) * sy), size = Size(44f * sx, 26f * sy))
        }
        Foreground.HEATHER -> for (index in 0 until 16) {
            val x = index * 15f
            drawOval(color, topLeft = Offset(x * sx, (240f - index % 3 * 3f) * sy), size = Size(18f * sx, 12f * sy))
            drawCircle(Color(0xFF6A3A7A).copy(alpha = 0.7f), radius = 1.4f * sx, center = Offset((x + 9f) * sx, (241f - index % 3 * 3f) * sy))
        }
    }
    if (recipe.fallingLeaves) {
        for (index in 0 until 14) {
            val fall = if (still) 0f else tick * (0.25f + index % 3 * 0.08f)
            val y = (index * 41 + fall) % 300f
            val x = (index * 67 % 240) + if (still) 0f else sin((tick + index * 20) / 30f) * 6f
            drawOval(
                listOf(Color(0xFFD0702A), Color(0xFFE8A040), Color(0xFF9A3A1A))[index % 3],
                topLeft = Offset(x * sx, y * sy),
                size = Size(3.5f * sx, 2.2f * sy),
            )
        }
    }
}

/** The fair stall for the clay levels and the free clay shoot. */
fun DrawScope.drawFairStall(sx: Float, sy: Float, tick: Int, still: Boolean) {
    val stripes = 12
    val width = 240f / stripes
    for (index in 0 until stripes) {
        drawRect(
            if (index % 2 == 0) Color(0xFFD9483C) else Color(0xFFF4E9D4),
            topLeft = Offset(index * width * sx, 0f),
            size = Size(width * sx, 22f * sy),
        )
        val scallop = Path().apply {
            moveTo(index * width * sx, 22f * sy)
            quadraticTo((index + 0.5f) * width * sx, 30f * sy, (index + 1) * width * sx, 22f * sy)
            close()
        }
        drawPath(scallop, if (index % 2 == 0) Color(0xFFD9483C) else Color(0xFFF4E9D4))
    }
    // String lights across the sky.
    val lights = Path().apply {
        moveTo(0f, 40f * sy)
        quadraticTo(120f * sx, 64f * sy, 240f * sx, 40f * sy)
    }
    drawPath(lights, Color(0xFF2A1810), style = Stroke(width = max(1f, 0.6f * sx)))
    for (index in 1 until 12) {
        val t = index / 12f
        val x = 240f * t
        val y = 40f + 48f * t * (1 - t)
        val on = still || (tick / 15 + index) % 4 != 0
        drawCircle(
            listOf(Color(0xFFFFD66B), Color(0xFFFF8A5B), Color(0xFF8FD0FF))[index % 3].copy(alpha = if (on) 1f else 0.4f),
            radius = 2.2f * sx,
            center = Offset(x * sx, y * sy),
        )
    }
    // Wooden posts and the counter.
    for (x in listOf(4f, 228f)) {
        drawRect(Color(0xFF5A3418), topLeft = Offset(x * sx, 22f * sy), size = Size(8f * sx, 230f * sy))
    }
    drawRect(Color(0xFF6A3E1E), topLeft = Offset(0f, 262f * sy), size = Size(size.width, 38f * sy))
    drawRect(Color(0xFF8A5428), topLeft = Offset(0f, 258f * sy), size = Size(size.width, 6f * sy))
}

/**
 * The weather on top of the birds: wind, fog, cover, snow, rain and the night
 * with its lamp. It only draws what the engine already decides, so it never changes the play.
 */
fun DrawScope.drawWeather(state: GameState, sx: Float, sy: Float, still: Boolean) {
    val modifiers = state.spec.modifiers
    val extras = state.extras
    val tick = state.tick
    if (Modifier.WIND in modifiers) {
        val direction = if ((extras?.windFx ?: 1) >= 0) 1f else -1f
        for (index in 0 until 12) {
            val travel = if (still) 0f else tick * 3.2f * direction
            val x = ((index * 47 + travel) % 280f + 280f) % 280f - 20f
            val y = 30f + index * 17f
            drawLine(
                Color.White.copy(alpha = 0.22f),
                Offset(x * sx, y * sy),
                Offset((x + 16f * direction) * sx, y * sy),
                strokeWidth = max(1f, 0.6f * sx),
            )
        }
    }
    if (extras != null && Modifier.FOG in modifiers) {
        for ((index, bank) in extras.fogBanks.withIndex()) {
            val top = bank.y * sy
            val height = bank.height * sy
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color(0xFFDDE4EC).copy(alpha = 0f),
                    0.3f to Color(0xFFDDE4EC).copy(alpha = 0.42f),
                    0.7f to Color(0xFFDDE4EC).copy(alpha = 0.42f),
                    1f to Color(0xFFDDE4EC).copy(alpha = 0f),
                    startY = top - 6f * sy,
                    endY = top + height + 6f * sy,
                ),
                topLeft = Offset(0f, top - 6f * sy),
                size = Size(size.width, height + 12f * sy),
            )
            for (puff in 0 until 4) {
                val drift = if (still) 0f else tick * (0.15f + index * 0.05f)
                val x = ((puff * 70 + drift) % 300f) - 30f
                drawOval(
                    Color(0xFFEFF3F7).copy(alpha = 0.22f),
                    topLeft = Offset(x * sx, top + (puff % 2) * height * 0.3f),
                    size = Size(60f * sx, height * 0.6f),
                )
            }
        }
    }
    if (extras != null && Modifier.COVER in modifiers) {
        for (box in extras.coverBoxes) {
            val trunkX = (box.x + box.width / 2f - 3f) * sx
            drawRect(Color(0xFF2A160C), topLeft = Offset(trunkX, (box.y + box.height * 0.5f) * sy), size = Size(6f * sx, box.height * 0.6f * sy))
            for (blob in 0 until 4) {
                drawOval(
                    listOf(Color(0xFF8A3A16), Color(0xFFB85A20), Color(0xFF6A2A10), Color(0xFFC8782A))[blob],
                    topLeft = Offset((box.x - 6f + blob * box.width / 4f) * sx, (box.y - 6f + (blob % 2) * 10f) * sy),
                    size = Size((box.width / 2f + 6f) * sx, (box.height * 0.6f) * sy),
                )
            }
        }
    }
    if (Modifier.SNOW in modifiers) {
        for (index in 0 until 60) {
            val fall = if (still) 0f else tick * (0.35f + index % 4 * 0.1f)
            val y = (index * 23 + fall) % 300f
            val x = (index * 41 % 240) + if (still) 0f else sin((tick + index * 13) / 40f) * 3f
            drawCircle(Color.White.copy(alpha = 0.85f), radius = (0.7f + index % 3 * 0.35f) * sx, center = Offset(x * sx, y * sy))
        }
    }
    if (Modifier.RAIN in modifiers) {
        for (index in 0 until 60) {
            val seed = (index * 2_654_435_761L and 0xFFFF).toInt()
            val fall = if (still) 0f else tick * (5f + seed % 3)
            val y = (seed % 320 + fall) % 320f - 20f
            val x = (seed * 7 % 251).toFloat()
            drawLine(
                Color(0xFFBFD0E0).copy(alpha = 0.45f),
                Offset(x * sx, y * sy),
                Offset((x - 3f) * sx, (y + 11f) * sy),
                strokeWidth = max(1f, 0.5f * sx),
            )
        }
        // A soft flash at most once every 4 s, never a white screen.
        if (!still && tick % 300 in 0..6) drawRect(Color.White.copy(alpha = 0.15f))
    }
    // Ripples where a diving bird is under the water.
    for (bird in state.birds) {
        if (bird.status == BirdStatus.FLYING && bird.diveUntilTick > tick && bird.bossPhase == 0) {
            drawRipple(bird.x / 256f, bird.y / 256f, sx, sy, tick, still)
        }
    }
    if (extras != null && Modifier.NIGHT in modifiers) {
        val center = Offset(extras.nightX * sx, extras.nightY * sy)
        val radius = NIGHT_LAMP_RADIUS * sx
        drawRect(
            brush = Brush.radialGradient(
                0f to Color(0xFF05040C).copy(alpha = 0f),
                ((NIGHT_LAMP_RADIUS - 10f) / NIGHT_LAMP_RADIUS) to Color(0xFF05040C).copy(alpha = 0f),
                1f to Color(0xFF05040C).copy(alpha = 0.88f),
                center = center,
                radius = radius,
            ),
        )
        drawCircle(Color(0xFFFFE8B0).copy(alpha = 0.25f), radius = radius, center = center, style = Stroke(width = max(1f, 0.6f * sx)))
        // The moon still shines through the dark: it lights nothing, but it is there.
        val recipe = sceneFor(state.spec.place)
        if (recipe.celestial == Celestial.MOON) {
            drawCircle(
                recipe.celestialColor.copy(alpha = 0.85f),
                radius = recipe.celestialRadius * sx,
                center = Offset(recipe.celestialX * sx, recipe.celestialY * sy),
            )
        }
    }
}

/** Night darkens everything around the field, so the edges match it. */
val NightEdge = Color(0xFF05040C)

/** World radius of the night lamp, the same as the engine's. */
private const val NIGHT_LAMP_RADIUS = 46f

fun DrawScope.drawRipple(x: Float, y: Float, sx: Float, sy: Float, tick: Int, still: Boolean) {
    for (ring in 0 until 2) {
        val grow = if (still) 6f else ((tick + ring * 15) % 30) / 30f * 10f
        drawOval(
            Color.White.copy(alpha = 0.4f * (1f - grow / 12f)),
            topLeft = Offset((x - 4f - grow) * sx, (y - 1.5f - grow * 0.3f) * sy),
            size = Size((8f + grow * 2f) * sx, (3f + grow * 0.6f) * sy),
            style = Stroke(width = max(1f, 0.6f * sx)),
        )
    }
}

/** Fireflies over the heath at night: small, slow and never flashing. */
fun DrawScope.drawFireflies(sx: Float, sy: Float, tick: Int, still: Boolean) {
    for (index in 0 until 10) {
        val x = (index * 29 % 220 + 10) + if (still) 0f else sin((tick + index * 37) / 50f) * 8f
        val y = (150 + index * 17 % 90) + if (still) 0f else sin((tick + index * 23) / 70f) * 5f
        drawCircle(Color(0xFFE8FF8A).copy(alpha = 0.65f), radius = 1.1f * sx, center = Offset(x * sx, y * sy))
    }
}

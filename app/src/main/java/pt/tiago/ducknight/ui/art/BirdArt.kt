package pt.tiago.ducknight.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import pt.tiago.ducknight.core.species.SpeciesCategory

/** Dark plumage shared by every bird; the species color is mixed into it. */
val BirdShadow = Color(0xFF1B1028)

private val Signature = mapOf(
    "mallard" to Color(0xFF76A35A),
    "teal" to Color(0xFFC47A54),
    "gadwall" to Color(0xFFC9B17A),
    "wigeon" to Color(0xFFD69A63),
    "golden" to Color(0xFFFFD66B),
    "great_duck" to Color(0xFFFFD66B),
    "old_mallard" to Color(0xFF5FA05A),
    "twin_teal" to Color(0xFFD0805A),
    "wind_goose" to Color(0xFFB8C4D0),
    "mist_goose" to Color(0xFFE2E8EE),
    "thief_crow" to Color(0xFF8A8AA0),
    "royal_snow_goose" to Color(0xFFF6F8FF),
    "royal_grebe" to Color(0xFFD86A3A),
    "imperial_mandarin" to Color(0xFFF08A2A),
    "ghost_duck" to Color(0xFFBFE6FF),
    "thunder_flock" to Color(0xFF9AA8B8),
    "golden_eider" to Color(0xFFFFC94A),
)

private val Palette = listOf(
    Color(0xFF7FB069), Color(0xFFD08C60), Color(0xFFB5A27A), Color(0xFF8FB6C9), Color(0xFFC97B8E),
    Color(0xFF9E8FC9), Color(0xFFE0B25A), Color(0xFF6FA8A0), Color(0xFFB9845C), Color(0xFFA7C4E0),
)

/** A stable color per species: the signature ones first, then the category, then the id. */
fun speciesColor(id: String, category: SpeciesCategory? = null): Color =
    Signature[id] ?: when (category) {
        SpeciesCategory.WADER_PROTECTED -> Color(0xFFE8EEF2)
        SpeciesCategory.LEGENDARY -> Color(0xFFFFD66B)
        else -> Palette[(id.hashCode() and Int.MAX_VALUE) % Palette.size]
    }

/**
 * The flying bird: body, head, beak and a two-frame wing around [center], [unit] being the radius.
 * The body mixes the species color into the dusk shadow so it reads against the tree line too.
 */
fun DrawScope.drawBirdFigure(
    center: Offset,
    unit: Float,
    accent: Color,
    wingUp: Boolean,
    alpha: Float = 1f,
    stroke: Float = unit * 0.09f,
    highContrast: Boolean = false,
) {
    val cx = center.x
    val cy = center.y
    val fill = lerp(BirdShadow, accent, 0.38f).copy(alpha = alpha)
    val line = accent.copy(alpha = alpha)
    val body = Path().apply {
        moveTo(cx - unit * 0.75f, cy + unit * 0.1f)
        cubicTo(cx - unit * 0.9f, cy - unit * 0.25f, cx - unit * 0.45f, cy - unit * 0.5f, cx, cy - unit * 0.42f)
        cubicTo(cx + unit * 0.35f, cy - unit * 0.35f, cx + unit * 0.55f, cy - unit * 0.1f, cx + unit * 0.5f, cy + unit * 0.15f)
        cubicTo(cx + unit * 0.4f, cy + unit * 0.45f, cx - unit * 0.2f, cy + unit * 0.5f, cx - unit * 0.55f, cy + unit * 0.32f)
        close()
    }
    drawPath(body, fill)
    drawPath(body, line, style = Stroke(width = stroke))
    if (highContrast) {
        drawPath(body, Color.White.copy(alpha = alpha), style = Stroke(width = stroke * 3f))
        drawPath(body, Color.Black.copy(alpha = alpha), style = Stroke(width = stroke * 1.5f))
    }
    val head = Offset(cx + unit * 0.42f, cy - unit * 0.42f)
    drawCircle(fill, radius = unit * 0.22f, center = head)
    drawCircle(line, radius = unit * 0.22f, center = head, style = Stroke(width = stroke))
    val beak = Path().apply {
        moveTo(head.x + unit * 0.18f, head.y - unit * 0.02f)
        lineTo(head.x + unit * 0.48f, head.y + unit * 0.05f)
        lineTo(head.x + unit * 0.18f, head.y + unit * 0.14f)
        close()
    }
    drawPath(beak, line)
    val wing = Path().apply {
        moveTo(cx - unit * 0.25f, cy)
        if (wingUp) {
            cubicTo(cx - unit * 0.05f, cy - unit * 0.65f, cx + unit * 0.35f, cy - unit * 0.55f, cx + unit * 0.28f, cy - unit * 0.02f)
            cubicTo(cx + unit * 0.05f, cy + unit * 0.18f, cx - unit * 0.12f, cy + unit * 0.16f, cx - unit * 0.25f, cy)
        } else {
            cubicTo(cx - unit * 0.08f, cy + unit * 0.32f, cx + unit * 0.3f, cy + unit * 0.3f, cx + unit * 0.28f, cy - unit * 0.02f)
            cubicTo(cx + unit * 0.05f, cy + unit * 0.08f, cx - unit * 0.12f, cy + unit * 0.06f, cx - unit * 0.25f, cy)
        }
        close()
    }
    drawPath(wing, lerp(BirdShadow, accent, 0.6f).copy(alpha = alpha))
    drawPath(wing, line, style = Stroke(width = stroke))
}

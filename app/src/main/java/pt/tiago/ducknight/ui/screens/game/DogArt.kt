package pt.tiago.ducknight.ui.screens.game

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.max
import kotlin.math.sin

/** The three Portuguese dogs: coat colors and ear shape only, never the gameplay. */
private class DogLook(
    val coat: Color,
    val patch: Color,
    val ear: Color,
    val outline: Color,
    val erectEars: Boolean,
    val curly: Boolean,
)

private fun lookFor(dogId: String): DogLook = when (dogId) {
    // Cão-d'água português: black, curly coat.
    "pantufa" -> DogLook(Color(0xFF26262C), Color(0xFF26262C), Color(0xFF1A1A1F), Color(0xFF4A4A54), erectEars = false, curly = true)
    // Podengo português: fawn, ears up.
    "tejo" -> DogLook(Color(0xFFC98A4A), Color(0xFFE3B47A), Color(0xFFB0763C), Color(0xFF5A3418), erectEars = true, curly = false)
    // Perdigueiro português: white with liver patches and long, soft liver ears.
    else -> DogLook(Color(0xFFF4E9D4), Color(0xFF8B4F32), Color(0xFF7A4028), Color(0xFF3A2414), erectEars = false, curly = false)
}

enum class DogPose { SIT, PHOTO, LAUGH, SHRUG }

/**
 * A sitting dog seen from the side, facing right, rising from behind the bank. [cx] and [base] are
 * in pixels; [u] is one world unit in pixels, so the dog keeps its size on every screen.
 */
fun DrawScope.drawDogFigure(dogId: String, pose: DogPose, cx: Float, base: Float, u: Float, tick: Int, still: Boolean) {
    val look = lookFor(dogId)
    // Laughing makes it bounce 1.5 units at about 11 Hz.
    val hop = if (pose == DogPose.LAUGH && !still) sin(tick * 1.15f) * 1.5f * u else 0f
    val y0 = base + hop
    fun p(x: Float, y: Float) = Offset(cx + x * u, y0 + y * u)
    val line = Stroke(width = max(1f, 0.7f * u))

    // Tail, behind the body: a curve up from the back that wags while the dog waits.
    val wag = if (pose == DogPose.SIT && !still) sin(tick * 0.35f) * 2f else 0f
    val tail = Path().apply {
        moveTo(p(-9f, -6f).x, p(-9f, -6f).y)
        quadraticTo(p(-15f, -9f).x, p(-15f, -9f).y, p(-14f + wag, if (pose == DogPose.LAUGH) -18f else -15f).x, p(-14f + wag, if (pose == DogPose.LAUGH) -18f else -15f).y)
    }
    drawPath(tail, look.coat, style = Stroke(width = 2.2f * u))
    drawPath(tail, look.outline, style = Stroke(width = max(1f, 0.4f * u)))

    // Sitting body: haunch at the back, chest up front.
    val body = Path().apply {
        moveTo(p(-10f, 2f).x, p(-10f, 2f).y)
        cubicTo(p(-13f, -6f).x, p(-13f, -6f).y, p(-8f, -14f).x, p(-8f, -14f).y, p(-1f, -15f).x, p(-1f, -15f).y)
        cubicTo(p(4f, -16f).x, p(4f, -16f).y, p(7f, -12f).x, p(7f, -12f).y, p(6f, -4f).x, p(6f, -4f).y)
        lineTo(p(6f, 2f).x, p(6f, 2f).y)
        close()
    }
    drawPath(body, look.coat)
    if (look.patch != look.coat) {
        drawOval(look.patch, topLeft = p(-9f, -11f), size = Size(8f * u, 7f * u))
        drawOval(look.patch, topLeft = p(-4f, -4f), size = Size(4f * u, 3f * u))
    }
    drawPath(body, look.outline, style = line)
    if (look.curly) {
        for (i in 0 until 9) {
            drawCircle(look.outline, radius = 1f * u, center = p(-11f + i * 2f, -13f + (i % 2) * 1.2f + if (i > 5) (i - 5) * 1.5f else 0f), style = line)
        }
    }

    // Front leg and paw.
    drawRect(look.coat, topLeft = p(1.5f, -6f), size = Size(3f * u, 8f * u))
    drawRect(look.outline, topLeft = p(1.5f, -6f), size = Size(3f * u, 8f * u), style = line)
    drawOval(look.coat, topLeft = p(1f, 0.5f), size = Size(5f * u, 2.5f * u))
    drawOval(look.outline, topLeft = p(1f, 0.5f), size = Size(5f * u, 2.5f * u), style = line)

    // Neck and head; shaking the head tilts it a little.
    val tilt = if (pose == DogPose.SHRUG && !still) sin(tick * 0.6f) * 1.2f else 0f
    val neck = Path().apply {
        moveTo(p(0f, -14f).x, p(0f, -14f).y)
        lineTo(p(4f, -21f).x, p(4f, -21f).y)
        lineTo(p(9f, -19f).x, p(9f, -19f).y)
        lineTo(p(6.5f, -12f).x, p(6.5f, -12f).y)
        close()
    }
    drawPath(neck, look.coat)
    val head = p(6f + tilt, -23f)
    drawCircle(look.coat, radius = 5.5f * u, center = head)
    drawCircle(look.outline, radius = 5.5f * u, center = head, style = line)
    // Broad muzzle, the perdigueiro's trademark, with the nose at the tip.
    val muzzle = Offset(head.x + 3f * u, head.y + 0.5f * u)
    drawOval(look.coat, topLeft = muzzle, size = Size(7f * u, 4.6f * u))
    drawOval(look.outline, topLeft = muzzle, size = Size(7f * u, 4.6f * u), style = line)
    drawCircle(DogNoseColor, radius = 1.3f * u, center = Offset(muzzle.x + 6.6f * u, muzzle.y + 1.4f * u))
    if (look.patch != look.coat && !look.erectEars) {
        // A liver patch around the eye.
        drawCircle(look.patch, radius = 2.2f * u, center = Offset(head.x + 0.8f * u, head.y - 1.2f * u))
    }

    // Eye and mouth by pose.
    val eye = Offset(head.x + 1.4f * u, head.y - 1.4f * u)
    when (pose) {
        DogPose.LAUGH -> {
            val arc = Path().apply {
                moveTo(eye.x - 1.2f * u, eye.y + 0.4f * u)
                quadraticTo(eye.x, eye.y - 1.2f * u, eye.x + 1.2f * u, eye.y + 0.4f * u)
            }
            drawPath(arc, DogNoseColor, style = Stroke(width = max(1f, 0.6f * u)))
            val mouth = Path().apply {
                moveTo(muzzle.x + 1f * u, muzzle.y + 4f * u)
                lineTo(muzzle.x + 6f * u, muzzle.y + 4f * u)
                quadraticTo(muzzle.x + 4f * u, muzzle.y + 8f * u, muzzle.x + 1f * u, muzzle.y + 4f * u)
                close()
            }
            drawPath(mouth, Color(0xFF3A1010))
            drawOval(Color(0xFFE07A7A), topLeft = Offset(muzzle.x + 2.2f * u, muzzle.y + 5f * u), size = Size(2.4f * u, 1.8f * u))
        }
        else -> {
            drawCircle(DogNoseColor, radius = 0.85f * u, center = eye)
            drawCircle(Color.White, radius = 0.3f * u, center = Offset(eye.x + 0.3f * u, eye.y - 0.3f * u))
        }
    }

    // Ears last, so they sit over the head.
    if (look.erectEars) {
        val ear = Path().apply {
            moveTo(head.x - 3f * u, head.y - 3f * u)
            lineTo(head.x - 1.5f * u, head.y - 11f * u)
            lineTo(head.x + 1.5f * u, head.y - 4f * u)
            close()
        }
        drawPath(ear, look.ear)
        drawPath(ear, look.outline, style = line)
    } else {
        val swing = if (pose == DogPose.SHRUG && !still) sin(tick * 0.6f) * 1.5f else 0f
        val ear = Path().apply {
            moveTo(head.x - 2.5f * u, head.y - 4f * u)
            cubicTo(
                head.x - 7f * u, head.y - 2f * u,
                head.x - (6f + swing) * u, head.y + 6f * u,
                head.x - (3f + swing) * u, head.y + 7f * u,
            )
            cubicTo(head.x - 1f * u, head.y + 5f * u, head.x, head.y - 1f * u, head.x - 2.5f * u, head.y - 4f * u)
            close()
        }
        drawPath(ear, look.ear)
        drawPath(ear, look.outline, style = line)
    }

    if (pose == DogPose.SHRUG) {
        // Two short marks either side of the head: it shakes it.
        drawLine(DogNoseColor, Offset(head.x - 8f * u, head.y - 2f * u), Offset(head.x - 10f * u, head.y), strokeWidth = max(1f, 0.6f * u))
        drawLine(DogNoseColor, Offset(head.x + 12f * u, head.y - 2f * u), Offset(head.x + 14f * u, head.y), strokeWidth = max(1f, 0.6f * u))
    }
}

/** Where the photo card sits when the dog shows it: held above the head, in front of the muzzle. */
fun dogCardCenter(cx: Float, base: Float, u: Float): Offset = Offset(cx + 10f * u, base - 37f * u)

val DogNoseColor = Color(0xFF2A140C)

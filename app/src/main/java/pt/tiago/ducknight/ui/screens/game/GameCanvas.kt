package pt.tiago.ducknight.ui.screens.game

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import pt.tiago.ducknight.R
import pt.tiago.ducknight.core.fixed.WORLD_HEIGHT
import pt.tiago.ducknight.core.fixed.WORLD_WIDTH
import pt.tiago.ducknight.core.flight.BirdState
import pt.tiago.ducknight.core.flight.BirdStatus
import pt.tiago.ducknight.core.flight.BossStyle
import pt.tiago.ducknight.core.level.LevelType
import pt.tiago.ducknight.core.fair.ClayStatus
import pt.tiago.ducknight.core.replay.FailureTracePoint
import pt.tiago.ducknight.core.replay.ReplayPhoto
import pt.tiago.ducknight.game.GameSceneState
import pt.tiago.ducknight.game.WorldMapping
import pt.tiago.ducknight.ui.art.drawBirdFigure
import pt.tiago.ducknight.ui.art.speciesColor
import pt.tiago.ducknight.ui.theme.LocalDuckColors
import kotlin.math.max
import kotlin.math.min

private val PhotoPaper = Color(0xFFFFF8EE)

/** The playing field: the place's scene, the birds, the weather and the dog, drawn in code. */
@Composable
fun GameCanvas(
    session: GameSceneState,
    dogLaugh: String,
    dogPhoto: String,
    aimPoint: Pair<Int, Int>? = null,
    onTap: ((Int, Int) -> Unit)? = null,
    dogId: String = "farrusco",
    viewfinder: String = "corners",
    highContrast: Boolean = false,
    trace: List<FailureTracePoint> = emptyList(),
    replayPhotos: List<ReplayPhoto> = emptyList(),
    reducedMotion: Boolean = false,
    stolenText: String = "",
    modifier: Modifier = Modifier,
) {
    val colors = LocalDuckColors.current
    val sceneDescription = stringResource(R.string.game_scene_description)
    val paint = remember {
        Paint().apply {
            isAntiAlias = true
            typeface = Typeface.DEFAULT_BOLD
        }
    }
    // The caller rebuilds `onTap` on every tick; keying the gesture on it would restart the
    // detector each frame and drop every tap, so the latest lambda is read at tap time instead.
    val currentOnTap = rememberUpdatedState(onTap)
    val taps = if (onTap != null) {
        Modifier.pointerInput(Unit) {
            detectTapGestures { offset ->
                val x = WorldMapping.worldX(offset.x, size.width.toFloat())
                val y = WorldMapping.worldY(offset.y, size.height.toFloat())
                currentOnTap.value?.invoke(x, y)
            }
        }
    } else {
        Modifier
    }
    Canvas(
        modifier.fillMaxSize().clipToBounds().semantics { contentDescription = sceneDescription }.then(taps),
    ) {
        val sx = size.width / WORLD_WIDTH.toFloat()
        val sy = size.height / WORLD_HEIGHT.toFloat()
        val state = session.state
        val tick = state.tick
        val still = reducedMotion
        val fair = state.spec.type == LevelType.CLAY
        val recipe = sceneFor(state.spec.place)
        if (fair) {
            drawSceneBack(sceneFor(2), sx, sy, tick, still)
            drawFairStall(sx, sy, tick, still)
        } else {
            drawSceneBack(recipe, sx, sy, tick, still)
        }
        for (clay in state.extras?.clays.orEmpty()) {
            if (clay.status == ClayStatus.FLYING) drawClay(clay, sx, sy, highContrast)
        }
        for (bird in state.birds) {
            if (bird.status != BirdStatus.GONE) drawBird(bird, sx, sy, highContrast, tick, still)
        }
        if (!fair) {
            val wind = state.extras?.windFx ?: 0
            drawSceneFront(recipe, sx, sy, tick, still, if (wind > 0) 1 else if (wind < 0) -1 else 0)
            if (recipe.fireflies) drawFireflies(sx, sy, tick, still)
        }
        drawWeather(state, sx, sy, still)
        val boss = state.extras?.boss
        if (!still && boss != null && boss.stormUntil > tick && boss.stormUntil - tick > BossStyle.STORM_TICKS - 7) {
            // The thunder flock's lightning: a soft flash, never a white screen.
            drawRect(Color.White.copy(alpha = 0.18f))
        }
        drawReplayOverlay(tick, trace, replayPhotos, sx, sy, colors.accent)
        drawDog(session, sx, sy, paint, dogLaugh, dogPhoto, dogId, still)
        drawView(session, sx, sy, colors.viewfinder, aimPoint, viewfinder)
        drawFeathers(session, sx, sy)
        drawFloater(session, sx, sy, paint, colors.accent)
        if (tick < session.bossStolenUntilTick && stolenText.isNotEmpty()) {
            paint.color = android.graphics.Color.rgb(255, 214, 107)
            paint.textSize = max(12f, 8f * sx)
            val width = paint.measureText(stolenText)
            drawContext.canvas.nativeCanvas.drawText(stolenText, (size.width - width) / 2f, 40f * sy, paint)
        }
        if (tick < session.pinkUntilTick) {
            drawRect(Color(0xFFFFB4A8).copy(alpha = 0.35f))
        }
    }
}

private fun DrawScope.drawClay(
    clay: pt.tiago.ducknight.core.fair.ClayPigeon,
    sx: Float,
    sy: Float,
    highContrast: Boolean,
) {
    val center = Offset(clay.x / 256f * sx, clay.y / 256f * sy)
    val radius = max(2f, 13f * clay.scalePermille / 1_000f * sx)
    drawCircle(Color(0xFFE58A4A), radius = radius, center = center)
    drawCircle(Color(0xFF5A2A1A), radius = radius, center = center, style = Stroke(width = max(1f, sx * 0.6f)))
    if (highContrast) {
        drawCircle(Color.White, radius = radius + 2f * sx, center = center, style = Stroke(width = max(1.5f, sx * 1.4f)))
        drawCircle(Color.Black, radius = radius + 1f * sx, center = center, style = Stroke(width = max(1f, sx * 0.8f)))
    }
    drawLine(
        Color(0xFF5A2A1A),
        start = Offset(center.x - radius, center.y),
        end = Offset(center.x + radius, center.y),
        strokeWidth = max(1f, sx * 0.4f),
    )
}

private fun DrawScope.drawBird(bird: BirdState, sx: Float, sy: Float, highContrast: Boolean, tick: Int, still: Boolean) {
    val cx = bird.x / 256f * sx
    val cy = bird.y / 256f * sy
    val radius = max(7f, bird.species.radius.toFloat())
    val boss = bird.bossPhase > 0
    val unit = radius * sx * if (boss) 1.6f else 1f
    val style = if (boss) BossStyle.of(bird.species.id).forPhase(bird.bossPhase) else BossStyle.PLAIN
    val accent = speciesColor(bird.species.id, bird.species.category)
    if (boss && bird.diveUntilTick > tick) {
        // Out of sight after a page, or under water: only the grebe leaves rings on the surface.
        if (style == BossStyle.ROYAL_GREBE && bird.y / 256f >= 200f) drawRipple(bird.x / 256f, bird.y / 256f, sx, sy, tick, still)
        return
    }
    var alpha = if (bird.hidden) 0.18f else 1f
    if (boss) {
        val tell = style == BossStyle.GHOST && bird.nextTurnTick - bird.ageTicks <= BossStyle.GHOST_TELL_TICKS
        when (style) {
            BossStyle.GHOST -> alpha = if (bird.hidden) 0.08f else 0.7f
            BossStyle.SNOW_GOOSE -> if (bird.hidden) alpha = 0.1f
            else -> Unit
        }
        // A soft ring of light marks the legendary bird.
        drawCircle(accent.copy(alpha = 0.16f * alpha), radius = unit * 1.25f, center = Offset(cx, cy))
        if (tell) {
            val pulse = if (still) 1f else 0.6f + 0.4f * kotlin.math.sin(tick * 0.5f)
            drawCircle(Color(0xFFDDF6FF).copy(alpha = 0.45f * pulse), radius = unit * 1.1f, center = Offset(cx, cy))
        }
    }
    val head = Offset(cx + unit * 0.42f, cy - unit * 0.42f)
    drawBirdFigure(
        center = Offset(cx, cy),
        unit = unit,
        accent = accent,
        wingUp = bird.wingPhase in 1638..2457,
        alpha = alpha,
        stroke = max(1.2f, sx * 0.8f),
        highContrast = highContrast,
    )
    if (boss && style == BossStyle.MIST_GOOSE && bird.hidden) {
        // The mist closes around the goose while it is out of reach.
        for (puff in 0 until 3) {
            drawOval(
                Color(0xFFE6ECF2).copy(alpha = 0.55f),
                topLeft = Offset(cx - unit * (1.6f - puff * 0.8f), cy - unit * (0.6f + puff % 2 * 0.4f)),
                size = Size(unit * 1.8f, unit * 1.1f),
            )
        }
    }
    // Startled birds open the eye and hold still.
    if (bird.status == BirdStatus.STARTLED) {
        drawCircle(Color.White.copy(alpha = 0.9f), radius = unit * 0.07f, center = Offset(head.x + unit * 0.05f, head.y - unit * 0.03f))
        drawCircle(DogNoseColor, radius = unit * 0.03f, center = Offset(head.x + unit * 0.06f, head.y - unit * 0.03f))
    }
}

private fun DrawScope.drawDog(
    session: GameSceneState,
    sx: Float,
    sy: Float,
    paint: Paint,
    dogLaugh: String,
    dogPhoto: String,
    dogId: String,
    still: Boolean,
) {
    val tick = session.state.tick
    val cue = session.dogCue?.takeIf { tick <= it.untilTick }
    // In a round the dog is always on the bank; the replay only shows it for its reactions.
    val worldX = session.dogX ?: cue?.worldX?.coerceIn(30f, 176f) ?: return
    if (worldX < -12f) return
    val target = cue?.worldX?.coerceIn(30f, 176f)
    // It only reacts once it has arrived; on the way it trots.
    val arrived = target == null || kotlin.math.abs(target - worldX) < 2f
    val cx = worldX * sx
    val base = (250 - if (!arrived && !still) kotlin.math.abs(kotlin.math.sin(tick * 0.6f)) * 2f else 0f) * sy
    val pose = when {
        cue == null || !arrived -> DogPose.SIT
        cue.kind == pt.tiago.ducknight.game.DogCueKind.PHOTO -> DogPose.PHOTO
        cue.kind == pt.tiago.ducknight.game.DogCueKind.LAUGH -> DogPose.LAUGH
        else -> DogPose.SHRUG
    }
    drawDogFigure(dogId, pose, cx, base, sx, tick, still)
    when (pose) {
        DogPose.PHOTO -> {
            // The developed photo, held up for the player: white border, the quality on it.
            val center = dogCardCenter(cx, base, sx)
            val card = Rect(center.x - 9f * sx, center.y - 7f * sy, center.x + 9f * sx, center.y + 7f * sy)
            drawRect(PhotoPaper, topLeft = card.topLeft, size = card.size)
            drawRect(Color(0xFF5E2A5C), topLeft = Offset(card.left + 1.5f * sx, card.top + 1.5f * sy), size = Size(card.width - 3f * sx, card.height - 5f * sy))
            drawRect(DogNoseColor, topLeft = card.topLeft, size = card.size, style = Stroke(width = max(1f, sx * 0.4f)))
            paint.color = android.graphics.Color.rgb(255, 248, 238)
            paint.textSize = max(9f, 6f * sx)
            val quality = "${cue?.quality ?: 0}"
            drawContext.canvas.nativeCanvas.drawText(quality, center.x - paint.measureText(quality) / 2f, center.y + 2f * sy, paint)
            paint.color = android.graphics.Color.rgb(247, 232, 216)
            paint.textSize = max(8f, 4.5f * sx)
            drawContext.canvas.nativeCanvas.drawText(dogPhoto, center.x - paint.measureText(dogPhoto) / 2f, card.top - 2f * sy, paint)
        }
        DogPose.LAUGH -> {
            paint.color = android.graphics.Color.rgb(247, 232, 216)
            paint.textSize = max(10f, 7f * sx)
            drawContext.canvas.nativeCanvas.drawText(dogLaugh, cx - 2f * sx, base - 34f * sy, paint)
        }
        DogPose.SHRUG, DogPose.SIT -> Unit
    }
}

private fun DrawScope.drawView(
    session: GameSceneState,
    sx: Float,
    sy: Float,
    color: Color,
    aimPoint: Pair<Int, Int>?,
    style: String,
) {
    val x = aimPoint?.first ?: session.lastAimX ?: return
    val y = aimPoint?.second ?: session.lastAimY ?: return
    if (aimPoint == null && session.state.tick > session.aimUntilTick) return
    val halfW = 26f * sx
    val halfH = 19.5f * sy
    val cx = x * sx
    val cy = y * sy
    val corner = 6f * sx
    val stroke = max(1.5f, 1.2f * sx)
    val rect = Rect(cx - halfW, cy - halfH, cx + halfW, cy + halfH)
    when (style) {
        "reticle" -> {
            drawLine(color, Offset(rect.left, cy), Offset(rect.right, cy), strokeWidth = stroke)
            drawLine(color, Offset(cx, rect.top), Offset(cx, rect.bottom), strokeWidth = stroke)
        }
        "circle" -> drawCircle(color, radius = halfH * 0.9f, center = Offset(cx, cy), style = Stroke(width = stroke))
        "thirds" -> {
            for (fraction in listOf(1f / 3f, 2f / 3f)) {
                drawLine(color, Offset(rect.left + rect.width * fraction, rect.top), Offset(rect.left + rect.width * fraction, rect.bottom), strokeWidth = stroke)
                drawLine(color, Offset(rect.left, rect.top + rect.height * fraction), Offset(rect.right, rect.top + rect.height * fraction), strokeWidth = stroke)
            }
        }
        else -> {
            drawLine(color, Offset(rect.left, rect.top), Offset(rect.left + corner, rect.top), strokeWidth = stroke)
            drawLine(color, Offset(rect.left, rect.top), Offset(rect.left, rect.top + corner), strokeWidth = stroke)
            drawLine(color, Offset(rect.right, rect.top), Offset(rect.right - corner, rect.top), strokeWidth = stroke)
            drawLine(color, Offset(rect.right, rect.top), Offset(rect.right, rect.top + corner), strokeWidth = stroke)
            drawLine(color, Offset(rect.left, rect.bottom), Offset(rect.left + corner, rect.bottom), strokeWidth = stroke)
            drawLine(color, Offset(rect.left, rect.bottom), Offset(rect.left, rect.bottom - corner), strokeWidth = stroke)
            drawLine(color, Offset(rect.right, rect.bottom), Offset(rect.right - corner, rect.bottom), strokeWidth = stroke)
            drawLine(color, Offset(rect.right, rect.bottom), Offset(rect.right, rect.bottom - corner), strokeWidth = stroke)
        }
    }
    drawCircle(color, radius = max(1f, sx * 0.8f), center = Offset(cx, cy))
}

private fun DrawScope.drawFeathers(session: GameSceneState, sx: Float, sy: Float) {
    for (feather in session.visibleFeathers()) {
        val age = (session.state.tick - feather.startTick).coerceAtLeast(0)
        val x = (feather.x + feather.vx * age) * sx
        val y = (feather.y + feather.vy * age + 0.06f * age * age) * sy
        drawLine(
            Color(0xFFF7E8D8).copy(alpha = (1f - age / 72f).coerceIn(0f, 1f)),
            Offset(x, y),
            Offset(x + 2 * sx, y + 3 * sy),
            strokeWidth = max(1f, sx * 0.5f),
        )
    }
}

private fun DrawScope.drawReplayOverlay(
    tick: Int,
    trace: List<FailureTracePoint>,
    photos: List<ReplayPhoto>,
    sx: Float,
    sy: Float,
    color: Color,
) {
    val visible = trace.filter { it.tick <= tick }
    if (visible.size > 1) {
        val path = Path().apply {
            moveTo(visible.first().x / 256f * sx, visible.first().y / 256f * sy)
            for (point in visible.drop(1)) {
                lineTo(point.x / 256f * sx, point.y / 256f * sy)
            }
        }
        drawPath(path, color.copy(alpha = 0.45f), style = Stroke(width = max(1f, sx * 0.8f)))
    }
    visible.lastOrNull()?.let { point ->
        drawCircle(
            color.copy(alpha = 0.8f),
            radius = max(1.5f, sx * 2.2f),
            center = Offset(point.x / 256f * sx, point.y / 256f * sy),
        )
    }
    for (photo in photos) {
        if (photo.tick > tick) continue
        val halfWidth = photo.frame.widthUnits * sx
        val halfHeight = photo.frame.heightUnits * sy
        val center = Offset(photo.aimX * sx, photo.aimY * sy)
        drawRect(
            color = color.copy(alpha = 0.85f),
            topLeft = Offset(center.x - halfWidth, center.y - halfHeight),
            size = Size(halfWidth * 2, halfHeight * 2),
            style = Stroke(width = max(1f, sx * 0.8f)),
        )
    }
}

private fun DrawScope.drawFloater(session: GameSceneState, sx: Float, sy: Float, paint: Paint, color: Color) {
    val floater = session.floater ?: return
    val age = session.state.tick - floater.startTick
    if (age !in 0..48) return
    paint.color = android.graphics.Color.argb(
        (255 * (1f - age / 48f)).toInt(),
        (color.red * 255).toInt(),
        (color.green * 255).toInt(),
        (color.blue * 255).toInt(),
    )
    paint.textSize = max(10f, 7f * sx)
    drawContext.canvas.nativeCanvas.drawText(
        floater.text,
        floater.x * sx,
        floater.y * sy - age * 0.25f * sy,
        paint,
    )
}

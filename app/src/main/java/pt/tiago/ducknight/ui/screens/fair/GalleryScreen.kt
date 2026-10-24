package pt.tiago.ducknight.ui.screens.fair

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.tiago.ducknight.R
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.game.WorldMapping
import pt.tiago.ducknight.ui.theme.LocalDuckColors
import pt.tiago.ducknight.ui.theme.LocalDuckType
import kotlin.math.abs
import kotlin.random.Random

private data class GalleryTarget(
    val rack: Int,
    var x: Float,
    val y: Float,
    val speed: Float,
)

@Composable
fun GalleryScreen(
    difficulty: Difficulty,
    onFinished: (score: Int, targets: Int) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    var remaining by remember { mutableFloatStateOf(60f) }
    var score by remember { mutableIntStateOf(0) }
    var corks by remember { mutableIntStateOf(5) }
    var targetsHit by remember { mutableIntStateOf(0) }
    // An empty load refills by itself after 1.2 s.
    var reloadLeft by remember { mutableFloatStateOf(0f) }
    val speedScale = difficulty.speedPercent / 100f
    var running by remember { mutableStateOf(true) }
    val rnd = remember { Random(7) }
    val targets = remember {
        mutableListOf<GalleryTarget>().apply {
            for (rack in 0..2) {
                repeat(4) { index ->
                    add(
                        GalleryTarget(
                            rack = rack,
                            x = 30f + index * 50f,
                            y = 70f + rack * 55f,
                            speed = if (rack == 0) 18f else if (rack == 1) -24f else 30f,
                        ),
                    )
                }
            }
        }
    }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        var last = withFrameNanos { it }
        while (running) {
            withFrameNanos { now ->
                val dt = ((now - last) / 1_000_000_000f).coerceAtMost(0.1f)
                last = now
                remaining -= dt
                if (corks == 0) {
                    reloadLeft -= dt
                    if (reloadLeft <= 0f) corks = 5
                }
                if (remaining <= 0f) {
                    remaining = 0f
                    running = false
                    onFinished(score, targetsHit)
                }
                for (target in targets) {
                    target.x += target.speed * speedScale * dt
                    if (target.x < 12f) target.x = 228f
                    if (target.x > 228f) target.x = 12f
                }
            }
        }
    }
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.level_back), color = colors.textMuted) }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.gallery_time, remaining.toInt()), style = type.hudLevel, color = colors.text)
            Text("  ${stringResource(R.string.gallery_score, score)}", style = type.hudLevel, color = colors.accent)
        }
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.8f)
                .background(colors.surface)
                .pointerInput(running) {
                    detectTapGestures { offset ->
                        if (!running) return@detectTapGestures
                        val aimX = WorldMapping.worldX(offset.x, size.width.toFloat())
                        val aimY = WorldMapping.worldY(offset.y, size.height.toFloat())
                        val hit = targets.minByOrNull { target ->
                            abs(target.x - aimX) + abs(target.y - aimY)
                        }?.takeIf { abs(it.x - aimX) + abs(it.y - aimY) < 18f }
                        if (corks > 0) {
                            corks--
                            if (corks == 0) reloadLeft = 1.2f
                            if (hit != null) {
                                score += 100
                                targetsHit++
                                hit.x = rnd.nextFloat() * 216f + 12f
                            }
                        }
                    }
                },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val sx = size.width / 240f
                val sy = size.height / 300f
                for (target in targets) {
                    drawCircle(
                        Color(0xFFC9B17A),
                        radius = 9 * sx,
                        center = Offset(target.x * sx, target.y * sy),
                    )
                    drawCircle(
                        Color(0xFF1B1028),
                        radius = 9 * sx,
                        center = Offset(target.x * sx, target.y * sy),
                        style = Stroke(width = 1.5f * sx),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text("${corks}", style = type.hudScore, color = colors.accent)
            Text(
                if (corks == 0) stringResource(R.string.gallery_reloading) else stringResource(R.string.gallery_corks),
                style = type.caption,
                color = colors.textMuted,
            )
        }
        if (!running) {
            Button(onClick = { running = true; remaining = 60f; score = 0; corks = 5; targetsHit = 0 }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Text(stringResource(R.string.gallery_start))
            }
        }
    }
}

package pt.tiago.ducknight.ui.screens.replay

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.isActive
import pt.tiago.ducknight.R
import pt.tiago.ducknight.core.replay.FailureReplay
import pt.tiago.ducknight.game.ReplaySession
import pt.tiago.ducknight.ui.screens.game.GameCanvas
import pt.tiago.ducknight.ui.theme.LocalDuckColors
import pt.tiago.ducknight.ui.theme.LocalDuckShapes
import pt.tiago.ducknight.ui.theme.LocalDuckType

private const val TICK_NANOS = 1_000_000_000L / 60L
private const val REPLAY_SPEED_DIVISOR = 4L

@Composable
fun ReplayScreen(
    replay: FailureReplay,
    onBack: () -> Unit,
    dogId: String = "farrusco",
    viewfinder: String = "corners",
    highContrast: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val shapes = LocalDuckShapes.current
    val first = replay.frames.first()
    val session = remember(replay) { ReplaySession(first) }
    var index by remember(replay) { mutableIntStateOf(0) }
    val last = replay.frames.lastIndex
    LaunchedEffect(replay) {
        var previous = withFrameNanos { it }
        var accumulator = 0L
        val stepNanos = TICK_NANOS * REPLAY_SPEED_DIVISOR
        while (isActive && index < last) {
            withFrameNanos { now ->
                accumulator += (now - previous).coerceAtLeast(0L)
                previous = now
                while (accumulator >= stepNanos && index < last) {
                    index++
                    accumulator -= stepNanos
                    session.state = replay.frames[index]
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
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.replay_title), style = type.placeName, color = colors.text)
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.replay_speed), style = type.caption, color = colors.accent)
        }
        Text(
            stringResource(R.string.replay_progress, index + 1, last + 1),
            style = type.caption,
            color = colors.textMuted,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.8f)
                .background(colors.surface),
        ) {
            GameCanvas(
                session = session,
                dogLaugh = stringResource(R.string.dog_laugh),
                dogPhoto = stringResource(R.string.dog_photo),
                dogId = dogId,
                viewfinder = viewfinder,
                highContrast = highContrast,
                trace = replay.trace,
                replayPhotos = replay.photos,
            )
        }
        Text(
            stringResource(R.string.replay_caption),
            style = type.caption,
            color = colors.textMuted,
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onBack,
            shape = RoundedCornerShape(shapes.button),
            modifier = Modifier.fillMaxWidth(0.7f),
        ) {
            Text(stringResource(R.string.replay_close), style = type.button)
        }
    }
}

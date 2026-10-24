package pt.tiago.ducknight.ui.screens.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.tiago.ducknight.R
import pt.tiago.ducknight.ui.theme.DuckColors
import pt.tiago.ducknight.ui.theme.DuckShapes
import pt.tiago.ducknight.ui.theme.DuckType
import pt.tiago.ducknight.ui.theme.LocalDuckColors
import pt.tiago.ducknight.ui.theme.LocalDuckShapes
import pt.tiago.ducknight.ui.theme.LocalDuckType
import pt.tiago.ducknight.ui.theme.ThemeId
import pt.tiago.ducknight.ui.theme.themeColors

/** Home screen: the current place, the next level and the other modes. */
/** What the Home screen shows of the player's campaign. */
data class HomeProgress(
    val place: Int,
    val places: Int,
    val placeName: String,
    val nextLevel: Int,
    val levelsDone: Int,
    val levels: Int,
    val birdsSeen: Int,
    val birds: Int,
)

@Composable
fun HomeScreen(
    themeId: ThemeId,
    progress: HomeProgress,
    onContinue: () -> Unit,
    onSettings: () -> Unit = {},
    onTraining: () -> Unit = {},
    onProfile: () -> Unit = {},
    onDaily: () -> Unit = {},
    onBirdBook: () -> Unit = {},
    onFair: () -> Unit = {},
    onArcade: () -> Unit = {},
    onDuel: () -> Unit = {},
    title: String? = null,
    stars: Int = 0,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val titleText = title ?: stringResource(R.string.home_rank_sample)
    BoxWithConstraints(
        modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(colors.bg, colors.bg2))),
    ) {
        val compact = maxWidth < 360.dp
        val gutter = if (compact) 16.dp else 20.dp
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = gutter, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                Modifier.widthIn(max = 520.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HomeHeader(compact = compact, title = titleText, stars = stars, onProfile = onProfile)
                PlaceScene(themeId = themeId, progress = progress)
                ContinueButton(progress, onContinue)
                ProgressLine(progress)
                ModeGrid(progress = progress, onDaily = onDaily, onBirdBook = onBirdBook, onFair = onFair, onArcade = onArcade)
                SmallModes(onTraining = onTraining, onDuel = onDuel, onSettings = onSettings)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun HomeHeader(compact: Boolean, title: String, stars: Int, onProfile: () -> Unit) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val brand = if (compact) type.brand.copy(fontSize = 36.sp, lineHeight = 34.sp) else type.brand
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.brand_duck), style = brand, color = colors.text)
            Text(stringResource(R.string.brand_night), style = brand, color = colors.accent)
            Text(stringResource(R.string.home_subtitle), style = type.caption, color = colors.textMuted)
        }
        Surface(
            shape = RoundedCornerShape(LocalDuckShapes.current.button),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.line),
            modifier = Modifier.clickable(onClick = onProfile),
        ) {
            Text(
                stringResource(R.string.home_rank_chip, title, stars),
                style = type.caption,
                color = colors.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/** The place preview: a painted dusk scene with a flying duck, drawn in code (no raster assets). */
@Composable
private fun PlaceScene(themeId: ThemeId, progress: HomeProgress) {
    val colors = LocalDuckColors.current
    val shapes = LocalDuckShapes.current
    val type = LocalDuckType.current
    Surface(
        shape = RoundedCornerShape(shapes.card),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.line),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(Modifier.fillMaxWidth().height(150.dp)) {
            Canvas(
                Modifier
                    .fillMaxSize()
                    .clearAndSetSemantics {},
            ) {
                paintDuskScene(themeId, colors)
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colorStops = arrayOf(
                                0.35f to colors.bg.copy(alpha = 0f),
                                1f to colors.bg.copy(alpha = 0.82f),
                            ),
                        ),
                    ),
            )
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(16.dp),
            ) {
                Text(
                    stringResource(R.string.home_place_label, progress.place, progress.places),
                    style = type.label,
                    color = colors.textMuted,
                )
                Text(progress.placeName, style = type.placeName, color = colors.text)
            }
        }
    }
}

private fun DrawScope.paintDuskScene(themeId: ThemeId, colors: DuckColors) {
    val flat = themeId == ThemeId.CASSETTE
    if (flat) {
        drawRect(colors.bg)
    } else {
        drawRect(
            brush = Brush.verticalGradient(
                listOf(colors.bg, colors.surface2, colors.accent.copy(alpha = 0.55f)),
            ),
        )
    }
    // Sun low over the tree line.
    val sunCenter = Offset(size.width * 0.74f, size.height * 0.58f)
    val sunRadius = size.minDimension * 0.09f
    if (flat) {
        drawRect(
            color = colors.accent,
            topLeft = Offset(sunCenter.x - sunRadius, sunCenter.y - sunRadius),
            size = Size(sunRadius * 2, sunRadius * 2),
        )
    } else {
        drawCircle(colors.accent.copy(alpha = 0.9f), radius = sunRadius, center = sunCenter)
        drawCircle(colors.accent.copy(alpha = 0.18f), radius = sunRadius * 1.7f, center = sunCenter)
    }
    // Water.
    drawRect(
        color = colors.bg2.copy(alpha = 0.65f),
        topLeft = Offset(0f, size.height * 0.68f),
        size = Size(size.width, size.height * 0.32f),
    )
    // Tree line in silhouette.
    val trees = Path().apply {
        moveTo(0f, size.height * 0.70f)
        var x = 0f
        var index = 0
        while (x < size.width) {
            val step = size.width * (0.035f + 0.012f * (index % 3))
            val top = size.height * (0.56f - 0.02f * (index % 4))
            lineTo(x + step * 0.5f, top)
            lineTo(x + step, size.height * 0.70f)
            x += step
            index++
        }
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(trees, colors.bg2.copy(alpha = 0.9f))
    // Reeds in the foreground.
    repeat(14) { index ->
        val x = size.width * (0.03f + index * 0.072f)
        val top = size.height * (0.66f + (index % 3) * 0.035f)
        drawLine(
            color = colors.bg2,
            start = Offset(x, size.height),
            end = Offset(x + size.width * 0.012f, top),
            strokeWidth = size.width * 0.007f,
        )
    }
    // A duck crossing the sun.
    drawDuck(
        body = colors.bg2,
        outline = if (flat) colors.bg2 else colors.accent.copy(alpha = 0.85f),
        left = size.width * 0.46f,
        top = size.height * 0.30f,
        width = size.width * 0.16f,
        height = size.height * 0.12f,
        wingUp = true,
    )
}

private fun DrawScope.drawDuck(
    body: Color,
    outline: Color,
    left: Float,
    top: Float,
    width: Float,
    height: Float,
    wingUp: Boolean,
) {
    fun x(fraction: Float) = left + width * fraction
    fun y(fraction: Float) = top + height * fraction
    val silhouette = Path().apply {
        moveTo(x(0.06f), y(0.64f))
        cubicTo(x(0.02f), y(0.44f), x(0.16f), y(0.28f), x(0.38f), y(0.30f))
        cubicTo(x(0.54f), y(0.31f), x(0.64f), y(0.38f), x(0.70f), y(0.50f))
        cubicTo(x(0.76f), y(0.62f), x(0.70f), y(0.73f), x(0.54f), y(0.76f))
        lineTo(x(0.22f), y(0.76f))
        cubicTo(x(0.12f), y(0.76f), x(0.07f), y(0.71f), x(0.06f), y(0.64f))
        close()
        moveTo(x(0.08f), y(0.56f))
        lineTo(x(-0.05f), y(0.46f))
        lineTo(x(0.05f), y(0.66f))
        close()
    }
    val head = Offset(x(0.72f), y(0.36f))
    drawPath(silhouette, body)
    drawCircle(body, radius = height * 0.15f, center = head)
    val beak = Path().apply {
        moveTo(x(0.83f), y(0.34f))
        lineTo(x(0.99f), y(0.38f))
        lineTo(x(0.83f), y(0.43f))
        close()
    }
    drawPath(beak, body)
    val wing = Path().apply {
        if (wingUp) {
            moveTo(x(0.30f), y(0.54f))
            cubicTo(x(0.38f), y(0.30f), x(0.60f), y(0.28f), x(0.66f), y(0.48f))
            cubicTo(x(0.54f), y(0.58f), x(0.40f), y(0.60f), x(0.30f), y(0.54f))
        } else {
            moveTo(x(0.30f), y(0.52f))
            cubicTo(x(0.40f), y(0.62f), x(0.58f), y(0.62f), x(0.64f), y(0.52f))
            cubicTo(x(0.54f), y(0.48f), x(0.40f), y(0.46f), x(0.30f), y(0.52f))
        }
        close()
    }
    drawPath(wing, body)
    if (outline.alpha > 0f) {
        val stroke = Stroke(width = size.minDimension * 0.006f)
        drawPath(silhouette, outline, style = stroke)
        drawPath(wing, outline, style = stroke)
        drawCircle(outline, radius = height * 0.15f, center = head, style = stroke)
    }
}

@Composable
private fun ContinueButton(progress: HomeProgress, onClick: () -> Unit) {
    val colors = LocalDuckColors.current
    val shapes = LocalDuckShapes.current
    val type = LocalDuckType.current
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(shapes.button),
        colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.onAccent),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
        modifier = Modifier.fillMaxWidth().height(56.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(stringResource(R.string.home_continue), style = type.button)
            Text(
                stringResource(R.string.home_continue_caption, progress.nextLevel, progress.placeName),
                style = type.caption,
            )
        }
    }
}

@Composable
private fun ProgressLine(progress: HomeProgress) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            stringResource(
                R.string.home_progress,
                progress.place,
                progress.places,
                progress.levelsDone,
                progress.levels,
            ),
            style = type.caption,
            color = colors.textMuted,
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(6.dp)
                .background(colors.surface2, RoundedCornerShape(3.dp)),
        ) {
            val fraction = if (progress.levels == 0) 0f else progress.levelsDone.toFloat() / progress.levels
            if (fraction > 0f) {
                Box(
                    Modifier
                        .fillMaxWidth(fraction.coerceAtLeast(0.01f))
                        .height(6.dp)
                        .background(colors.accent, RoundedCornerShape(3.dp)),
                )
            }
        }
    }
}

@Composable
private fun ModeGrid(progress: HomeProgress, onDaily: () -> Unit, onBirdBook: () -> Unit, onFair: () -> Unit, onArcade: () -> Unit) {
    val type = LocalDuckType.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ModeCard(
                title = stringResource(R.string.home_daily),
                caption = stringResource(R.string.home_daily_caption),
                badge = LocalDuckColors.current.accent,
                modifier = Modifier.weight(1f),
                onClick = onDaily,
            )
            ModeCard(
                title = stringResource(R.string.home_arcade),
                caption = stringResource(R.string.home_arcade_caption),
                badge = LocalDuckColors.current.good,
                modifier = Modifier.weight(1f),
                onClick = onArcade,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            ModeCard(
                title = stringResource(R.string.home_fair),
                caption = stringResource(R.string.home_fair_caption),
                badge = LocalDuckColors.current.danger,
                modifier = Modifier.weight(1f),
                onClick = onFair,
            )
            ModeCard(
                title = stringResource(R.string.home_album),
                caption = stringResource(R.string.home_album_caption, progress.birdsSeen, progress.birds),
                badge = LocalDuckColors.current.protected,
                modifier = Modifier.weight(1f),
                onClick = onBirdBook,
            )
        }
    }
}

@Composable
private fun ModeCard(title: String, caption: String, badge: Color, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val colors = LocalDuckColors.current
    val shapes = LocalDuckShapes.current
    val type = LocalDuckType.current
    Surface(
        shape = RoundedCornerShape(shapes.card),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.line),
        modifier = modifier.heightIn(min = 86.dp).then(
            if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
        ),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Surface(shape = CircleShape, color = badge, modifier = Modifier.size(10.dp)) {}
            Text(title, style = type.cardTitle, color = colors.text)
            Text(caption, style = type.caption, color = colors.textMuted)
        }
    }
}

@Composable
private fun SmallModes(onTraining: () -> Unit, onDuel: () -> Unit, onSettings: () -> Unit) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            SmallModeButton(stringResource(R.string.home_training), onTraining, Modifier.weight(1f))
            SmallModeButton(stringResource(R.string.home_duel), onDuel, Modifier.weight(1f))
        }
        SmallModeButton(stringResource(R.string.home_settings), onSettings, Modifier.fillMaxWidth())
    }
}

@Composable
private fun SmallModeButton(label: String, onClick: () -> Unit, modifier: Modifier) {
    val colors = LocalDuckColors.current
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(LocalDuckShapes.current.button),
        border = BorderStroke(1.dp, colors.line),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.text),
        modifier = modifier.height(48.dp),
    ) {
        Text(label, style = LocalDuckType.current.button)
    }
}

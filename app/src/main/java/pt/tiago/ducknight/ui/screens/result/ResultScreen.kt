package pt.tiago.ducknight.ui.screens.result

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.tiago.ducknight.R
import pt.tiago.ducknight.catalog.ContentRepository
import pt.tiago.ducknight.core.content.LevelFile
import pt.tiago.ducknight.core.engine.BirdResult
import pt.tiago.ducknight.core.engine.RoundResult
import pt.tiago.ducknight.ui.theme.LocalDuckColors
import pt.tiago.ducknight.ui.theme.LocalDuckShapes
import pt.tiago.ducknight.ui.theme.LocalDuckType

@Composable
fun ResultScreen(
    summary: RoundResult,
    results: List<BirdResult>,
    level: LevelFile,
    repository: ContentRepository,
    hasNext: Boolean,
    onNext: () -> Unit,
    onRepeat: () -> Unit,
    onMap: () -> Unit,
    onReplay: (BirdResult) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val shapes = LocalDuckShapes.current
    val average = results.filter { it.photographed }.map { it.quality }.average().let {
        if (it.isNaN()) 0 else it.toInt()
    }
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.result_title), style = type.placeName, color = colors.text)
        Text(
            stringResource(R.string.game_level, level.ordinal),
            style = type.caption,
            color = colors.textMuted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            for (index in 0 until 3) {
                Text(
                    if (index < summary.stars) "★" else "☆",
                    fontSize = 36.sp,
                    color = if (index < summary.stars) colors.accent else colors.line,
                )
            }
        }
        if (!summary.passed) {
            Text(stringResource(R.string.result_failed), style = type.bodyText, color = colors.danger)
        }
        Text(stringResource(R.string.result_score, summary.score), style = type.hudScore, color = colors.accent)
        Text(
            stringResource(R.string.result_birds, summary.photographed, summary.birds, average),
            style = type.caption,
            color = colors.textMuted,
            textAlign = TextAlign.Center,
        )
        if (level.type == pt.tiago.ducknight.core.level.LevelType.BOSS) {
            Text(stringResource(R.string.result_boss, summary.bossPages), style = type.caption, color = colors.accent)
        }
        if (results.isNotEmpty()) {
            Text(
                stringResource(R.string.result_replay_hint),
                style = type.caption,
                color = colors.textMuted,
            )
        }
        // Every bird of the round, photographed or fled, in one grid that fills the free space.
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(results, key = { it.birdId }) { result ->
                PhotoCard(result = result, repository = repository, onClick = { onReplay(result) })
            }
        }
        Button(
            onClick = onNext,
            enabled = hasNext,
            shape = RoundedCornerShape(shapes.button),
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) { Text(stringResource(R.string.result_next), style = type.button) }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = onRepeat,
                shape = RoundedCornerShape(shapes.button),
                modifier = Modifier.weight(1f).height(48.dp),
            ) { Text(stringResource(R.string.result_repeat)) }
            OutlinedButton(
                onClick = onMap,
                shape = RoundedCornerShape(shapes.button),
                modifier = Modifier.weight(1f).height(48.dp),
            ) { Text(stringResource(R.string.result_map)) }
        }
    }
}

@Composable
private fun PhotoCard(result: BirdResult, repository: ContentRepository, onClick: () -> Unit) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val name = repository.speciesName(result.speciesId, LocalConfiguration.current.locales[0].language == "pt")
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (result.photographed) colors.surface2 else colors.surface,
        border = BorderStroke(1.dp, if (result.photographed) colors.good else colors.line),
        modifier = Modifier.fillMaxWidth().height(74.dp).clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                name,
                style = type.caption,
                color = if (result.photographed) colors.text else colors.textMuted,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Text(
                if (result.photographed) stringResource(R.string.result_quality, result.quality)
                else stringResource(R.string.result_fled),
                style = type.caption,
                color = colors.textMuted,
            )
            Text(
                if (result.photographed) "+${result.points}" else "—",
                style = type.caption,
                color = if (result.photographed) colors.accent else colors.textMuted,
            )
        }
    }
}

package pt.tiago.ducknight.ui.screens.daily

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.tiago.ducknight.R
import pt.tiago.ducknight.ui.screens.place.placeName
import pt.tiago.ducknight.catalog.ContentRepository
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.progress.Profile
import pt.tiago.ducknight.ui.theme.LocalDuckColors
import pt.tiago.ducknight.ui.theme.LocalDuckShapes
import pt.tiago.ducknight.ui.theme.LocalDuckType
import java.time.LocalDate

@Composable
fun DailyScreen(
    profile: Profile,
    repository: ContentRepository,
    date: LocalDate = LocalDate.now(),
    onPlay: (String, Difficulty) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val shapes = LocalDuckShapes.current
    val pool = repository.dailyPool
    val index = (date.toString().hashCode() and Int.MAX_VALUE) % pool.entries.size.coerceAtLeast(1)
    val entry = pool.entries.getOrNull(index)
    val today = profile.dailyHistory.firstOrNull { it.date == date.toString() }
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.profile_back), color = colors.textMuted) }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.daily_title), style = type.screenTitle, color = colors.text)
        }
        Text(date.format(java.time.format.DateTimeFormatter.ofLocalizedDate(java.time.format.FormatStyle.LONG)), style = type.caption, color = colors.textMuted)
        Text(stringResource(R.string.daily_body), style = type.bodyText, color = colors.text)
        if (today != null) {
            Text(stringResource(R.string.daily_done, today.score), style = type.caption, color = colors.accent)
        }
        Surface(shape = RoundedCornerShape(shapes.card), color = colors.surface, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                entry?.rounds?.forEachIndexed { round, value ->
                    val level = repository.level(value.levelId)
                    val difficultyName = when (Difficulty.valueOf(value.difficulty)) {
                        Difficulty.EASY -> stringResource(R.string.difficulty_easy)
                        Difficulty.MEDIUM -> stringResource(R.string.difficulty_medium)
                        Difficulty.HARD -> stringResource(R.string.difficulty_hard)
                    }
                    Column {
                        Text(
                            stringResource(R.string.daily_round, round + 1, level?.ordinal ?: 0, difficultyName),
                            style = type.bodyText,
                            color = colors.text,
                        )
                        if (level != null) {
                            Text(placeName(level.place), style = type.caption, color = colors.textMuted)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = {
                entry?.rounds?.firstOrNull()?.let { round ->
                    onPlay(round.levelId, Difficulty.valueOf(round.difficulty))
                }
            },
            shape = RoundedCornerShape(shapes.button),
            enabled = entry != null,
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) { Text(stringResource(R.string.daily_play), style = type.button) }
    }
}

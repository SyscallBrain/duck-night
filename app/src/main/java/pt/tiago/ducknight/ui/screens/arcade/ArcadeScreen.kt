package pt.tiago.ducknight.ui.screens.arcade

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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.tiago.ducknight.R
import pt.tiago.ducknight.progress.Records
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.ui.theme.LocalDuckColors
import pt.tiago.ducknight.ui.theme.LocalDuckShapes
import pt.tiago.ducknight.ui.theme.LocalDuckType

@Composable
fun ArcadeScreen(
    difficulty: Difficulty,
    onDifficulty: (Difficulty) -> Unit,
    records: Records,
    onStart: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val shapes = LocalDuckShapes.current
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.level_back), color = colors.textMuted) }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.arcade_title), style = type.placeName, color = colors.text)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (value in Difficulty.entries) {
                FilterChip(
                    selected = value == difficulty,
                    onClick = { onDifficulty(value) },
                    label = {
                        Text(
                            when (value) {
                                Difficulty.EASY -> stringResource(R.string.difficulty_easy)
                                Difficulty.MEDIUM -> stringResource(R.string.difficulty_medium)
                                Difficulty.HARD -> stringResource(R.string.difficulty_hard)
                            },
                        )
                    },
                )
            }
        }
        for ((game, label) in listOf(
            "A" to R.string.arcade_game_a,
            "B" to R.string.arcade_game_b,
            "C" to R.string.arcade_game_c,
        )) {
            Surface(
                shape = RoundedCornerShape(shapes.card),
                color = colors.surface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(stringResource(label), style = type.cardTitle, color = colors.text)
                    val key = "$game:${difficulty.name}"
                    val rounds = records.arcadeRounds[key] ?: 0
                    Text(
                        if (rounds == 0 && (records.arcadeScores[key] ?: 0) == 0) {
                            stringResource(R.string.record_none)
                        } else {
                            stringResource(R.string.record_arcade, rounds, records.arcadeScores[key] ?: 0)
                        },
                        style = type.caption,
                        color = colors.textMuted,
                    )
                    Button(
                        onClick = { onStart(game) },
                        modifier = Modifier.fillMaxWidth().height(46.dp),
                    ) { Text(stringResource(R.string.arcade_start)) }
                }
            }
        }
    }
}

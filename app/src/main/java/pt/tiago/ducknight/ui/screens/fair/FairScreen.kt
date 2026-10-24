package pt.tiago.ducknight.ui.screens.fair

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.ui.unit.dp
import pt.tiago.ducknight.R
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.progress.Records
import pt.tiago.ducknight.ui.theme.LocalDuckColors
import pt.tiago.ducknight.ui.theme.LocalDuckShapes
import pt.tiago.ducknight.ui.theme.LocalDuckType

@Composable
fun FairScreen(
    difficulty: Difficulty,
    records: Records,
    onDifficulty: (Difficulty) -> Unit,
    onClay: () -> Unit,
    onGallery: () -> Unit,
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
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.level_back), color = colors.textMuted) }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.fair_title), style = type.placeName, color = colors.text)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
        val clay = records.clayScores[difficulty.name] ?: 0
        val gallery = records.galleryScores[difficulty.name] ?: 0
        FairCard(
            stringResource(R.string.fair_clay),
            stringResource(R.string.fair_clay_caption),
            if (clay > 0) stringResource(R.string.record_points, clay) else stringResource(R.string.record_none),
            onClay,
        )
        FairCard(
            stringResource(R.string.fair_gallery),
            stringResource(R.string.fair_gallery_caption),
            if (gallery > 0) stringResource(R.string.record_points, gallery) else stringResource(R.string.record_none),
            onGallery,
        )
    }
}

@Composable
private fun FairCard(title: String, caption: String, record: String, onClick: () -> Unit) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val shapes = LocalDuckShapes.current
    Surface(
        shape = RoundedCornerShape(shapes.card),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.line),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = type.cardTitle, color = colors.text)
            Text(caption, style = type.caption, color = colors.textMuted)
            Text(record, style = type.caption, color = colors.accent)
        }
    }
}

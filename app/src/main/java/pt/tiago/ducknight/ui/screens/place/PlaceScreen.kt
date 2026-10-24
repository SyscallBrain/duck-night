package pt.tiago.ducknight.ui.screens.place

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.tiago.ducknight.R
import pt.tiago.ducknight.core.content.LevelFile
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.core.level.LevelType
import pt.tiago.ducknight.progress.Profile
import pt.tiago.ducknight.ui.theme.DuckColors
import pt.tiago.ducknight.ui.theme.DuckType
import pt.tiago.ducknight.ui.theme.LocalDuckColors
import pt.tiago.ducknight.ui.theme.LocalDuckShapes
import pt.tiago.ducknight.ui.theme.LocalDuckType

@Composable
fun PlaceScreen(
    place: Int,
    levels: List<LevelFile>,
    profile: Profile,
    difficulty: Difficulty,
    onDifficulty: (Difficulty) -> Unit,
    onBack: () -> Unit,
    onLevel: (LevelFile) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.level_back), color = colors.textMuted)
            }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.place_levels), style = type.caption, color = colors.textMuted)
        }
        Text(placeName(place), style = type.placeName, color = colors.text)
        Text(
            stringResource(R.string.place_progress, levels.count { profile.stars(it.id, difficulty.name) > 0 }, levels.size),
            style = type.caption,
            color = colors.textMuted,
        )
        Spacer(Modifier.height(12.dp))
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
                            style = type.caption,
                        )
                    },
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(levels, key = { it.id }) { level ->
                LevelNode(level = level, difficulty = difficulty, profile = profile, onClick = { onLevel(level) })
            }
        }
    }
}

@Composable
private fun LevelNode(level: LevelFile, difficulty: Difficulty, profile: Profile, onClick: () -> Unit) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val accent = typeColor(level.type, colors)
    val unlocked = profile.isUnlocked(level.id, level.ordinal, difficulty.name)
    val stars = profile.stars(level.id, difficulty.name)
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (unlocked) colors.surface else colors.bg,
        border = BorderStroke(if (unlocked && stars == 0) 2.dp else 1.dp, accent.copy(alpha = if (unlocked) 0.7f else 0.12f)),
        modifier = Modifier.fillMaxWidth().aspectRatio(1f).clickable(enabled = unlocked, onClick = onClick),
    ) {
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                level.ordinal.toString(),
                style = type.cardTitle.copy(fontSize = 16.sp),
                color = if (unlocked) colors.text else colors.textMuted.copy(alpha = 0.35f),
                textAlign = TextAlign.Center,
            )
            if (unlocked) {
                Text(
                    "★".repeat(stars) + "☆".repeat(3 - stars),
                    style = type.caption.copy(fontSize = 11.sp),
                    color = colors.accent,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun typeLabel(type: LevelType): String = when (type) {
    LevelType.CLASSIC -> stringResource(R.string.level_type_classic)
    LevelType.FILM -> stringResource(R.string.level_type_film)
    LevelType.BOSS -> stringResource(R.string.level_type_boss)
    else -> stringResource(R.string.level_type_other)
}

private fun typeColor(type: LevelType, colors: DuckColors): Color = when (type) {
    LevelType.BOSS -> colors.accent
    LevelType.FILM -> colors.protected
    else -> colors.good
}

package pt.tiago.ducknight.ui.screens.place

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.tiago.ducknight.R
import pt.tiago.ducknight.catalog.ContentRepository
import pt.tiago.ducknight.core.content.LevelFile
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.core.level.LevelType
import pt.tiago.ducknight.progress.Profile
import pt.tiago.ducknight.ui.theme.LocalDuckColors
import pt.tiago.ducknight.ui.theme.LocalDuckShapes
import pt.tiago.ducknight.ui.theme.LocalDuckType

private const val SPECIES_SHOWN = 6

@Composable
fun LevelSheetScreen(
    level: LevelFile,
    difficulty: Difficulty,
    repository: ContentRepository,
    profile: Profile,
    onBack: () -> Unit,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val shapes = LocalDuckShapes.current
    val spec = level.spec(difficulty)
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        TextButton(onClick = onBack) {
            Text(stringResource(R.string.level_back), color = colors.textMuted)
        }
        Text(
            stringResource(R.string.level_number, level.ordinal),
            style = type.placeName,
            color = colors.text,
        )
        Text(placeName(level.place), style = type.caption, color = colors.textMuted)
        Surface(
            shape = RoundedCornerShape(shapes.card),
            color = colors.surface,
            border = BorderStroke(1.dp, colors.line),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(objective(level.type, spec.need, spec.birds), style = type.bodyText, color = colors.text)
                Text(
                    run {
                        val portuguese = LocalConfiguration.current.locales[0].language == "pt"
                        val names = spec.species.map { repository.speciesName(it.id, portuguese) }.distinct()
                        // Late places mix dozens of species: name a few and count the rest.
                        val shown = names.take(SPECIES_SHOWN).joinToString(" · ")
                        val rest = names.size - SPECIES_SHOWN
                        stringResource(R.string.species_in_level, shown) +
                            if (rest > 0) " " + stringResource(R.string.species_more, rest) else ""
                    },
                    style = type.caption,
                    color = colors.textMuted,
                )
                Text(
                    stringResource(R.string.level_stars, spec.stars.two, spec.stars.three),
                    style = type.caption,
                    color = colors.accent,
                )
                Text(
                    "★".repeat(profile.stars(level.id, difficulty.name)) +
                        "☆".repeat(3 - profile.stars(level.id, difficulty.name)),
                    style = type.bodyText,
                    color = colors.accent,
                )
            }
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onPlay,
            shape = RoundedCornerShape(shapes.button),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) {
            Text(stringResource(R.string.level_play), style = type.button)
        }
    }
}

@Composable
private fun objective(type: LevelType, need: Int, birds: Int): String = when (type) {
    LevelType.BOSS -> stringResource(R.string.level_objective_boss)
    LevelType.FILM -> stringResource(R.string.level_objective_film, need, birds)
    LevelType.PROTECT -> stringResource(R.string.level_objective_protect, need, birds)
    LevelType.FLOCK -> stringResource(R.string.level_objective_flock, need, birds)
    LevelType.TIMED -> stringResource(R.string.level_objective_timed, need)
    LevelType.CLAY -> stringResource(R.string.level_objective_clay, need, birds)
    else -> stringResource(R.string.level_objective_classic, need, birds)
}

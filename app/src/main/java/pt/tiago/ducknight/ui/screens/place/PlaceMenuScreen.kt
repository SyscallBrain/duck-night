package pt.tiago.ducknight.ui.screens.place

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.progress.Profile
import pt.tiago.ducknight.ui.theme.LocalDuckColors
import pt.tiago.ducknight.ui.theme.LocalDuckShapes
import pt.tiago.ducknight.ui.theme.LocalDuckType

@Composable
fun PlaceMenuScreen(
    places: List<Int>,
    profile: Profile,
    difficulty: Difficulty,
    onDifficulty: (Difficulty) -> Unit,
    onBack: () -> Unit,
    onPlace: (Int) -> Unit,
    onDebugUnlock: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val shapes = LocalDuckShapes.current
    val unlocked = places.count { profile.isPlaceUnlocked(it, difficulty.name) }
    LazyColumn(
        modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().padding(top = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.level_back), color = colors.textMuted) }
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.place_menu_title), style = type.placeName, color = colors.text)
            }
        }
        item {
            Text(
                stringResource(R.string.place_menu_progress, unlocked, places.size),
                style = type.caption,
                color = colors.textMuted,
            )
        }
        item {
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
        }
        if (pt.tiago.ducknight.BuildConfig.DEBUG) {
            item {
                TextButton(onClick = onDebugUnlock) {
                    Text(stringResource(R.string.debug_unlock), color = colors.textMuted)
                }
            }
        }
        items(places, key = { it }) { place ->
            val open = profile.isPlaceUnlocked(place, difficulty.name)
            val stars = (1..50).sumOf { profile.stars("p%02dl%03d".format(place, it), difficulty.name) }
            Surface(
                shape = RoundedCornerShape(shapes.card),
                color = colors.surface,
                border = BorderStroke(1.dp, if (open) colors.line else colors.line.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth().height(74.dp).clickable(enabled = open) { onPlace(place) },
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            placeName(place),
                            style = type.cardTitle,
                            color = if (open) colors.text else colors.textMuted,
                        )
                        Text(
                            stringResource(R.string.place_levels),
                            style = type.caption,
                            color = if (open) colors.textMuted else colors.line,
                        )
                    }
                    Text(
                        if (open) "★ $stars" else stringResource(R.string.level_locked),
                        style = type.caption,
                        color = if (open) colors.accent else colors.line,
                    )
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
fun placeName(place: Int): String = when (place) {
    2 -> stringResource(R.string.place_2_name)
    3 -> stringResource(R.string.place_3_name)
    4 -> stringResource(R.string.place_4_name)
    5 -> stringResource(R.string.place_5_name)
    6 -> stringResource(R.string.place_6_name)
    7 -> stringResource(R.string.place_7_name)
    8 -> stringResource(R.string.place_8_name)
    9 -> stringResource(R.string.place_9_name)
    10 -> stringResource(R.string.place_10_name)
    11 -> stringResource(R.string.place_11_name)
    12 -> stringResource(R.string.place_12_name)
    else -> stringResource(R.string.place_title)
}

/** The place name after "of": in Portuguese it carries its article ("da Lagoa da Aldeia"). */
@Composable
fun placeNameOf(place: Int): String = stringResource(
    when (place) {
        2 -> R.string.place_of_2
        3 -> R.string.place_of_3
        4 -> R.string.place_of_4
        5 -> R.string.place_of_5
        6 -> R.string.place_of_6
        7 -> R.string.place_of_7
        8 -> R.string.place_of_8
        9 -> R.string.place_of_9
        10 -> R.string.place_of_10
        11 -> R.string.place_of_11
        12 -> R.string.place_of_12
        else -> R.string.place_of_1
    },
)

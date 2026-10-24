package pt.tiago.ducknight.ui.screens.profile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import pt.tiago.ducknight.R
import pt.tiago.ducknight.ui.art.speciesColor
import pt.tiago.ducknight.ui.art.drawBirdFigure
import androidx.compose.ui.unit.sp
import androidx.compose.ui.geometry.Offset
import pt.tiago.ducknight.catalog.ContentRepository
import pt.tiago.ducknight.progress.PhotoRecord
import pt.tiago.ducknight.progress.Profile
import pt.tiago.ducknight.ui.theme.LocalDuckColors
import pt.tiago.ducknight.ui.theme.LocalDuckShapes
import pt.tiago.ducknight.ui.theme.LocalDuckType

@Composable
fun BirdBookScreen(
    profile: Profile,
    repository: ContentRepository,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val portuguese = LocalConfiguration.current.locales[0].language == "pt"
    val photographed = profile.birds.count { it.value.photographed > 0 }
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.profile_back), color = colors.textMuted) }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.bird_book_title), style = type.screenTitle, color = colors.text)
        }
        Text(
            stringResource(R.string.bird_book_count, photographed, repository.species.size),
            style = type.caption,
            color = colors.textMuted,
        )
        Spacer(Modifier.height(8.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxWidth().weight(1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(repository.species, key = { it.id }) { species ->
                val record = profile.birds[species.id]
                val name = if (portuguese) species.namePt else species.nameEn
                val seen = (record?.seen ?: 0) > 0
                Surface(
                    shape = RoundedCornerShape(LocalDuckShapes.current.card),
                    color = colors.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        PhotoThumb(record?.bestPhoto, speciesColor(species.id, species.category), seen)
                        Text(name, style = type.cardTitle.copy(fontSize = 16.sp, lineHeight = 20.sp), color = if (seen) colors.text else colors.textMuted)
                        Text(species.scientific, style = type.caption, color = colors.textMuted)
                        if (record == null || record.seen == 0) {
                            Text(stringResource(R.string.bird_not_seen), style = type.caption, color = colors.line)
                        } else {
                            Text(pluralStringResource(R.plurals.bird_seen, record.seen, record.seen), style = type.caption, color = colors.textMuted)
                            if (record.photographed > 0) {
                                Text(
                                    pluralStringResource(R.plurals.bird_photographed, record.photographed, record.photographed, record.bestQuality),
                                    style = type.caption,
                                    color = colors.good,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoThumb(photo: PhotoRecord?, accent: Color, seen: Boolean) {
    val colors = LocalDuckColors.current
    Box(
        Modifier
            .fillMaxWidth()
            .height(76.dp)
            .background(
                Brush.verticalGradient(
                    if (photo == null) listOf(colors.surface2, colors.surface2) else listOf(Color(0xFF5E2A5C), Color(0xFFC9584A)),
                ),
                RoundedCornerShape(6.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            photo != null -> {
                Canvas(Modifier.fillMaxSize()) {
                    drawBirdFigure(
                        center = Offset(size.width * 0.5f, size.height * 0.55f),
                        unit = size.height * 0.42f,
                        accent = accent,
                        wingUp = photo.wingPhase in 1638..2457,
                    )
                }
                Box(Modifier.fillMaxSize().padding(6.dp), contentAlignment = Alignment.BottomEnd) {
                    Text("${photo.quality}", color = colors.text, style = LocalDuckType.current.caption)
                }
            }
            // Seen but not photographed: only the silhouette.
            seen -> Canvas(Modifier.fillMaxSize()) {
                drawBirdFigure(
                    center = Offset(size.width * 0.5f, size.height * 0.55f),
                    unit = size.height * 0.42f,
                    accent = colors.textMuted.copy(alpha = 0.5f),
                    wingUp = true,
                )
            }
            else -> Text("?", style = LocalDuckType.current.screenTitle, color = colors.textMuted.copy(alpha = 0.5f))
        }
    }
}

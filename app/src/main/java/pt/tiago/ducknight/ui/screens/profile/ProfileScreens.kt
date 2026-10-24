package pt.tiago.ducknight.ui.screens.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pt.tiago.ducknight.R
import pt.tiago.ducknight.catalog.ContentRepository
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.core.level.LevelType
import pt.tiago.ducknight.data.ProfileStore
import pt.tiago.ducknight.progress.AchievementRule
import pt.tiago.ducknight.progress.PhotographerTitle
import pt.tiago.ducknight.progress.Profile
import pt.tiago.ducknight.ui.screens.place.placeName
import pt.tiago.ducknight.ui.screens.place.placeNameOf
import pt.tiago.ducknight.ui.theme.LocalDuckColors
import pt.tiago.ducknight.ui.theme.LocalDuckShapes
import pt.tiago.ducknight.ui.theme.LocalDuckType

@Composable
fun ProfileHubScreen(
    profile: Profile,
    store: ProfileStore,
    onProfile: (Profile) -> Unit,
    onBack: () -> Unit,
    onBirdBook: () -> Unit,
    onBosses: () -> Unit,
    onAchievements: () -> Unit,
    onStats: () -> Unit,
    onCosmetics: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val shapes = LocalDuckShapes.current
    val context = LocalContext.current
    var message by remember { mutableStateOf<String?>(null) }
    var confirmReset by remember { mutableStateOf(false) }
    val exportedMessage = stringResource(R.string.backup_exported)
    val importedMessage = stringResource(R.string.backup_imported)
    val failedMessage = stringResource(R.string.backup_failed)
    val export = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use {
                    it.write(store.export(profile).toByteArray(Charsets.UTF_8))
                }
            }.onSuccess { message = exportedMessage }
                .onFailure { message = failedMessage }
        }
    }
    val import = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            val loaded = runCatching {
                context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
                    ?.let(store::import)
            }.getOrNull()
            if (loaded != null) {
                onProfile(loaded)
                message = importedMessage
            } else {
                message = failedMessage
            }
        }
    }
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.profile_back), color = colors.textMuted) }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.profile_title), style = type.screenTitle, color = colors.text)
        }
        Text(titleName(profile.title()), style = type.placeName, color = colors.accent)
        Text(pluralStringResource(R.plurals.profile_stars, profile.totalStars, profile.totalStars), style = type.caption, color = colors.textMuted)
        Spacer(Modifier.height(6.dp))
        ProfileButton(stringResource(R.string.profile_bird_book), onBirdBook)
        ProfileButton(stringResource(R.string.profile_bosses), onBosses)
        ProfileButton(stringResource(R.string.profile_achievements), onAchievements)
        ProfileButton(stringResource(R.string.profile_stats), onStats)
        ProfileButton(stringResource(R.string.profile_cosmetics), onCosmetics)
        Text(stringResource(R.string.backup_title), style = type.cardTitle, color = colors.text)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { export.launch("duck-night-progress.json") },
                shape = RoundedCornerShape(shapes.button),
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.backup_export)) }
            OutlinedButton(
                onClick = { import.launch(arrayOf("application/json")) },
                shape = RoundedCornerShape(shapes.button),
                modifier = Modifier.weight(1f),
            ) { Text(stringResource(R.string.backup_import)) }
        }
        message?.let { Text(it, style = type.caption, color = colors.good, textAlign = TextAlign.Center) }
        TextButton(onClick = { confirmReset = true }) {
            Text(stringResource(R.string.profile_reset), color = colors.danger)
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            title = { Text(stringResource(R.string.profile_reset)) },
            text = { Text(stringResource(R.string.profile_reset_confirm)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    onProfile(profile.resetCampaign())
                }) { Text(stringResource(R.string.profile_reset), color = colors.danger) }
            },
            dismissButton = {
                TextButton(onClick = { confirmReset = false }) { Text(stringResource(R.string.profile_back)) }
            },
        )
    }
}

@Composable
private fun ProfileButton(label: String, onClick: () -> Unit) {
    val colors = LocalDuckColors.current
    Surface(
        shape = RoundedCornerShape(LocalDuckShapes.current.card),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.line),
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = LocalDuckType.current.bodyText, color = colors.text, modifier = Modifier.weight(1f))
            Text("›", style = LocalDuckType.current.cardTitle, color = colors.accent)
        }
    }
}

@Composable
fun AchievementsScreen(profile: Profile, onBack: () -> Unit) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val rules = AchievementRule.all
    // Earned ones first, then the closest to being earned.
    val ordered = rules.sortedWith(
        compareByDescending<AchievementRule> { it.id in profile.achievements }
            .thenByDescending { it.progress(profile).coerceAtMost(it.target) * 1_000 / it.target },
    )
    Column(
        Modifier.fillMaxSize().background(colors.bg).windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.profile_back), color = colors.textMuted) }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.achievements_title), style = type.screenTitle, color = colors.text)
        }
        Text(
            stringResource(R.string.achievements_count, rules.count { it.id in profile.achievements }, rules.size),
            style = type.caption,
            color = colors.textMuted,
        )
        Spacer(Modifier.height(8.dp))
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (rule in ordered) {
                val unlocked = rule.id in profile.achievements
                val progress = rule.progress(profile).coerceIn(0, rule.target)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surface,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (unlocked) "★" else "☆",
                            color = if (unlocked) colors.accent else colors.line,
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                achievementName(rule),
                                style = type.bodyText,
                                color = if (unlocked) colors.text else colors.textMuted,
                            )
                            if (!unlocked && rule.target > 1) {
                                Box(
                                    Modifier.fillMaxWidth().height(4.dp)
                                        .background(colors.surface2, RoundedCornerShape(2.dp)),
                                ) {
                                    if (progress > 0) {
                                        Box(
                                            Modifier.fillMaxWidth(progress.toFloat() / rule.target).height(4.dp)
                                                .background(colors.accent, RoundedCornerShape(2.dp)),
                                        )
                                    }
                                }
                            }
                        }
                        if (!unlocked && rule.target > 1) {
                            Spacer(Modifier.width(10.dp))
                            Text(
                                stringResource(R.string.achievement_progress, progress, rule.target),
                                style = type.caption,
                                color = colors.textMuted,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatsScreen(profile: Profile, onBack: () -> Unit) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val stats = profile.stats
    val rows = listOf(
        R.string.stats_levels to stats.levelsPlayed,
        R.string.stats_photos to stats.photos,
        R.string.stats_birds to stats.birdsPhotographed,
        R.string.stats_good to stats.goodPhotos,
        R.string.stats_perfect to stats.perfectPhotos,
        R.string.stats_group to stats.groupPhotos,
        R.string.stats_quality to stats.bestQuality,
        R.string.stats_streak to stats.longestGoodStreak,
        R.string.stats_biggest_group to stats.bestGroup,
        R.string.stats_plates to stats.platesBroken,
        R.string.stats_gyro to stats.gyroLevels,
        R.string.stats_trigger to stats.triggerLevels,
    )
    Column(
        Modifier.fillMaxSize().background(colors.bg).windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.profile_back), color = colors.textMuted) }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.stats_title), style = type.screenTitle, color = colors.text)
        }
        for ((id, value) in rows) {
            Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Text(stringResource(id), style = type.bodyText, color = colors.textMuted, modifier = Modifier.weight(1f))
                Text(value.toString(), style = type.hudScore, color = colors.text)
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
            Text(stringResource(R.string.stats_playtime), style = type.bodyText, color = colors.textMuted, modifier = Modifier.weight(1f))
            Text(stringResource(R.string.stats_value_minutes, stats.playTicks / 3_600, stats.playTicks / 60 % 60), style = type.hudScore, color = colors.text)
        }
    }
}

@Composable
fun BossGalleryScreen(profile: Profile, repository: ContentRepository, onBack: () -> Unit) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val bosses = remember(repository) { repository.levels.filter { it.type == LevelType.BOSS } }
    val portuguese = LocalConfiguration.current.locales[0].language == "pt"
    LazyColumn(
        Modifier.fillMaxSize().background(colors.bg).windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text(stringResource(R.string.profile_back), color = colors.textMuted) }
                Spacer(Modifier.weight(1f))
                Text(stringResource(R.string.boss_gallery_title), style = type.screenTitle, color = colors.text)
            }
        }
        items(bosses, key = { it.id }) { level ->
            val record = profile.bosses[level.id]
            val speciesId = level.spec(Difficulty.MEDIUM).species.first().id
            Surface(shape = RoundedCornerShape(10.dp), color = colors.surface) {
                Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(placeName(level.place), style = type.caption, color = colors.textMuted)
                    Text(repository.speciesName(speciesId, portuguese), style = type.cardTitle, color = colors.text)
                    if (record == null) {
                        Text(stringResource(R.string.boss_locked), style = type.caption, color = colors.textMuted)
                    } else {
                        Text(
                            stringResource(R.string.boss_album, record.bestPhotos.size),
                            style = type.caption,
                            color = colors.accent,
                        )
                        for (photo in record.bestPhotos) {
                            Row(Modifier.fillMaxWidth()) {
                                Text(
                                    repository.speciesName(photo.speciesId, portuguese),
                                    style = type.bodyText,
                                    color = colors.text,
                                    modifier = Modifier.weight(1f),
                                )
                                Text("${photo.quality}", style = type.hudScore, color = colors.good)
                            }
                        }
                        Text(
                            stringResource(R.string.boss_best_score, record.bestScore),
                            style = type.caption,
                            color = colors.textMuted,
                        )
                    }
                }
            }
        }
        item { Spacer(Modifier.height(20.dp)) }
    }
}

@Composable
fun CosmeticsScreen(
    profile: Profile,
    onDog: (String) -> Unit,
    onCamera: (String) -> Unit,
    onViewfinder: (String) -> Unit,
    onBack: () -> Unit,
) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    Column(
        Modifier.fillMaxSize().background(colors.bg).windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.profile_back), color = colors.textMuted) }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.cosmetics_title), style = type.screenTitle, color = colors.text)
        }
        Text(stringResource(R.string.cosmetics_dog), style = type.cardTitle, color = colors.text)
        CosmeticOption(dogName("farrusco"), null, profile.selectedDog == "farrusco", true) { onDog("farrusco") }
        CosmeticOption(dogName("pantufa"), stringResource(R.string.cosmetic_locked_perfect), profile.selectedDog == "pantufa", "pantufa" in profile.dogs) { onDog("pantufa") }
        CosmeticOption(dogName("tejo"), stringResource(R.string.cosmetic_locked_boss), profile.selectedDog == "tejo", "tejo" in profile.dogs) { onDog("tejo") }
        Text(stringResource(R.string.cosmetics_camera), style = type.cardTitle, color = colors.text)
        CosmeticOption(cameraName("box"), null, profile.selectedCamera == "box", true) { onCamera("box") }
        CosmeticOption(cameraName("reflex"), titleHint(PhotographerTitle.THIRD), profile.selectedCamera == "reflex", "reflex" in profile.cameras) { onCamera("reflex") }
        CosmeticOption(cameraName("instant"), stringResource(R.string.cosmetic_locked_group), profile.selectedCamera == "instant", "instant" in profile.cameras) { onCamera("instant") }
        CosmeticOption(cameraName("telephoto"), stringResource(R.string.cosmetic_locked_hard_bosses), profile.selectedCamera == "telephoto", "telephoto" in profile.cameras) { onCamera("telephoto") }
        Text(stringResource(R.string.cosmetics_viewfinder), style = type.cardTitle, color = colors.text)
        CosmeticOption(viewfinderName("corners"), null, profile.selectedViewfinder == "corners", true) { onViewfinder("corners") }
        CosmeticOption(viewfinderName("reticle"), titleHint(PhotographerTitle.FIRST), profile.selectedViewfinder == "reticle", "reticle" in profile.viewfinders) { onViewfinder("reticle") }
        CosmeticOption(viewfinderName("circle"), titleHint(PhotographerTitle.NATURE), profile.selectedViewfinder == "circle", "circle" in profile.viewfinders) { onViewfinder("circle") }
        CosmeticOption(viewfinderName("thirds"), titleHint(PhotographerTitle.MASTER), profile.selectedViewfinder == "thirds", "thirds" in profile.viewfinders) { onViewfinder("thirds") }
    }
}

@Composable
private fun titleHint(title: PhotographerTitle): String = stringResource(R.string.cosmetic_locked_title, titleName(title))

/** One row per item: locked items stay visible with what unlocks them. */
@Composable
private fun CosmeticOption(name: String, hint: String?, selected: Boolean, unlocked: Boolean, onSelect: () -> Unit) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) colors.surface2 else colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (selected) colors.accent else colors.line),
        modifier = Modifier.fillMaxWidth().clickable(enabled = unlocked, onClick = onSelect),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(name, style = type.bodyText, color = if (unlocked) colors.text else colors.textMuted)
                if (!unlocked && hint != null) Text(hint, style = type.caption, color = colors.textMuted)
            }
            Text(
                when {
                    selected -> "✓"
                    unlocked -> ""
                    else -> "🔒"
                },
                color = colors.accent,
            )
        }
    }
}

@Composable
fun titleName(title: PhotographerTitle): String = stringResource(
    when (title) {
        PhotographerTitle.APPRENTICE -> R.string.title_apprentice
        PhotographerTitle.AMATEUR -> R.string.title_amateur
        PhotographerTitle.THIRD -> R.string.title_third
        PhotographerTitle.SECOND -> R.string.title_second
        PhotographerTitle.FIRST -> R.string.title_first
        PhotographerTitle.FIELD -> R.string.title_field
        PhotographerTitle.NATURE -> R.string.title_nature
        PhotographerTitle.REPORTER -> R.string.title_reporter
        PhotographerTitle.MASTER -> R.string.title_master
        PhotographerTitle.LEGEND -> R.string.title_legend
    },
)

@Composable
private fun achievementName(rule: AchievementRule): String {
    rule.place?.let { place -> return stringResource(R.string.achievement_boss, placeNameOf(place)) }
    return stringResource(
        when (rule.id) {
            "first_photo" -> R.string.achievement_first_photo
            "first_good" -> R.string.achievement_first_good
            "first_perfect" -> R.string.achievement_first_perfect
            "first_group" -> R.string.achievement_first_group
            "group_3" -> R.string.achievement_group_3
            "group_5" -> R.string.achievement_group_5
            "no_flee" -> R.string.achievement_no_flee
            "good_streak_10" -> R.string.achievement_good_streak_10
            "hard_places" -> R.string.achievement_hard_places
            "birds_10" -> R.string.achievement_birds_10
            "birds_20" -> R.string.achievement_birds_20
            "birds_30" -> R.string.achievement_birds_30
            "book_complete" -> R.string.achievement_book_complete
            "perfect_25" -> R.string.achievement_perfect_25
            "perfect_100" -> R.string.achievement_perfect_100
            "clays_100" -> R.string.achievement_clays_100
            "clays_500" -> R.string.achievement_clays_500
            "gallery_40" -> R.string.achievement_gallery_40
            "arcade_a_10" -> R.string.achievement_arcade_a_10
            "arcade_b_10" -> R.string.achievement_arcade_b_10
            "arcade_c_10" -> R.string.achievement_arcade_c_10
            "daily_7" -> R.string.achievement_daily_7
            "daily_30" -> R.string.achievement_daily_30
            "gyro_level" -> R.string.achievement_gyro_level
            "trigger_level" -> R.string.achievement_trigger_level
            "protect_20" -> R.string.achievement_protect_20
            "golden_p01" -> R.string.achievement_golden_p01
            "duel_sweep" -> R.string.achievement_duel_sweep
            "ten_levels" -> R.string.achievement_ten_levels
            "levels_100" -> R.string.achievement_levels_100
            "stars_100" -> R.string.achievement_stars_100
            "stars_1000" -> R.string.achievement_stars_1000
            else -> R.string.achievement_three_stars_50
        },
    )
}

@Composable
private fun dogName(id: String): String = stringResource(
    when (id) {
        "pantufa" -> R.string.dog_pantufa
        "tejo" -> R.string.dog_tejo
        else -> R.string.dog_farrusco
    },
)

@Composable
private fun cameraName(id: String): String = stringResource(
    when (id) {
        "reflex" -> R.string.camera_reflex
        "instant" -> R.string.camera_instant
        "telephoto" -> R.string.camera_telephoto
        else -> R.string.camera_box
    },
)

@Composable
private fun viewfinderName(id: String): String = stringResource(
    when (id) {
        "reticle" -> R.string.viewfinder_reticle
        "circle" -> R.string.viewfinder_circle
        "thirds" -> R.string.viewfinder_thirds
        else -> R.string.viewfinder_corners
    },
)

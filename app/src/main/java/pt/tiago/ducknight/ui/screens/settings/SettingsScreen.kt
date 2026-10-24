package pt.tiago.ducknight.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import pt.tiago.ducknight.R
import pt.tiago.ducknight.data.AimMode
import pt.tiago.ducknight.data.Settings
import pt.tiago.ducknight.data.SettingsRepository
import pt.tiago.ducknight.ui.theme.ColorblindPalette
import pt.tiago.ducknight.ui.theme.ThemeId
import pt.tiago.ducknight.i18n.AppLanguage
import pt.tiago.ducknight.input.GyroAim
import pt.tiago.ducknight.ui.theme.LocalDuckColors
import pt.tiago.ducknight.ui.theme.LocalDuckType

@Composable
fun SettingsScreen(
    settings: Settings,
    repository: SettingsRepository,
    onBack: () -> Unit,
    onCalibrate: () -> Unit,
    onAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val gyroAvailable = GyroAim.available(context)
    fun update(transform: (Settings) -> Settings) {
        scope.launch { repository.update(transform) }
    }
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.settings_back), color = colors.textMuted)
            }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.settings_title), style = type.screenTitle, color = colors.text)
        }
        Text(stringResource(R.string.settings_language), style = type.cardTitle, color = colors.accent)
        val language = remember { AppLanguage.current(context) }
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for ((tag, label) in listOf(
                AppLanguage.SYSTEM to stringResource(R.string.language_system),
                AppLanguage.ENGLISH to stringResource(R.string.language_english),
                AppLanguage.PORTUGUESE to stringResource(R.string.language_portuguese),
            )) {
                FilterChip(
                    selected = language == tag,
                    onClick = {
                        if (tag != language) (context as? android.app.Activity)?.let { AppLanguage.set(it, tag) }
                    },
                    label = { Text(label, style = type.caption) },
                )
            }
        }
        Text(stringResource(R.string.home_theme_switcher), style = type.cardTitle, color = colors.accent)
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (id in ThemeId.entries) {
                FilterChip(
                    selected = settings.theme == id.name,
                    onClick = { update { it.copy(theme = id.name) } },
                    label = {
                        Text(
                            stringResource(
                                when (id) {
                                    ThemeId.CASSETTE -> R.string.theme_cassette
                                    ThemeId.LAGOON -> R.string.theme_lagoon
                                    ThemeId.PAPER -> R.string.theme_paper
                                    ThemeId.FAIR -> R.string.theme_fair
                                },
                            ),
                            style = type.caption,
                        )
                    },
                )
            }
        }
        Text(stringResource(R.string.settings_aiming), style = type.cardTitle, color = colors.accent)
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AimChip(AimMode.TAP, settings, { transform -> update(transform) }, stringResource(R.string.aim_tap))
            if (gyroAvailable) {
                AimChip(AimMode.GYRO, settings, { transform -> update(transform) }, stringResource(R.string.aim_gyro))
            }
            AimChip(AimMode.AIM_TRIGGER, settings, { transform -> update(transform) }, stringResource(R.string.aim_trigger))
        }
        if (!gyroAvailable) {
            Text(stringResource(R.string.settings_aiming_note), style = type.caption, color = colors.textMuted)
        }
        SliderRow(
            label = stringResource(R.string.settings_gyro_gain, settings.gyroGainPercent),
            value = settings.gyroGainPercent.toFloat(),
            range = 60f..160f,
            steps = 19,
        ) { value -> update { it.copy(gyroGainPercent = value.toInt()) } }
        SliderRow(
            label = stringResource(R.string.settings_tolerance, settings.tolerancePercent),
            value = settings.tolerancePercent.toFloat(),
            range = 85f..125f,
            steps = 7,
        ) { value -> update { it.copy(tolerancePercent = value.toInt()) } }
        SliderRow(
            label = stringResource(R.string.settings_speed, settings.gameSpeedPercent),
            value = settings.gameSpeedPercent.toFloat(),
            range = 70f..100f,
            steps = 5,
        ) { value -> update { it.copy(gameSpeedPercent = value.toInt()) } }
        ToggleRow(stringResource(R.string.settings_aim_assist), settings.aimAssist) { checked ->
            update { it.copy(aimAssist = checked) }
        }
        ToggleRow(stringResource(R.string.settings_bigger_birds), settings.biggerBirds) { checked ->
            update { it.copy(biggerBirds = checked) }
        }
        ToggleRow(stringResource(R.string.settings_left_handed), settings.leftHanded) { checked ->
            update { it.copy(leftHanded = checked) }
        }
        ToggleRow(stringResource(R.string.settings_long_press), settings.advanceLongPress) { checked ->
            update { it.copy(advanceLongPress = checked) }
        }
        Text(stringResource(R.string.settings_accessibility), style = type.cardTitle, color = colors.accent)
        ToggleRow(stringResource(R.string.settings_high_contrast), settings.highContrast) { checked ->
            update { it.copy(highContrast = checked) }
        }
        ToggleRow(stringResource(R.string.settings_large_text), settings.largeText) { checked ->
            update { it.copy(largeText = checked) }
        }
        ToggleRow(stringResource(R.string.settings_follow_system_motion), settings.followSystemMotion) { checked ->
            update { it.copy(followSystemMotion = checked) }
        }
        ToggleRow(stringResource(R.string.settings_reduced_motion), settings.reducedMotion) { checked ->
            update { it.copy(reducedMotion = checked) }
        }
        Text(stringResource(R.string.settings_palette), style = type.cardTitle, color = colors.text)
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            for (palette in ColorblindPalette.entries) {
                FilterChip(
                    selected = settings.palette == palette.name,
                    onClick = { update { it.copy(palette = palette.name) } },
                    label = {
                        Text(
                            stringResource(
                                when (palette) {
                                    ColorblindPalette.NORMAL -> R.string.palette_normal
                                    ColorblindPalette.DEUTERANOPIA -> R.string.palette_deuteranopia
                                    ColorblindPalette.PROTANOPIA -> R.string.palette_protanopia
                                    ColorblindPalette.TRITANOPIA -> R.string.palette_tritanopia
                                },
                            ),
                            style = type.caption,
                        )
                    },
                )
            }
        }
        Text(stringResource(R.string.settings_palette_note), style = type.caption, color = colors.textMuted)
        Button(
            onClick = onCalibrate,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text(stringResource(R.string.settings_calibrate)) }
        Text(stringResource(R.string.settings_aiming_note), style = type.caption, color = colors.textMuted)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.settings_sound, (settings.sfxVolume * 100).toInt()), style = type.bodyText, color = colors.text)
            Slider(
                value = settings.sfxVolume,
                onValueChange = { value -> update { it.copy(sfxVolume = value) } },
                modifier = Modifier.weight(1f),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(stringResource(R.string.settings_music, (settings.musicVolume * 100).toInt()), style = type.bodyText, color = colors.text)
            Slider(
                value = settings.musicVolume,
                onValueChange = { value -> update { it.copy(musicVolume = value) } },
                modifier = Modifier.weight(1f),
            )
        }
        ToggleRow(stringResource(R.string.settings_vibration), settings.vibration) { checked ->
            update { it.copy(vibration = checked) }
        }
        ToggleRow(stringResource(R.string.settings_dog_laugh), settings.dogLaugh) { checked ->
            update { it.copy(dogLaugh = checked) }
        }
        TextButton(onClick = onAbout) {
            Text(stringResource(R.string.settings_about), color = colors.accent)
        }
    }
}

@Composable
private fun AimChip(
    mode: AimMode,
    settings: Settings,
    update: ((Settings) -> Settings) -> Unit,
    label: String,
) {
    FilterChip(
        selected = settings.aimMode == mode,
        onClick = { update { it.copy(aimMode = mode) } },
        label = { Text(label) },
    )
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    steps: Int,
    onChange: (Float) -> Unit,
) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = type.caption, color = colors.textMuted)
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            steps = steps,
            colors = SliderDefaults.colors(activeTickColor = Color.Transparent, inactiveTickColor = Color.Transparent),
        )
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(label, style = type.bodyText, color = colors.text, modifier = Modifier.weight(1f))
        Switch(
            checked = checked,
            onCheckedChange = onChange,
            colors = SwitchDefaults.colors(
                uncheckedThumbColor = colors.textMuted,
                uncheckedTrackColor = colors.surface2,
                uncheckedBorderColor = colors.line,
            ),
        )
    }
}

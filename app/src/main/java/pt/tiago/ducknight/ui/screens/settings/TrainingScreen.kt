package pt.tiago.ducknight.ui.screens.settings

import androidx.compose.foundation.background
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
import pt.tiago.ducknight.data.AimMode
import pt.tiago.ducknight.data.Settings
import pt.tiago.ducknight.ui.theme.LocalDuckColors
import pt.tiago.ducknight.ui.theme.LocalDuckShapes
import pt.tiago.ducknight.ui.theme.LocalDuckType

@Composable
fun TrainingScreen(
    settings: Settings,
    onMode: (AimMode) -> Unit,
    onStart: () -> Unit,
    onCalibrate: () -> Unit,
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
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.settings_back), color = colors.textMuted)
            }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.training_title), style = type.screenTitle, color = colors.text)
        }
        Surface(
            shape = androidx.compose.foundation.shape.RoundedCornerShape(shapes.card),
            color = colors.surface,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.training_body), style = type.bodyText, color = colors.text)
                Text(stringResource(R.string.training_hint), style = type.caption, color = colors.textMuted)
            }
        }
        Text(stringResource(R.string.training_mode), style = type.label, color = colors.textMuted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = settings.aimMode == AimMode.TAP,
                onClick = { onMode(AimMode.TAP) },
                label = { Text(stringResource(R.string.aim_tap)) },
            )
            FilterChip(
                selected = settings.aimMode == AimMode.GYRO,
                onClick = { onMode(AimMode.GYRO) },
                label = { Text(stringResource(R.string.aim_gyro)) },
            )
            FilterChip(
                selected = settings.aimMode == AimMode.AIM_TRIGGER,
                onClick = { onMode(AimMode.AIM_TRIGGER) },
                label = { Text(stringResource(R.string.aim_trigger)) },
            )
        }
        TextButton(onClick = onCalibrate) {
            Text(stringResource(R.string.settings_calibrate), color = colors.protected)
        }
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onStart,
            shape = androidx.compose.foundation.shape.RoundedCornerShape(shapes.button),
            modifier = Modifier.fillMaxWidth().height(56.dp),
        ) { Text(stringResource(R.string.training_start), style = type.button) }
    }
}

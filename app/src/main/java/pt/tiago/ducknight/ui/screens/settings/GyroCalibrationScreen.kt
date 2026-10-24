package pt.tiago.ducknight.ui.screens.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.tiago.ducknight.R
import pt.tiago.ducknight.input.GyroAim
import pt.tiago.ducknight.ui.theme.LocalDuckColors
import pt.tiago.ducknight.ui.theme.LocalDuckType

@Composable
fun GyroCalibrationScreen(
    gainPercent: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val context = LocalContext.current
    val gyro = remember { GyroAim(context, gainPercent / 100f) }
    val available = remember { GyroAim.available(context) }
    DisposableEffect(Unit) {
        if (available) {
            gyro.start()
            gyro.recenter()
        }
        onDispose { gyro.stop() }
    }
    Column(
        modifier
            .fillMaxSize()
            .background(colors.bg)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 18.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.settings_back), color = colors.textMuted)
            }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.calibration_title), style = type.screenTitle, color = colors.text)
        }
        Text(stringResource(R.string.calibration_body), style = type.bodyText, color = colors.textMuted)
        if (!available) {
            Text(stringResource(R.string.settings_aiming_note), style = type.caption, color = colors.danger)
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .aspectRatio(0.8f)
                .background(colors.surface),
        ) {
            val targets = listOf(
                0.12f to 0.12f,
                0.88f to 0.12f,
                0.12f to 0.88f,
                0.88f to 0.88f,
            )
            for ((fx, fy) in targets) {
                drawCircle(
                    colors.accent,
                    radius = size.minDimension * 0.055f,
                    center = Offset(size.width * fx, size.height * fy),
                    style = Stroke(width = size.minDimension * 0.012f),
                )
            }
            if (available) {
                drawCircle(
                    colors.viewfinder,
                    radius = size.minDimension * 0.018f,
                    center = Offset(
                        (gyro.aimX / 240f) * size.width,
                        (gyro.aimY / 300f) * size.height,
                    ),
                )
            }
        }
        Text(
            stringResource(R.string.calibration_drift, gyro.driftDegrees.toInt()),
            style = type.caption,
            color = colors.textMuted,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { gyro.recenter() },
                modifier = Modifier.weight(1f).height(48.dp),
            ) { Text(stringResource(R.string.calibration_center)) }
        }
    }
}

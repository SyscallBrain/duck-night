package pt.tiago.ducknight.ui.screens.settings

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.tiago.ducknight.R
import pt.tiago.ducknight.ui.theme.LocalDuckColors
import pt.tiago.ducknight.ui.theme.LocalDuckType

@Composable
fun AboutScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
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
            TextButton(onClick = onBack) { Text(stringResource(R.string.level_back), color = colors.textMuted) }
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.about_title), style = type.screenTitle, color = colors.text)
        }
        Text(stringResource(R.string.about_description), style = type.bodyText, color = colors.text)
        Text(stringResource(R.string.about_author), style = type.bodyText, color = colors.accent)
        Text(stringResource(R.string.about_author_email), style = type.bodyText, color = colors.textMuted)
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth().height(48.dp),
        ) { Text(stringResource(R.string.about_close), style = type.button) }
    }
}

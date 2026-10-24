package pt.tiago.ducknight.ui.screens.duel

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pt.tiago.ducknight.R
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.game.DuelGame
import pt.tiago.ducknight.game.DuelMatch
import pt.tiago.ducknight.ui.theme.LocalDuckColors
import pt.tiago.ducknight.ui.theme.LocalDuckShapes
import pt.tiago.ducknight.ui.theme.LocalDuckType

@Composable
fun DuelScreen(
    difficulty: Difficulty,
    onDifficulty: (Difficulty) -> Unit,
    onStart: (String, String, Int, DuelGame, Difficulty) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val shapes = LocalDuckShapes.current
    val defaultFirst = stringResource(R.string.duel_default_one)
    val defaultSecond = stringResource(R.string.duel_default_two)
    var first by rememberSaveable { mutableStateOf(defaultFirst) }
    var second by rememberSaveable { mutableStateOf(defaultSecond) }
    var bestOf by rememberSaveable { mutableIntStateOf(3) }
    var game by rememberSaveable { mutableStateOf(DuelGame.A) }
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
            Text(stringResource(R.string.duel_title), style = type.placeName, color = colors.text)
        }
        Text(stringResource(R.string.duel_setup_caption), style = type.caption, color = colors.textMuted)
        OutlinedTextField(
            value = first,
            onValueChange = { first = it.take(16) },
            label = { Text(stringResource(R.string.duel_player_one)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = second,
            onValueChange = { second = it.take(16) },
            label = { Text(stringResource(R.string.duel_player_two)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.duel_best_of), style = type.cardTitle, color = colors.text)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (value in listOf(3, 5)) {
                FilterChip(
                    selected = bestOf == value,
                    onClick = { bestOf = value },
                    label = { Text(stringResource(R.string.duel_best_of_value, value)) },
                )
            }
        }
        Text(stringResource(R.string.duel_game), style = type.cardTitle, color = colors.text)
        for (value in DuelGame.entries) {
            FilterChip(
                selected = game == value,
                onClick = { game = value },
                label = {
                    Text(
                        when (value) {
                            DuelGame.A -> stringResource(R.string.duel_game_a)
                            DuelGame.B -> stringResource(R.string.duel_game_b)
                            DuelGame.C -> stringResource(R.string.duel_game_c)
                        },
                    )
                },
            )
        }
        Text(stringResource(R.string.level_difficulty), style = type.cardTitle, color = colors.text)
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
        Button(
            onClick = { onStart(first.trim().ifBlank { defaultFirst }, second.trim().ifBlank { defaultSecond }, bestOf, game, difficulty) },
            shape = RoundedCornerShape(shapes.button),
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) { Text(stringResource(R.string.duel_start), style = type.button) }
    }
}

@Composable
fun DuelCurtainScreen(
    match: DuelMatch,
    onContinue: () -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val shapes = LocalDuckShapes.current
    Surface(
        shape = RoundedCornerShape(shapes.card),
        color = colors.surface,
        border = BorderStroke(1.dp, colors.line),
        modifier = modifier.fillMaxSize().padding(20.dp),
    ) {
        Column(
            Modifier.fillMaxSize().padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.weight(1f))
            Text(stringResource(R.string.duel_curtain_title), style = type.screenTitle, color = colors.accent)
            Text(
                stringResource(R.string.duel_curtain_turn, match.playerName(match.currentPlayer)),
                style = type.placeName,
                color = colors.text,
                textAlign = TextAlign.Center,
            )
            Text(
                stringResource(R.string.duel_curtain_round, match.round, match.game.name),
                style = type.caption,
                color = colors.textMuted,
            )
            Text(
                stringResource(
                    R.string.duel_scoreboard,
                    match.playerOneName,
                    match.wins[0],
                    match.playerTwoName,
                    match.wins[1],
                ),
                style = type.bodyText,
                color = colors.text,
            )
            Spacer(Modifier.weight(1f))
            Button(
                onClick = onContinue,
                shape = RoundedCornerShape(shapes.button),
                modifier = Modifier.fillMaxWidth().height(54.dp),
            ) { Text(stringResource(R.string.duel_curtain_continue), style = type.button) }
            OutlinedButton(onClick = onExit, modifier = Modifier.fillMaxWidth().height(46.dp)) {
                Text(stringResource(R.string.duel_leave))
            }
        }
    }
}

@Composable
fun DuelResultScreen(
    match: DuelMatch,
    onRematch: () -> Unit,
    onHome: () -> Unit,
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
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(stringResource(R.string.duel_result_title), style = type.placeName, color = colors.text)
        Spacer(Modifier.weight(1f))
        Text(
            if (match.winner == null) {
                stringResource(R.string.duel_draw)
            } else {
                stringResource(R.string.duel_winner, match.playerName(match.winner!!))
            },
            style = type.screenTitle,
            color = colors.accent,
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(
                R.string.duel_scoreboard,
                match.playerOneName,
                match.wins[0],
                match.playerTwoName,
                match.wins[1],
            ),
            style = type.bodyText,
            color = colors.text,
        )
        Text(
            stringResource(
                R.string.duel_round_score,
                match.playerOneName,
                match.totals[0],
                match.totals[1],
                match.playerTwoName,
            ),
            style = type.caption,
            color = colors.textMuted,
        )
        Spacer(Modifier.weight(1f))
        Button(
            onClick = onRematch,
            shape = RoundedCornerShape(shapes.button),
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) { Text(stringResource(R.string.duel_play_again), style = type.button) }
        OutlinedButton(onClick = onHome, modifier = Modifier.fillMaxWidth().height(46.dp)) {
            Text(stringResource(R.string.duel_home))
        }
    }
}

package pt.tiago.ducknight.ui.screens.game

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlinx.coroutines.isActive
import pt.tiago.ducknight.R
import pt.tiago.ducknight.core.content.LevelFile
import pt.tiago.ducknight.core.engine.RoundPhase
import pt.tiago.ducknight.core.engine.RoundResult
import pt.tiago.ducknight.core.level.LevelType
import pt.tiago.ducknight.core.replay.FailureReplay
import pt.tiago.ducknight.data.AimMode
import pt.tiago.ducknight.data.Settings
import pt.tiago.ducknight.game.GameSession
import pt.tiago.ducknight.input.GyroAim
import pt.tiago.ducknight.ui.theme.DuckColors
import pt.tiago.ducknight.ui.theme.DuckShapes
import pt.tiago.ducknight.ui.theme.DuckType
import pt.tiago.ducknight.ui.theme.LocalDuckColors
import pt.tiago.ducknight.ui.theme.LocalDuckShapes
import pt.tiago.ducknight.ui.theme.LocalDuckType

private const val TICK_NANOS = 1_000_000_000L / 60L

@Composable
fun GameScreen(
    session: GameSession,
    level: LevelFile,
    settings: Settings,
    dogId: String = "farrusco",
    viewfinder: String = "corners",
    highContrast: Boolean = false,
    onExit: () -> Unit,
    onFinished: (RoundResult) -> Unit,
    onReplay: (FailureReplay) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val shapes = LocalDuckShapes.current
    var paused by remember { mutableStateOf(session.paused) }
    // Tutorials teach the first levels; every boss is introduced by a card before its level.
    val introKey = level.tutorial ?: if (level.type == LevelType.BOSS) "boss_${level.place}" else null
    var showTutorial by rememberSaveable { mutableStateOf(introKey != null) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnExit = rememberUpdatedState(onExit)
    val lastResult = session.state.results.lastOrNull()
    val announcement = when {
        session.state.phase == RoundPhase.FINISHED -> {
            val summary = session.state.summary
            if (summary?.passed == true) {
                stringResource(R.string.game_announce_passed, summary.photographed, summary.need)
            } else {
                stringResource(R.string.game_announce_failed)
            }
        }
        lastResult == null -> ""
        lastResult.photographed -> stringResource(R.string.game_announce_photo, lastResult.quality)
        else -> stringResource(R.string.game_announce_fled)
    }
    val context = LocalContext.current
    val gyro = remember(settings.aimMode) {
        if (settings.aimMode == AimMode.GYRO) GyroAim(context, settings.gyroGainPercent / 100f) else null
    }
    val effectiveMode = if (settings.aimMode == AimMode.GYRO && gyro?.available != true) AimMode.TAP else settings.aimMode
    var triggerX by remember { mutableFloatStateOf(120f) }
    var triggerY by remember { mutableFloatStateOf(150f) }
    val aimPoint = when (effectiveMode) {
        AimMode.TAP -> null
        AimMode.GYRO -> gyro?.let { it.aimX.toInt() to it.aimY.toInt() }
        AimMode.AIM_TRIGGER -> triggerX.toInt() to triggerY.toInt()
    }
    val onTap: ((Int, Int) -> Unit)? = when (effectiveMode) {
        AimMode.TAP -> { x, y -> session.queueSnap(x, y) }
        AimMode.GYRO -> { _, _ -> aimPoint?.let { session.queueSnap(it.first, it.second) } }
        AimMode.AIM_TRIGGER -> null
    }
    DisposableEffect(gyro, paused) {
        if (gyro != null && !paused) {
            gyro.start()
            gyro.recenter()
        } else {
            gyro?.stop()
        }
        onDispose { gyro?.stop() }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) {
                session.pause()
                paused = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(session.state.phase) {
        if (session.state.phase == RoundPhase.FINISHED) {
            session.state.summary?.let(onFinished)
        }
    }

    LaunchedEffect(paused, showTutorial) {
        if (paused || showTutorial) return@LaunchedEffect
        var last = withFrameNanos { it }
        var accumulator = 0L
        val speed = settings.gameSpeedPercent.coerceIn(70, 100)
        while (isActive) {
            withFrameNanos { now ->
                accumulator += (now - last).coerceAtLeast(0L) * speed / 100L
                last = now
                var steps = 0
                while (accumulator >= TICK_NANOS && steps < 4) {
                    session.tick()
                    accumulator -= TICK_NANOS
                    steps++
                }
                if (steps == 4) accumulator = 0L
            }
        }
    }

    BackHandler {
        if (paused) {
            currentOnExit.value()
        } else {
            session.pause()
            paused = true
        }
    }

    val fair = level.type == LevelType.CLAY
    val night = pt.tiago.ducknight.core.level.Modifier.NIGHT in session.spec.modifiers
    val edgeTop = if (night) NightEdge else sceneFor(if (fair) 2 else session.spec.place).sky.first()
    val edgeBottom = when {
        night -> NightEdge
        fair -> FairCounter
        else -> BankColor
    }
    Box(
        modifier
            .fillMaxSize()
            // The sky above and the bank below continue the scene to the screen edges.
            .background(
                Brush.verticalGradient(
                    0f to edgeTop,
                    0.5f to edgeTop,
                    0.5f to edgeBottom,
                    1f to edgeBottom,
                ),
            ),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The field runs edge to edge so its scene meets the screen sides; only the bars are inset.
            Box(Modifier.padding(horizontal = 12.dp)) {
                GameHud(session = session, level = level, onPause = {
                    session.pause()
                    paused = true
                })
            }
            BoxWithConstraints(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val availableWidth = maxWidth.value
                val availableHeight = maxHeight.value
                val fieldWidth = minOf(availableWidth, availableHeight * 0.8f).dp
                Surface(
                    shape = RectangleShape,
                    color = edgeTop,
                    modifier = Modifier.width(fieldWidth).height(fieldWidth / 0.8f),
                ) {
                    Box(Modifier.fillMaxSize()) {
                        GameCanvas(
                            session = session,
                            dogLaugh = stringResource(R.string.dog_laugh),
                            dogPhoto = stringResource(R.string.dog_photo),
                            aimPoint = aimPoint,
                            onTap = onTap,
                            dogId = dogId,
                            viewfinder = viewfinder,
                            highContrast = highContrast,
                            reducedMotion = session.reducedMotion,
                            stolenText = stringResource(R.string.boss_stole),
                        )
                        if (effectiveMode == AimMode.AIM_TRIGGER) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .pointerInput(Unit) {
                                        detectDragGestures { change, drag ->
                                            val width = size.width.toFloat().coerceAtLeast(1f)
                                            val height = size.height.toFloat().coerceAtLeast(1f)
                                            triggerX = (triggerX + drag.x / width * 240f * 1.6f).coerceIn(4f, 236f)
                                            triggerY = (triggerY + drag.y / height * 300f * 1.6f).coerceIn(4f, 296f)
                                            change.consume()
                                        }
                                    },
                            )
                        }
                        if (effectiveMode != AimMode.TAP) {
                            Box(
                                Modifier
                                    .align(if (settings.leftHanded) Alignment.BottomStart else Alignment.BottomEnd)
                                    .padding(14.dp)
                                    .size(72.dp)
                                    .background(colors.accent, CircleShape)
                                    .clickable {
                                        aimPoint?.let { session.queueSnap(it.first, it.second) }
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("●", fontSize = 30.sp, color = colors.onAccent)
                            }
                        }
                        if (session.state.extras?.needsAdvance == true) {
                            AdvanceLever(
                                modifier = Modifier
                                    .align(if (settings.leftHanded) Alignment.BottomStart else Alignment.BottomEnd)
                                    .padding(10.dp),
                                longPress = settings.advanceLongPress,
                                onAdvance = session::queueAdvance,
                            )
                        }
                    }
                }
            }
            Box(Modifier.padding(horizontal = 12.dp)) {
                FilmBar(session = session, level = level)
            }
            if (effectiveMode != AimMode.TAP) {
                Text(
                    stringResource(
                        if (effectiveMode == AimMode.GYRO) R.string.game_aim_hint else R.string.game_trigger_hint,
                    ),
                    style = type.caption,
                    color = colors.textMuted,
                )
            }
            if (session.state.extras?.needsAdvance == true) {
                Text(
                    stringResource(R.string.game_advance_hint),
                    style = type.caption,
                    color = colors.accent,
                )
            }
        }

        // Soft flash: never a full-screen 100 % white, maximum two per second in the session.
        val now = System.nanoTime()
        if (now < session.flashUntilNanos) {
            Box(Modifier.fillMaxSize().background(Color.White.copy(alpha = 0.32f)))
        }

        if (announcement.isNotBlank()) {
            Text(
                text = announcement,
                color = Color.Transparent,
                fontSize = 1.sp,
                modifier = Modifier.semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = announcement
                },
            )
        }
        if (paused) {
            PauseOverlay(
                onContinue = {
                    session.resume()
                    paused = false
                },
                onRestart = {
                    session.restart()
                    paused = false
                    showTutorial = introKey != null
                },
                onExit = currentOnExit.value,
                exitLabel = if (CampaignLevel.matches(level.id)) {
                    stringResource(R.string.pause_quit)
                } else {
                    stringResource(R.string.pause_leave)
                },
                // Only offered once a bird has actually fled: before that there is nothing to replay.
                onReplay = if (session.state.results.any { !it.photographed }) {
                    { onReplay(session.buildReplay()) }
                } else {
                    null
                },
            )
        }
        if (showTutorial) {
            TutorialOverlay(
                key = introKey,
                onDismiss = { showTutorial = false },
            )
        }
    }
}

@Composable
private fun GameHud(
    session: GameSession,
    level: LevelFile,
    onPause: () -> Unit,
) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val pauseDescription = stringResource(R.string.game_pause_description)
    Row(
        Modifier.fillMaxWidth().height(48.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        OutlinedButton(
            onClick = onPause,
            shape = RoundedCornerShape(10.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            modifier = Modifier.size(42.dp).semantics { contentDescription = pauseDescription },
        ) {
            Text("Ⅱ", style = type.button)
        }
        Column(Modifier.weight(1f)) {
            Text(
                if (level.id == "training") stringResource(R.string.game_training) else stringResource(R.string.game_level, level.ordinal),
                style = type.hudLevel,
                color = colors.text,
            )
            // A boss counts album pages, not birds.
            val boss = session.state.extras?.boss
            val need = session.state.spec.need
            val done = boss?.pages ?: session.state.birdsPhotographed
            if (need > 0) {
                val reached = done >= need
                Text(
                    stringResource(R.string.game_birds, done, need) +
                        if (reached) " ✓" else "",
                    style = type.caption,
                    color = if (reached) colors.good else colors.textMuted,
                )
            }
        }
        Text(
            stringResource(R.string.game_score, session.state.score),
            style = type.hudScore,
            color = colors.accent,
        )
    }
}

private val FairCounter = Color(0xFF6A3E1E)
private val CampaignLevel = Regex("p\\d{2}l\\d{3}")

@Composable
private fun FilmBar(session: GameSession, level: LevelFile) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val state = session.state
    val extras = state.extras
    Column(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (level.type == LevelType.BOSS) {
                Text(stringResource(R.string.result_boss, extras?.boss?.pages ?: 0), style = type.caption, color = colors.accent)
            } else if (extras?.filmLeft != null) {
                Text(pluralStringResource(R.plurals.game_film_left, extras.filmLeft ?: 0, extras.filmLeft ?: 0), style = type.caption, color = colors.textMuted)
            } else {
                // Unlimited film (training) shows no counter at all.
                if (state.spec.photosPerBird <= 3) {
                    val left = (state.spec.photosPerBird - state.photosUsed).coerceAtLeast(0)
                    Text(pluralStringResource(R.plurals.game_photos_left, left, left), style = type.caption, color = colors.textMuted)
                    repeat(state.spec.photosPerBird) { index ->
                        FilmFrame(filled = index < left)
                    }
                }
            }
            Spacer(Modifier.weight(1f))
            if (extras?.needsAdvance == true) {
                TextButton(onClick = session::queueAdvance) {
                    Text(stringResource(R.string.game_advance), style = type.caption, color = colors.accent)
                }
            }
        }
        if (level.type == LevelType.BOSS) return@Column
        if (level.type == LevelType.CLAY) {
            Text(
                stringResource(R.string.game_plates_count, state.birdsPhotographed, state.spec.need),
                style = type.caption,
                color = colors.textMuted,
            )
            return@Column
        }
        if (state.spec.birds > 10) {
            // A long stream (timed levels, training): a running count instead of one square per bird.
            Text(
                stringResource(R.string.game_birds_count, state.birdsPhotographed),
                style = type.caption,
                color = colors.textMuted,
            )
            return@Column
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            val resolved = state.results.associateBy { it.birdId }
            val active = state.birds.firstOrNull()?.id ?: -1
            for (index in 0 until state.spec.birds.coerceAtMost(10)) {
                val result = resolved[index]
                val filled = result?.photographed == true
                Box(
                    Modifier
                        .size(18.dp)
                        .background(
                            when {
                                filled -> colors.good
                                result != null -> colors.surface2
                                index == active -> colors.accent
                                else -> colors.surface
                            },
                            RoundedCornerShape(3.dp),
                        ),
                )
                if (index + 1 == state.spec.need) {
                    Box(Modifier.width(2.dp).height(18.dp).background(colors.accent))
                }
            }
            Spacer(Modifier.weight(1f))
            Text(
                stringResource(R.string.game_birds, state.birdsPhotographed, state.spec.birds),
                style = type.caption,
                color = colors.textMuted,
            )
        }
    }
}

@Composable
private fun FilmFrame(filled: Boolean) {
    val colors = LocalDuckColors.current
    Box(
        Modifier
            .size(width = 14.dp, height = 18.dp)
            .background(if (filled) colors.accent else colors.surface2, RoundedCornerShape(2.dp)),
    )
}

@Composable
private fun PauseOverlay(
    onContinue: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    exitLabel: String,
    onReplay: (() -> Unit)?,
) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val shapes = LocalDuckShapes.current
    Box(
        Modifier.fillMaxSize().background(colors.bg.copy(alpha = 0.72f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(shapes.sheet),
            color = colors.surface,
            modifier = Modifier.fillMaxWidth(0.84f),
        ) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.pause_title), style = type.screenTitle, color = colors.text)
                Button(
                    onClick = onContinue,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(shapes.button),
                ) { Text(stringResource(R.string.pause_continue)) }
                OutlinedButton(
                    onClick = onRestart,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(shapes.button),
                ) { Text(stringResource(R.string.pause_restart)) }
                if (onReplay != null) {
                    OutlinedButton(
                        onClick = onReplay,
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = RoundedCornerShape(shapes.button),
                    ) { Text(stringResource(R.string.pause_replay)) }
                }
                TextButton(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                    Text(exitLabel, color = colors.textMuted)
                }
            }
        }
    }
}

@Composable
private fun TutorialOverlay(key: String?, onDismiss: () -> Unit) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val shapes = LocalDuckShapes.current
    val text = when (key) {
        "tutorial_1" -> stringResource(R.string.tutorial_1)
        "tutorial_2" -> stringResource(R.string.tutorial_2)
        "tutorial_3" -> stringResource(R.string.tutorial_3)
        "tutorial_4" -> stringResource(R.string.tutorial_4)
        "tutorial_5" -> stringResource(R.string.tutorial_5)
        "tutorial_6" -> stringResource(R.string.tutorial_6)
        "tutorial_7" -> stringResource(R.string.tutorial_7)
        "tutorial_8" -> stringResource(R.string.tutorial_8)
        "tutorial_9" -> stringResource(R.string.tutorial_9)
        "tutorial_10" -> stringResource(R.string.tutorial_10)
        "boss_1" -> stringResource(R.string.boss_intro_1)
        "boss_2" -> stringResource(R.string.boss_intro_2)
        "boss_3" -> stringResource(R.string.boss_intro_3)
        "boss_4" -> stringResource(R.string.boss_intro_4)
        "boss_5" -> stringResource(R.string.boss_intro_5)
        "boss_6" -> stringResource(R.string.boss_intro_6)
        "boss_7" -> stringResource(R.string.boss_intro_7)
        "boss_8" -> stringResource(R.string.boss_intro_8)
        "boss_9" -> stringResource(R.string.boss_intro_9)
        "boss_10" -> stringResource(R.string.boss_intro_10)
        "boss_11" -> stringResource(R.string.boss_intro_11)
        "boss_12" -> stringResource(R.string.boss_intro_12)
        else -> stringResource(R.string.tutorial_1)
    }
    Box(
        Modifier.fillMaxSize().background(colors.bg.copy(alpha = 0.78f)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = RoundedCornerShape(shapes.sheet),
            color = colors.surface,
            modifier = Modifier.fillMaxWidth(0.86f),
        ) {
            Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.tutorial_title), style = type.screenTitle, color = colors.accent)
                Text(text, style = type.bodyText, color = colors.text, textAlign = TextAlign.Start)
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(shapes.button),
                ) { Text(stringResource(R.string.tutorial_ok)) }
            }
        }
    }
}


@Composable
private fun AdvanceLever(
    modifier: Modifier = Modifier,
    longPress: Boolean,
    onAdvance: () -> Unit,
) {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    val shapes = LocalDuckShapes.current
    val threshold = with(LocalDensity.current) { 40.dp.toPx() }
    var accumulated by remember { mutableFloatStateOf(0f) }
    var dragModifier = Modifier.pointerInput(Unit) {
        detectDragGestures(
            onDragStart = { accumulated = 0f },
            onDrag = { change, drag ->
                accumulated += drag.y
                change.consume()
            },
            onDragEnd = { if (abs(accumulated) >= threshold) onAdvance() },
        )
    }
    if (longPress) {
        dragModifier = dragModifier.pointerInput(Unit) {
            detectTapGestures(onLongPress = { onAdvance() })
        }
    }
    Surface(
        shape = RoundedCornerShape(shapes.button),
        color = colors.surface.copy(alpha = 0.8f),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.accent),
        modifier = modifier.size(width = 52.dp, height = 84.dp).then(dragModifier),
    ) {
        Column(
            Modifier.fillMaxSize().padding(6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("↑", style = type.screenTitle, color = colors.accent)
            Text(stringResource(R.string.game_advance_short), style = type.caption.copy(fontSize = 10.sp), color = colors.textMuted, maxLines = 1)
        }
    }
}

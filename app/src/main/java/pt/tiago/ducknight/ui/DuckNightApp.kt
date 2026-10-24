package pt.tiago.ducknight.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.toMutableStateList
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import pt.tiago.ducknight.AppContainer
import pt.tiago.ducknight.audio.MusicTrack
import pt.tiago.ducknight.R
import pt.tiago.ducknight.catalog.ContentRepository
import pt.tiago.ducknight.core.content.DailyRound
import pt.tiago.ducknight.core.content.LevelFile
import pt.tiago.ducknight.core.engine.BirdResult
import pt.tiago.ducknight.core.engine.RoundResult
import pt.tiago.ducknight.core.level.Difficulty
import pt.tiago.ducknight.core.level.GameOptions
import pt.tiago.ducknight.core.level.LevelSpec
import pt.tiago.ducknight.core.level.LevelType
import pt.tiago.ducknight.core.replay.FailureReplay
import pt.tiago.ducknight.progress.BossRecord
import pt.tiago.ducknight.progress.DailyRecord
import pt.tiago.ducknight.progress.Profile
import pt.tiago.ducknight.data.AimMode
import pt.tiago.ducknight.data.Settings
import pt.tiago.ducknight.game.DuelGame
import pt.tiago.ducknight.game.DuelMatch
import pt.tiago.ducknight.game.GameSession
import pt.tiago.ducknight.ui.screens.arcade.ArcadeScreen
import pt.tiago.ducknight.ui.screens.daily.DailyScreen
import pt.tiago.ducknight.ui.screens.duel.DuelCurtainScreen
import pt.tiago.ducknight.ui.screens.duel.DuelResultScreen
import pt.tiago.ducknight.ui.screens.duel.DuelScreen
import pt.tiago.ducknight.ui.screens.fair.FairScreen
import pt.tiago.ducknight.ui.screens.fair.GalleryScreen
import pt.tiago.ducknight.ui.screens.game.GameScreen
import pt.tiago.ducknight.ui.screens.home.HomeProgress
import pt.tiago.ducknight.ui.screens.home.HomeScreen
import pt.tiago.ducknight.ui.screens.place.placeName
import pt.tiago.ducknight.ui.screens.place.LevelSheetScreen
import pt.tiago.ducknight.ui.screens.place.PlaceMenuScreen
import pt.tiago.ducknight.ui.screens.place.PlaceScreen
import pt.tiago.ducknight.ui.screens.profile.AchievementsScreen
import pt.tiago.ducknight.ui.screens.profile.BirdBookScreen
import pt.tiago.ducknight.ui.screens.profile.BossGalleryScreen
import pt.tiago.ducknight.ui.screens.profile.CosmeticsScreen
import pt.tiago.ducknight.ui.screens.profile.ProfileHubScreen
import pt.tiago.ducknight.ui.screens.profile.StatsScreen
import pt.tiago.ducknight.ui.screens.profile.titleName
import pt.tiago.ducknight.ui.screens.replay.ReplayScreen
import pt.tiago.ducknight.ui.screens.result.ResultScreen
import pt.tiago.ducknight.ui.screens.settings.AboutScreen
import pt.tiago.ducknight.ui.screens.settings.GyroCalibrationScreen
import pt.tiago.ducknight.ui.screens.settings.SettingsScreen
import pt.tiago.ducknight.ui.screens.settings.TrainingScreen
import pt.tiago.ducknight.ui.theme.ColorblindPalette
import pt.tiago.ducknight.ui.theme.DuckTheme
import pt.tiago.ducknight.ui.theme.LocalDuckColors
import pt.tiago.ducknight.ui.theme.LocalDuckType
import pt.tiago.ducknight.ui.theme.ThemeId

private const val HOME = "home"
private const val PLACES = "places"
private const val PLACE = "place"
private const val SHEET = "sheet"
private const val GAME = "game"
private const val RESULT = "result"
private const val SETTINGS = "settings"
private const val CALIBRATION = "calibration"
private const val TRAINING = "training"
private const val PROFILE = "profile"
private const val BIRD_BOOK = "bird_book"
private const val BOSSES = "bosses"
private const val ACHIEVEMENTS = "achievements"
private const val STATS = "stats"
private const val COSMETICS = "cosmetics"
private const val DAILY = "daily"
private const val FAIR = "fair"
private const val GALLERY = "gallery"
private const val ARCADE = "arcade"
private const val ABOUT = "about"
private const val REPLAY = "replay"
private const val DUEL = "duel"
private const val DUEL_CURTAIN = "duel_curtain"
private const val DUEL_RESULT = "duel_result"

@Composable
fun DuckNightApp(container: AppContainer) {
    val settings by container.settings.settings.collectAsState(initial = Settings())
    val themeId = runCatching { ThemeId.valueOf(settings.theme) }.getOrDefault(ThemeId.LAGOON)
    val palette = runCatching { ColorblindPalette.valueOf(settings.palette) }
        .getOrDefault(ColorblindPalette.NORMAL)
    val context = LocalContext.current
    val systemReducedMotion = remember {
        runCatching {
            android.provider.Settings.Global.getFloat(
                context.contentResolver,
                android.provider.Settings.Global.ANIMATOR_DURATION_SCALE,
                1f,
            ) == 0f
        }.getOrDefault(false)
    }
    val reducedMotion = settings.reducedMotion || (settings.followSystemMotion && systemReducedMotion)
    val textScale = if (settings.largeText) 1.15f else 1f
    var profile by remember { mutableStateOf(container.profile.load()) }
    val scope = rememberCoroutineScope()
    val content = remember { runCatching { container.content }.getOrNull() }
    if (content == null) {
        DuckTheme(themeId = themeId, palette = palette, textScale = textScale) { ContentError() }
        return
    }
    val saved = remember { container.saved.load() }
    val savedLevel = saved?.let { content.level(it.spec.id) }
    if (saved != null && savedLevel == null) container.saved.clear()

    // A real back stack: Back always returns to the screen the player came from.
    val stack = rememberSaveable(saver = listSaver(save = { it.toList() }, restore = { it.toMutableStateList() })) {
        mutableStateListOf(HOME).apply { if (saved != null && savedLevel != null) add(GAME) }
    }
    val screen = stack.last()
    fun go(target: String) { if (stack.last() != target) stack.add(target) }
    fun replace(target: String) { stack[stack.lastIndex] = target }
    fun back() { if (stack.size > 1) stack.removeAt(stack.lastIndex) }
    fun reset() { stack.clear(); stack.add(HOME) }
    fun backTo(target: String) {
        val index = stack.lastIndexOf(target)
        if (index >= 0) {
            while (stack.lastIndex > index) stack.removeAt(stack.lastIndex)
        } else {
            back()
        }
    }
    var currentPlace by rememberSaveable { mutableStateOf(1) }
    var difficulty by rememberSaveable { mutableStateOf(saved?.spec?.difficulty ?: Difficulty.MEDIUM) }
    var levelFile by remember { mutableStateOf(savedLevel) }
    var session by remember {
        mutableStateOf(
            if (saved != null && savedLevel != null) {
                GameSession(
                    spec = saved.spec,
                    options = saved.options,
                    store = container.saved,
                    audio = container.audio,
                    haptics = container.haptics,
                    dogLaugh = settings.dogLaugh,
                    vibrationEnabled = settings.vibration,
                    camera = profile.selectedCamera,
                    themeName = settings.theme,
                    reducedMotion = reducedMotion,
                    restored = saved,
                )
            } else {
                null
            },
        )
    }
    var selected by remember { mutableStateOf<LevelFile?>(null) }
    var result by remember { mutableStateOf<RoundResult?>(null) }
    var results by remember { mutableStateOf<List<BirdResult>>(emptyList()) }
    var dailyDate by remember { mutableStateOf<String?>(null) }
    var dailyQueue by remember { mutableStateOf<List<DailyRound>>(emptyList()) }
    var dailyScore by remember { mutableStateOf(0) }
    var dailyStars by remember { mutableStateOf(0) }
    var arcadeActive by remember { mutableStateOf(false) }
    var arcadeGame by remember { mutableStateOf("A") }
    var arcadeRound by remember { mutableStateOf(1) }
    var arcadeScore by remember { mutableStateOf(0) }
    var arcadeSeed by remember { mutableStateOf(0L) }
    var replay by remember { mutableStateOf<FailureReplay?>(null) }
    var duelMatch by remember { mutableStateOf<DuelMatch?>(null) }
    var duelActive by remember { mutableStateOf(false) }

    fun startSession(level: LevelFile, spec: LevelSpec, duel: Boolean = false) {
        if (!duel) {
            duelMatch = null
            duelActive = false
        }
        container.saved.clear()
        container.audio.volume = settings.sfxVolume
        val newSession = GameSession(
            spec = spec,
            options = GameOptions(
                tolerancePercent = settings.tolerancePercent,
                biggerBirds = settings.biggerBirds,
                aimAssist = settings.aimAssist && spec.difficulty == Difficulty.EASY,
            ),
            store = container.saved,
            audio = container.audio,
            haptics = container.haptics,
            dogLaugh = settings.dogLaugh,
            vibrationEnabled = settings.vibration,
            camera = profile.selectedCamera,
            themeName = settings.theme,
            reducedMotion = reducedMotion,
        )
        levelFile = level
        currentPlace = level.place
        difficulty = spec.difficulty
        session = newSession
        result = null
        results = emptyList()
        if (screen in setOf(GAME, RESULT, DUEL_CURTAIN, DUEL_RESULT)) replace(GAME) else go(GAME)
    }

    fun startGame(level: LevelFile, chosen: Difficulty) {
        startSession(level, level.spec(chosen))
    }

    fun startDuelRound() {
        val match = duelMatch ?: return
        val spec = when (match.game) {
            DuelGame.B -> LevelSpec.double(
                seed = match.seedForRound(), birds = 10, need = 5, difficulty = match.difficulty,
            )
            DuelGame.C -> LevelSpec.clay(
                seed = match.seedForRound(), pairs = 10, need = 5, difficulty = match.difficulty,
            )
            DuelGame.A -> LevelSpec.classic(
                seed = match.seedForRound(), birds = 10, need = 5, difficulty = match.difficulty,
            )
        }
        val level = LevelFile(
            id = "duel-${match.game.name}",
            place = 1,
            ordinal = match.round,
            type = spec.type,
            family = "duel",
            tags = emptyList(),
            tutorial = null,
            variants = emptyMap(),
        )
        duelActive = true
        startSession(level, spec, duel = true)
    }

    fun startDuel(first: String, second: String, bestOf: Int, game: DuelGame, chosen: Difficulty) {
        duelMatch = DuelMatch(first, second, bestOf, game, chosen)
        difficulty = chosen
        startDuelRound()
    }

    fun startTraining() {
        val trainingSpec = LevelSpec.timed(
            seed = 0x7EA1_1234L,
            seconds = 60,
            need = 0,
            difficulty = Difficulty.EASY,
        ).copy(speedPercent = 50)
        val trainingLevel = LevelFile(
            id = "training",
            place = 1,
            ordinal = 1,
            type = LevelType.TIMED,
            family = "timed-stream",
            tags = emptyList(),
            tutorial = null,
            variants = emptyMap(),
        )
        startSession(trainingLevel, trainingSpec)
    }

    fun startArcadeRound() {
        // Arcade rules: the minimum rises to 10 and the birds get 7 % faster every round.
        val need = minOf(10, 5 + arcadeRound)
        val seed = arcadeSeed + arcadeRound * 7_919L
        val base = when (arcadeGame) {
            "B" -> LevelSpec.double(seed = seed, birds = 10, need = need, difficulty = difficulty)
            "C" -> LevelSpec.clay(seed = seed, pairs = 10, need = need, difficulty = difficulty)
            else -> LevelSpec.classic(seed = seed, birds = 10, need = need, difficulty = difficulty)
        }
        val spec = base.copy(
            speedPercent = (base.speedPercent * (100 + 7 * (arcadeRound - 1)) / 100).coerceAtMost(200),
        )
        val level = LevelFile(
            id = "arcade-$arcadeGame",
            place = 1,
            ordinal = arcadeRound,
            type = spec.type,
            family = "arcade",
            tags = emptyList(),
            tutorial = null,
            variants = emptyMap(),
        )
        arcadeActive = true
        startSession(level, spec)
    }

    fun startArcade(game: String, chosen: Difficulty) {
        arcadeGame = game
        difficulty = chosen
        arcadeRound = 1
        arcadeSeed = System.currentTimeMillis()
        arcadeScore = 0
        startArcadeRound()
    }

    BackHandler(enabled = stack.size > 1 && screen != GAME) {
        when (screen) {
            REPLAY -> { replay = null; back() }
            DUEL_CURTAIN, DUEL_RESULT -> { duelMatch = null; duelActive = false; reset() }
            RESULT -> backTo(PLACE)
            else -> back()
        }
    }

    val musicTrack = when (screen) {
        FAIR, GALLERY -> MusicTrack.FAIR
        GAME, REPLAY -> if (levelFile?.type == LevelType.CLAY) {
            MusicTrack.FAIR
        } else {
            MusicTrack.forPlace(levelFile?.place ?: currentPlace)
        }
        SHEET, PLACE -> MusicTrack.forPlace(currentPlace)
        DAILY, TRAINING -> MusicTrack.PLACE_1
        else -> MusicTrack.HOME
    }
    LaunchedEffect(musicTrack) { container.music.play(musicTrack) }
    LaunchedEffect(settings.musicVolume) { container.music.volume = settings.musicVolume }

    DuckTheme(themeId = themeId, palette = palette, textScale = textScale) {
        when (screen) {
            HOME -> {
                val next = content.levels.firstOrNull { level ->
                    profile.isPlaceUnlocked(level.place, difficulty.name) &&
                        profile.isUnlocked(level.id, level.ordinal, difficulty.name) &&
                        profile.stars(level.id, difficulty.name) == 0
                } ?: content.levels.last()
                val done = content.levels.count { level ->
                    Difficulty.entries.any { profile.stars(level.id, it.name) > 0 }
                }
                HomeScreen(
                    themeId = themeId,
                    progress = HomeProgress(
                        place = next.place,
                        places = content.places.size,
                        placeName = placeName(next.place),
                        nextLevel = next.ordinal,
                        levelsDone = done,
                        levels = content.levels.size,
                        birdsSeen = profile.birds.count { it.value.photographed > 0 },
                        birds = content.species.size,
                    ),
                    onContinue = {
                        currentPlace = next.place
                        selected = next
                        stack.clear()
                        stack.addAll(listOf(HOME, PLACES, PLACE, SHEET))
                    },
                    onSettings = { go(SETTINGS) },
                    onTraining = { go(TRAINING) },
                    onProfile = { go(PROFILE) },
                    onDaily = { go(DAILY) },
                    onBirdBook = { go(BIRD_BOOK) },
                    onFair = { go(FAIR) },
                    onArcade = { go(ARCADE) },
                    onDuel = { go(DUEL) },
                    title = titleName(profile.title()),
                    stars = profile.totalStars,
                )
            }
            DUEL -> DuelScreen(
                difficulty = difficulty,
                onDifficulty = { difficulty = it },
                onStart = { first, second, bestOf, game, chosen ->
                    startDuel(first, second, bestOf, game, chosen)
                },
                onBack = { back() },
            )
            DUEL_CURTAIN -> {
                val match = duelMatch
                if (match == null) {
                    reset()
                } else {
                    DuelCurtainScreen(
                        match = match,
                        onContinue = { startDuelRound() },
                        onExit = {
                            duelMatch = null
                            duelActive = false
                            reset()
                        },
                    )
                }
            }
            DUEL_RESULT -> {
                val match = duelMatch
                if (match == null) {
                    reset()
                } else {
                    DuelResultScreen(
                        match = match,
                        onRematch = {
                            duelMatch = match.newMatch()
                            startDuelRound()
                        },
                        onHome = {
                            duelMatch = null
                            duelActive = false
                            reset()
                        },
                    )
                }
            }
            REPLAY -> {
                val evidence = replay
                if (evidence == null) {
                    reset()
                } else {
                    ReplayScreen(
                        evidence,
                        onBack = { back() },
                        dogId = profile.selectedDog,
                        viewfinder = profile.selectedViewfinder,
                        highContrast = settings.highContrast,
                    )
                }
            }
            PLACES -> PlaceMenuScreen(
                places = content.places,
                profile = profile,
                difficulty = difficulty,
                onDifficulty = { difficulty = it },
                onBack = { back() },
                onPlace = { place ->
                    currentPlace = place
                    go(PLACE)
                },
                onDebugUnlock = {
                    val unlocked = profile.levels.toMutableMap()
                    for (level in content.levels) {
                        for (diff in Difficulty.entries) {
                            val key = profile.levelKey(level.id, diff.name)
                            if ((unlocked[key]?.stars ?: 0) < 1) {
                                unlocked[key] = pt.tiago.ducknight.progress.LevelProgress(stars = 1)
                            }
                        }
                    }
                    profile = profile.copy(levels = unlocked)
                    container.profile.save(profile)
                },
            )
            PLACE -> PlaceScreen(
                place = currentPlace,
                levels = content.levelsForPlace(currentPlace),
                profile = profile,
                difficulty = difficulty,
                onDifficulty = { difficulty = it },
                onBack = { back() },
                onLevel = {
                    selected = it
                    go(SHEET)
                },
            )
            SHEET -> {
                val level = selected
                if (level == null) {
                    back()
                } else {
                    LevelSheetScreen(
                        level = level,
                        difficulty = difficulty,
                        repository = content,
                        profile = profile,
                        onBack = { back() },
                        onPlay = { startGame(level, difficulty) },
                    )
                }
            }
            GAME -> {
                val activeSession = session
                val level = levelFile
                if (activeSession == null || level == null) {
                    reset()
                } else key(activeSession) {
                    // A fresh slot per session: pause, tutorial and gesture state never leak
                    // from the previous level after Next or Repeat.
                    GameScreen(
                        session = activeSession,
                        level = level,
                        settings = settings,
                        dogId = profile.selectedDog,
                        viewfinder = profile.selectedViewfinder,
                        highContrast = settings.highContrast,
                        onExit = {
                            duelActive = false
                            duelMatch = null
                            if (level.id.startsWith("duel-")) backTo(DUEL) else backTo(PLACE)
                        },
                        onReplay = { evidence ->
                            replay = evidence
                            go(REPLAY)
                        },
                        onFinished = {
                            val match = duelMatch
                            if (match != null && duelActive) {
                                match.record(it.score)
                                duelActive = false
                                if (match.over && match.wins.max() >= 3 && match.wins.min() == 0) {
                                    profile = profile.recordDuelSweep()
                                    container.profile.save(profile)
                                }
                                replace(if (match.over) DUEL_RESULT else DUEL_CURTAIN)
                                return@GameScreen
                            }
                            if (level.id != "training" && !level.id.startsWith("fair-") && !arcadeActive) {
                                profile = profile.recordResult(
                                    levelId = level.id,
                                    ordinal = level.ordinal,
                                    place = level.place,
                                    difficulty = difficulty.name,
                                    stars = it.stars,
                                    score = it.score,
                                    summary = it,
                                    photos = activeSession.photoRecords(),
                                    seenSpecies = activeSession.seenSpecies(),
                                    aimMode = settings.aimMode.name,
                                    round = activeSession.roundExtras(level.type == LevelType.PROTECT),
                                )
                                dailyDate?.let { date ->
                                    dailyScore += it.score
                                    dailyStars += it.stars
                                    if (dailyQueue.isEmpty()) {
                                        profile = profile.copy(
                                            dailyHistory = (profile.dailyHistory + DailyRecord(date, dailyScore, dailyStars))
                                                .takeLast(270),
                                        )
                                        dailyDate = null
                                        dailyScore = 0
                                        dailyStars = 0
                                    }
                                }
                                container.profile.save(profile)
                            }
                            if (level.id.startsWith("fair-") || arcadeActive) {
                                profile = profile.recordPlay(
                                    summary = it,
                                    photos = activeSession.photoRecords(),
                                    seenSpecies = activeSession.seenSpecies(),
                                    aimMode = settings.aimMode.name,
                                    round = activeSession.roundExtras(protectLevel = false),
                                )
                                if (level.id == "fair-clay") profile = profile.recordClay(difficulty.name, it.score)
                            }
                            if (arcadeActive) {
                                arcadeScore += it.score
                                // A run ends on the first round below the minimum; the rounds cleared are the record.
                                if (!it.passed) {
                                    arcadeActive = false
                                    profile = profile.recordArcadeRun(arcadeGame, difficulty.name, arcadeRound - 1, arcadeScore)
                                }
                            }
                            if (level.id.startsWith("fair-") || level.id.startsWith("arcade-")) {
                                container.profile.save(profile)
                            }
                            result = it
                            results = activeSession.state.results
                            replace(RESULT)
                        },
                    )
                }
            }
            PROFILE -> ProfileHubScreen(
                profile = profile,
                store = container.profile,
                onProfile = { updated -> profile = updated; container.profile.save(updated) },
                onBack = { back() },
                onBirdBook = { go(BIRD_BOOK) },
                onBosses = { go(BOSSES) },
                onAchievements = { go(ACHIEVEMENTS) },
                onStats = { go(STATS) },
                onCosmetics = { go(COSMETICS) },
            )
            BIRD_BOOK -> BirdBookScreen(profile, content, onBack = { back() })
            BOSSES -> BossGalleryScreen(profile, content, onBack = { back() })
            ACHIEVEMENTS -> AchievementsScreen(profile, onBack = { back() })
            STATS -> StatsScreen(profile, onBack = { back() })
            FAIR -> FairScreen(
                difficulty = difficulty,
                records = profile.records,
                onDifficulty = { difficulty = it },
                onClay = {
                    val spec = LevelSpec.clay(seed = 7_700L, pairs = 10, need = 10, difficulty = difficulty)
                    val level = LevelFile(
                        id = "fair-clay", place = 2, ordinal = 1, type = spec.type,
                        family = "clay-pairs", tags = emptyList(), tutorial = null, variants = emptyMap(),
                    )
                    startSession(level, spec)
                },
                onGallery = { go(GALLERY) },
                onBack = { back() },
            )
            GALLERY -> GalleryScreen(
                difficulty = difficulty,
                onFinished = { score, targets ->
                    profile = profile.recordGallery(difficulty.name, score, targets)
                    container.profile.save(profile)
                },
                onBack = { back() },
            )
            ARCADE -> ArcadeScreen(
                difficulty = difficulty,
                onDifficulty = { difficulty = it },
                records = profile.records,
                onStart = { game -> startArcade(game, difficulty) },
                onBack = { back() },
            )
            DAILY -> DailyScreen(
                profile = profile,
                repository = content,
                onPlay = { id, diff ->
                    val level = content.level(id)
                    if (level != null) {
                        val entry = content.dailyPool.entries.getOrNull(
                            (java.time.LocalDate.now().toString().hashCode() and Int.MAX_VALUE) %
                                content.dailyPool.entries.size.coerceAtLeast(1),
                        )
                        dailyDate = java.time.LocalDate.now().toString()
                        dailyQueue = entry?.rounds?.drop(1).orEmpty()
                        dailyScore = 0
                        dailyStars = 0
                        startGame(level, diff)
                    }
                },
                onBack = { back() },
            )
            COSMETICS -> CosmeticsScreen(
                profile = profile,
                onDog = { id -> profile = profile.copy(selectedDog = id); container.profile.save(profile) },
                onCamera = { id -> profile = profile.copy(selectedCamera = id); container.profile.save(profile) },
                onViewfinder = { id -> profile = profile.copy(selectedViewfinder = id); container.profile.save(profile) },
                onBack = { back() },
            )
            SETTINGS -> SettingsScreen(
                settings = settings,
                repository = container.settings,
                onBack = { back() },
                onCalibrate = { go(CALIBRATION) },
                onAbout = { go(ABOUT) },
            )
            ABOUT -> AboutScreen(onBack = { back() })
            CALIBRATION -> GyroCalibrationScreen(
                gainPercent = settings.gyroGainPercent,
                onBack = { back() },
            )
            TRAINING -> TrainingScreen(
                settings = settings,
                onMode = { mode ->
                    scope.launch { container.settings.update { it.copy(aimMode = mode) } }
                },
                onStart = { startTraining() },
                onCalibrate = { go(CALIBRATION) },
                onBack = { back() },
            )
            RESULT -> {
                val summary = result
                val level = levelFile
                if (summary == null || level == null) {
                    backTo(PLACE)
                } else {
                    ResultScreen(
                        summary = summary,
                        results = results,
                        level = level,
                        repository = content,
                        hasNext = summary.passed && (
                            arcadeActive ||
                                (dailyDate != null && dailyQueue.isNotEmpty()) ||
                                content.nextLevel(level.id) != null
                            ),
                        onNext = {
                            if (arcadeActive) {
                                arcadeRound++
                                startArcadeRound()
                                return@ResultScreen
                            }
                            val dailyNext = dailyQueue.firstOrNull()
                            if (dailyDate != null && dailyNext != null) {
                                dailyQueue = dailyQueue.drop(1)
                                val nextLevel = content.level(dailyNext.levelId)
                                if (nextLevel != null) {
                                    startGame(nextLevel, Difficulty.valueOf(dailyNext.difficulty))
                                } else {
                                    backTo(PLACE)
                                }
                            } else {
                                content.nextLevel(level.id)?.let { next -> startGame(next, difficulty) }
                                    ?: run { backTo(PLACE) }
                            }
                        },
                        onRepeat = { startGame(level, difficulty) },
                        onReplay = { result ->
                            session?.let { active ->
                                replay = active.buildReplay(result.birdId)
                                go(REPLAY)
                            }
                        },
                        onMap = { backTo(PLACE) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ContentError() {
    val colors = LocalDuckColors.current
    val type = LocalDuckType.current
    Box(
        Modifier.fillMaxSize().background(colors.bg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.app_name), style = type.placeName, color = colors.text)
            Text(stringResource(R.string.content_error), style = type.bodyText, color = colors.danger)
        }
    }
}

package pt.tiago.ducknight.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import pt.tiago.ducknight.R

// The only place in the app where color literals are allowed.
// Every token is spelled out for each theme so that the JVM test can check the design table.

/** The four visual directions. B is the main one; A, C and D are selectable in Settings. */
enum class ThemeId {
    /** A · Yellow Cassette (8-bit). */
    CASSETTE,

    /** B · Lakeside Dusk (default). */
    LAGOON,

    /** C · Cut Paper. */
    PAPER,

    /** D · Village Fair. */
    FAIR,
}

/** Color-blind palettes; they replace `good`, `protected` and `danger` in every theme. */
enum class ColorblindPalette { NORMAL, DEUTERANOPIA, PROTANOPIA, TRITANOPIA }

/** Color tokens. Nothing outside this package uses color literals. */
data class DuckColors(
    val bg: Color,
    val bg2: Color,
    val surface: Color,
    val surface2: Color,
    val line: Color,
    val text: Color,
    val textMuted: Color,
    val accent: Color,
    val onAccent: Color,
    val good: Color,
    val protected: Color,
    val danger: Color,
    val viewfinder: Color,
    val flash: Color,
    val difficultyEasy: Color,
    val difficultyMedium: Color,
    val difficultyHard: Color,
)

/** Corner radii per theme. Sheets use [sheet] in every theme. */
data class DuckShapes(
    val card: Dp,
    val button: Dp,
    val sheet: Dp = 28.dp,
)

/** Font families and the type scale. */
data class DuckType(
    /** Titles and the brand: Fraunces, Press Start 2P, Fredoka or Rye. */
    val display: FontFamily,
    /** Body and controls: Figtree. */
    val body: FontFamily,
    /** HUD numbers: Figtree with tabular figures. */
    val numbers: FontFamily,
    val brand: TextStyle,
    val placeName: TextStyle,
    val screenTitle: TextStyle,
    val cardTitle: TextStyle,
    val bodyText: TextStyle,
    val button: TextStyle,
    val caption: TextStyle,
    val hudLevel: TextStyle,
    val hudScore: TextStyle,
    val floatingPoints: TextStyle,
    val label: TextStyle,
)

/** Shared difficulty chip colors (text on a 15 % background, same in every theme). */
private val DifficultyEasy = Color(0xFF9ECE6A)
private val DifficultyMedium = Color(0xFFE0AF68)
private val DifficultyHard = Color(0xFFF7768E)

/** Color-blind overrides. Validated with deltaE2000 in the JVM tests. */
private data class ColorblindOverride(val good: Color, val protected: Color, val danger: Color)

private fun colorblindOverride(palette: ColorblindPalette): ColorblindOverride? = when (palette) {
    ColorblindPalette.NORMAL -> null
    ColorblindPalette.DEUTERANOPIA -> ColorblindOverride(
        good = Color(0xFF4E9BFF), protected = Color(0xFFF1CE63), danger = Color(0xFFF28E2B),
    )
    ColorblindPalette.PROTANOPIA -> ColorblindOverride(
        good = Color(0xFF4E9BFF), protected = Color(0xFFF5C342), danger = Color(0xFFE8A0FF),
    )
    ColorblindPalette.TRITANOPIA -> ColorblindOverride(
        good = Color(0xFF2EC4B6), protected = Color(0xFFF7A1C4), danger = Color(0xFFE8505B),
    )
}

/** Base colors of one theme, before the color-blind overrides. */
fun themeColors(themeId: ThemeId): DuckColors = when (themeId) {
    ThemeId.CASSETTE -> DuckColors(
        bg = Color(0xFF0B0B10),
        bg2 = Color(0xFF000000),
        surface = Color(0xFF1C1C26),
        surface2 = Color(0xFF26263A),
        line = Color(0xFF34344A),
        text = Color(0xFFFCFCFC),
        textMuted = Color(0xFFA4A4BA),
        accent = Color(0xFFF8B800),
        onAccent = Color(0xFF1A1200),
        good = Color(0xFF80D010),
        protected = Color(0xFF3CBCFC),
        danger = Color(0xFFF83800),
        viewfinder = Color(0xFFFCFCFC),
        flash = Color(0xFFFFFFFF),
        difficultyEasy = DifficultyEasy,
        difficultyMedium = DifficultyMedium,
        difficultyHard = DifficultyHard,
    )
    ThemeId.LAGOON -> DuckColors(
        bg = Color(0xFF170F26),
        bg2 = Color(0xFF120B1E),
        surface = Color(0xFF251A3B),
        surface2 = Color(0xFF2F2249),
        line = Color(0xFF3B2C55),
        text = Color(0xFFF7E8D8),
        textMuted = Color(0xFFB9A5C4),
        accent = Color(0xFFF39A55),
        onAccent = Color(0xFF2A1206),
        good = Color(0xFF9ED36A),
        protected = Color(0xFF7CC8F0),
        danger = Color(0xFFF07A6A),
        viewfinder = Color(0xFFFFF4E6),
        flash = Color(0xFFFFFFFF),
        difficultyEasy = DifficultyEasy,
        difficultyMedium = DifficultyMedium,
        difficultyHard = DifficultyHard,
    )
    ThemeId.PAPER -> DuckColors(
        bg = Color(0xFFF4E4D0),
        bg2 = Color(0xFFEED6BE),
        surface = Color(0xFFFFF8EE),
        surface2 = Color(0xFFF7E9D8),
        line = Color(0xFFE3CDB5),
        text = Color(0xFF3B2D28),
        textMuted = Color(0xFF7A665B),
        accent = Color(0xFFD9483C),
        onAccent = Color(0xFFFFF8EE),
        good = Color(0xFF3E8E5C),
        protected = Color(0xFF2F7FB0),
        danger = Color(0xFFC8372D),
        viewfinder = Color(0xFF3B2D28),
        flash = Color(0xFFFFFFFF),
        difficultyEasy = DifficultyEasy,
        difficultyMedium = DifficultyMedium,
        difficultyHard = DifficultyHard,
    )
    ThemeId.FAIR -> DuckColors(
        bg = Color(0xFF22100F),
        bg2 = Color(0xFF170A09),
        surface = Color(0xFF3A1B19),
        surface2 = Color(0xFF4A2420),
        line = Color(0xFF5A2C26),
        text = Color(0xFFFBEFD9),
        textMuted = Color(0xFFC9A88E),
        accent = Color(0xFFF5C033),
        onAccent = Color(0xFF2A1400),
        good = Color(0xFF9ED36A),
        protected = Color(0xFF7CC8F0),
        danger = Color(0xFFFF6B5A),
        viewfinder = Color(0xFFFFF4D6),
        flash = Color(0xFFFFFFFF),
        difficultyEasy = DifficultyEasy,
        difficultyMedium = DifficultyMedium,
        difficultyHard = DifficultyHard,
    )
}

/**
 * Final colors for a theme, applying the color-blind palette. Pure function, so the JVM tests
 * can check every combination.
 */
fun duckColors(themeId: ThemeId, palette: ColorblindPalette = ColorblindPalette.NORMAL): DuckColors {
    val base = themeColors(themeId)
    val override = colorblindOverride(palette) ?: return base
    return base.copy(good = override.good, protected = override.protected, danger = override.danger)
}

/** Corner radii per theme. */
fun duckShapes(themeId: ThemeId): DuckShapes = when (themeId) {
    ThemeId.CASSETTE -> DuckShapes(card = 0.dp, button = 0.dp, sheet = 0.dp)
    ThemeId.LAGOON -> DuckShapes(card = 14.dp, button = 12.dp)
    ThemeId.PAPER -> DuckShapes(card = 18.dp, button = 16.dp)
    ThemeId.FAIR -> DuckShapes(card = 6.dp, button = 4.dp)
}

/**
 * Builds the type scale for the given font families. Theme A sets display type at
 * 0.62 of the size because Press Start 2P is very wide.
 */
fun duckType(
    display: FontFamily,
    body: FontFamily,
    numbers: FontFamily,
    displayScale: Float,
    textScale: Float = 1f,
): DuckType {
    fun displayStyle(size: Int, lineHeight: Int, weight: FontWeight): TextStyle = TextStyle(
        fontFamily = display,
        fontWeight = weight,
        fontSize = (size * displayScale).sp,
        lineHeight = (lineHeight * displayScale).sp,
    )
    fun scale(style: TextStyle): TextStyle = if (textScale == 1f) style else style.copy(
        fontSize = style.fontSize * textScale,
        lineHeight = style.lineHeight * textScale,
    )
    return DuckType(
        display = display,
        body = body,
        numbers = numbers,
        brand = scale(displayStyle(44, 40, FontWeight.Bold)),
        placeName = scale(displayStyle(26, 30, FontWeight.Bold)),
        screenTitle = scale(displayStyle(22, 28, FontWeight.SemiBold)),
        cardTitle = scale(displayStyle(18, 24, FontWeight.SemiBold)),
        bodyText = scale(TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 22.sp)),
        button = scale(TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp)),
        caption = scale(TextStyle(fontFamily = body, fontWeight = FontWeight.Normal, fontSize = 13.sp, lineHeight = 18.sp)),
        hudLevel = scale(TextStyle(fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 20.sp)),
        hudScore = scale(TextStyle(
            fontFamily = numbers, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 24.sp,
            fontFeatureSettings = "tnum",
        )),
        floatingPoints = scale(TextStyle(fontFamily = body, fontWeight = FontWeight.Bold, fontSize = 14.sp, lineHeight = 18.sp)),
        label = scale(TextStyle(
            fontFamily = body, fontWeight = FontWeight.SemiBold, fontSize = 11.sp, lineHeight = 14.sp,
            letterSpacing = 0.1.em,
        )),
    )
}

/** Resource-backed font families for a theme. Must be called from composition. */
@Composable
fun rememberDuckType(themeId: ThemeId, textScale: Float = 1f): DuckType {
    val body = FontFamily(
        Font(R.font.figtree_regular, FontWeight.Normal),
        Font(R.font.figtree_medium, FontWeight.Medium),
        Font(R.font.figtree_semibold, FontWeight.SemiBold),
        Font(R.font.figtree_bold, FontWeight.Bold),
    )
    val display = when (themeId) {
        // Variable font: Android sets the wght axis from the requested weight.
        ThemeId.LAGOON -> FontFamily(
            Font(R.font.fraunces_variable, FontWeight.SemiBold),
            Font(R.font.fraunces_variable, FontWeight.Bold),
            Font(R.font.fraunces_variable, FontWeight.ExtraBold),
        )
        ThemeId.CASSETTE -> FontFamily(Font(R.font.press_start_2p, FontWeight.Normal))
        ThemeId.PAPER -> FontFamily(
            Font(R.font.fredoka_variable, FontWeight.Medium),
            Font(R.font.fredoka_variable, FontWeight.SemiBold),
            Font(R.font.fredoka_variable, FontWeight.Bold),
        )
        ThemeId.FAIR -> FontFamily(Font(R.font.rye, FontWeight.Normal))
    }
    val numbers = FontFamily(
        Font(R.font.figtree_regular, FontWeight.Normal),
        Font(R.font.figtree_medium, FontWeight.Medium),
        Font(R.font.figtree_semibold, FontWeight.SemiBold),
        Font(R.font.figtree_bold, FontWeight.Bold),
    )
    val scale = if (themeId == ThemeId.CASSETTE) 0.62f else 1f
    return remember(themeId, display, body, numbers, textScale) {
        duckType(display, body, numbers, scale, textScale)
    }
}

val LocalDuckColors = staticCompositionLocalOf { duckColors(ThemeId.LAGOON) }
val LocalDuckShapes = staticCompositionLocalOf { duckShapes(ThemeId.LAGOON) }
val LocalDuckType = staticCompositionLocalOf<DuckType> { error("DuckTheme() is missing from the composition") }

/**
 * Provides the Duck Night tokens to every child, and a Material 3 color scheme derived from them so
 * that default Material components follow the theme too.
 */
@Composable
fun DuckTheme(
    themeId: ThemeId = ThemeId.LAGOON,
    palette: ColorblindPalette = ColorblindPalette.NORMAL,
    textScale: Float = 1f,
    content: @Composable () -> Unit,
) {
    val colors = remember(themeId, palette) { duckColors(themeId, palette) }
    val shapes = remember(themeId) { duckShapes(themeId) }
    val type = rememberDuckType(themeId, textScale)
    val dark = themeId != ThemeId.PAPER
    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            background = colors.bg,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.surface2,
            onSurfaceVariant = colors.textMuted,
            error = colors.danger,
            outline = colors.line,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            background = colors.bg,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.surface2,
            onSurfaceVariant = colors.textMuted,
            error = colors.danger,
            outline = colors.line,
        )
    }
    val typography = Typography(
        titleLarge = type.screenTitle,
        bodyLarge = type.bodyText,
        labelLarge = type.button,
    )
    CompositionLocalProvider(
        LocalDuckColors provides colors,
        LocalDuckShapes provides shapes,
        LocalDuckType provides type,
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}

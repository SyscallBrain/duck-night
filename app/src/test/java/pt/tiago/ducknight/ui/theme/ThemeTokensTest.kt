package pt.tiago.ducknight.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

/** Checks the color tokens of every theme and the WCAG contrast of the text tokens. */
class ThemeTokensTest {

    /** Expected hex per token, in ThemeId order: CASSETTE, LAGOON, PAPER, FAIR. */
    private val expected: Map<String, List<Long>> = mapOf(
        "bg" to listOf(0x0B0B10L, 0x170F26L, 0xF4E4D0L, 0x22100FL),
        "bg2" to listOf(0x000000L, 0x120B1EL, 0xEED6BEL, 0x170A09L),
        "surface" to listOf(0x1C1C26L, 0x251A3BL, 0xFFF8EEL, 0x3A1B19L),
        "surface2" to listOf(0x26263AL, 0x2F2249L, 0xF7E9D8L, 0x4A2420L),
        "line" to listOf(0x34344AL, 0x3B2C55L, 0xE3CDB5L, 0x5A2C26L),
        "text" to listOf(0xFCFCFCL, 0xF7E8D8L, 0x3B2D28L, 0xFBEFD9L),
        "textMuted" to listOf(0xA4A4BAL, 0xB9A5C4L, 0x7A665BL, 0xC9A88EL),
        "accent" to listOf(0xF8B800L, 0xF39A55L, 0xD9483CL, 0xF5C033L),
        "onAccent" to listOf(0x1A1200L, 0x2A1206L, 0xFFF8EEL, 0x2A1400L),
        "good" to listOf(0x80D010L, 0x9ED36AL, 0x3E8E5CL, 0x9ED36AL),
        "protected" to listOf(0x3CBCFCL, 0x7CC8F0L, 0x2F7FB0L, 0x7CC8F0L),
        "danger" to listOf(0xF83800L, 0xF07A6AL, 0xC8372DL, 0xFF6B5AL),
        "viewfinder" to listOf(0xFCFCFCL, 0xFFF4E6L, 0x3B2D28L, 0xFFF4D6L),
        "flash" to listOf(0xFFFFFFL, 0xFFFFFFL, 0xFFFFFFL, 0xFFFFFFL),
        "difficultyEasy" to listOf(0x9ECE6AL, 0x9ECE6AL, 0x9ECE6AL, 0x9ECE6AL),
        "difficultyMedium" to listOf(0xE0AF68L, 0xE0AF68L, 0xE0AF68L, 0xE0AF68L),
        "difficultyHard" to listOf(0xF7768EL, 0xF7768EL, 0xF7768EL, 0xF7768EL),
    )

    private val themes = ThemeId.entries

    private fun token(colors: DuckColors, name: String): Color = when (name) {
        "bg" -> colors.bg
        "bg2" -> colors.bg2
        "surface" -> colors.surface
        "surface2" -> colors.surface2
        "line" -> colors.line
        "text" -> colors.text
        "textMuted" -> colors.textMuted
        "accent" -> colors.accent
        "onAccent" -> colors.onAccent
        "good" -> colors.good
        "protected" -> colors.protected
        "danger" -> colors.danger
        "viewfinder" -> colors.viewfinder
        "flash" -> colors.flash
        "difficultyEasy" -> colors.difficultyEasy
        "difficultyMedium" -> colors.difficultyMedium
        "difficultyHard" -> colors.difficultyHard
        else -> error("Unknown token $name")
    }

    private fun hex(color: Color): Long {
        fun channel(value: Float) = Math.round(value * 255f).toLong()
        return (channel(color.red) shl 16) or (channel(color.green) shl 8) or channel(color.blue)
    }

    private fun luminance(color: Color): Double {
        fun linear(value: Float): Double {
            val c = value.toDouble()
            return if (c <= 0.03928) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * linear(color.red) + 0.7152 * linear(color.green) + 0.0722 * linear(color.blue)
    }

    private fun contrast(a: Color, b: Color): Double {
        val la = luminance(a)
        val lb = luminance(b)
        return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
    }

    @Test
    fun everyThemeHasEveryTokenWithTheDesignHex() {
        assertEquals("Every design token is listed", 17, expected.size)
        for ((index, themeId) in themes.withIndex()) {
            val colors = themeColors(themeId)
            for ((name, values) in expected) {
                val actual = token(colors, name)
                assertEquals("$themeId.$name", values[index], hex(actual))
                assertEquals("$themeId.$name must be opaque", 1f, actual.alpha, 0f)
            }
        }
    }

    @Test
    fun textAndMutedTextReachAaContrastOnSurface() {
        val report = StringBuilder()
        for (themeId in themes) {
            val colors = themeColors(themeId)
            val text = contrast(colors.text, colors.surface)
            val muted = contrast(colors.textMuted, colors.surface)
            report.append("$themeId text/surface=%.2f textMuted/surface=%.2f\n".format(text, muted))
            assertTrue("$themeId text/surface $text < 4.5", text >= 4.5)
            assertTrue("$themeId textMuted/surface $muted < 4.5", muted >= 4.5)
        }
        println("WCAG contrast:\n$report")
    }

    @Test
    fun colorblindPalettesOverrideTheSemanticColorsOnly() {
        val expectedOverrides = mapOf(
            ColorblindPalette.DEUTERANOPIA to Triple(0x4E9BFFL, 0xF1CE63L, 0xF28E2BL),
            ColorblindPalette.PROTANOPIA to Triple(0x4E9BFFL, 0xF5C342L, 0xE8A0FFL),
            ColorblindPalette.TRITANOPIA to Triple(0x2EC4B6L, 0xF7A1C4L, 0xE8505BL),
        )
        for (themeId in themes) {
            val base = themeColors(themeId)
            for ((palette, hexes) in expectedOverrides) {
                val colors = duckColors(themeId, palette)
                assertEquals("$themeId $palette good", hexes.first, hex(colors.good))
                assertEquals("$themeId $palette protected", hexes.second, hex(colors.protected))
                assertEquals("$themeId $palette danger", hexes.third, hex(colors.danger))
                assertEquals("$themeId $palette keeps accent", base.accent, colors.accent)
                assertEquals("$themeId $palette keeps surface", base.surface, colors.surface)
            }
            assertEquals(base, duckColors(themeId, ColorblindPalette.NORMAL))
        }
    }

    @Test
    fun shapesFollowDesignSection2() {
        assertEquals(0.dp, duckShapes(ThemeId.CASSETTE).card)
        assertEquals(14.dp, duckShapes(ThemeId.LAGOON).card)
        assertEquals(18.dp, duckShapes(ThemeId.PAPER).card)
        assertEquals(6.dp, duckShapes(ThemeId.FAIR).card)
        assertEquals(12.dp, duckShapes(ThemeId.LAGOON).button)
        assertEquals(16.dp, duckShapes(ThemeId.PAPER).button)
        assertEquals(28.dp, duckShapes(ThemeId.LAGOON).sheet)
    }

    @Test
    fun largerTextScalesEveryStyle() {
        val normal = duckType(FontFamily.Default, FontFamily.Default, FontFamily.Default, 1f)
        val large = duckType(FontFamily.Default, FontFamily.Default, FontFamily.Default, 1f, textScale = 1.15f)
        assertEquals(normal.bodyText.fontSize.value * 1.15f, large.bodyText.fontSize.value, 0.01f)
        assertEquals(normal.caption.fontSize.value * 1.15f, large.caption.fontSize.value, 0.01f)
        assertEquals(normal.brand.fontSize.value * 1.15f, large.brand.fontSize.value, 0.01f)
    }

    @Test
    fun colorblindPalettesKeepTheSemanticColoursApart() {
        for (palette in ColorblindPalette.entries.filter { it != ColorblindPalette.NORMAL }) {
            val colors = duckColors(ThemeId.LAGOON, palette)
            val semantic = listOf(colors.good, colors.protected, colors.danger)
            for (first in semantic.indices) {
                for (second in first + 1 until semantic.size) {
                    val distance = deltaE2000(semantic[first], semantic[second])
                    assertTrue("$palette ${first}-${second}: $distance", distance >= 20.0)
                }
            }
        }
    }

    @Test
    fun typeScaleMatchesDesignSection3() {
        val type = duckType(FontFamily.Default, FontFamily.Default, FontFamily.Default, 1f)
        assertEquals(44f, type.brand.fontSize.value, 0.01f)
        assertEquals(26f, type.placeName.fontSize.value, 0.01f)
        assertEquals(22f, type.screenTitle.fontSize.value, 0.01f)
        assertEquals(18f, type.cardTitle.fontSize.value, 0.01f)
        assertEquals(16f, type.bodyText.fontSize.value, 0.01f)
        assertEquals(13f, type.caption.fontSize.value, 0.01f)
        assertEquals(16f, type.hudLevel.fontSize.value, 0.01f)
        assertEquals(20f, type.hudScore.fontSize.value, 0.01f)
        assertEquals(14f, type.floatingPoints.fontSize.value, 0.01f)
        val cassette = duckType(FontFamily.Default, FontFamily.Default, FontFamily.Default, 0.62f)
        assertEquals(44f * 0.62f, cassette.brand.fontSize.value, 0.01f)
        assertEquals(22f * 0.62f, cassette.screenTitle.fontSize.value, 0.01f)
    }

    /** CIE76 Lab conversion and the CIEDE2000 distance used by the palette acceptance test. */
    private fun lab(color: Color): Triple<Double, Double, Double> {
        fun linear(value: Float): Double {
            val c = value.toDouble()
            return if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)
        }
        val r = linear(color.red)
        val g = linear(color.green)
        val b = linear(color.blue)
        val x = (r * 0.4124564 + g * 0.3575761 + b * 0.1804375) / 0.95047
        val y = r * 0.2126729 + g * 0.7151522 + b * 0.0721750
        val z = (r * 0.0193339 + g * 0.1191920 + b * 0.9503041) / 1.08883
        fun f(value: Double) = if (value > 0.008856) value.pow(1.0 / 3.0) else 7.787 * value + 16.0 / 116.0
        val fx = f(x)
        val fy = f(y)
        val fz = f(z)
        return Triple(116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz))
    }

    private fun deltaE2000(a: Color, b: Color): Double {
        val (l1, a1, b1) = lab(a)
        val (l2, a2, b2) = lab(b)
        val c1 = hypot(a1, b1)
        val c2 = hypot(a2, b2)
        val cBar = (c1 + c2) / 2
        val g = 0.5 * (1 - sqrt(cBar.pow(7) / (cBar.pow(7) + 25.0.pow(7))))
        val ap1 = (1 + g) * a1
        val ap2 = (1 + g) * a2
        val cp1 = hypot(ap1, b1)
        val cp2 = hypot(ap2, b2)
        val hp1 = Math.toDegrees(atan2(b1, ap1)).let { if (it < 0) it + 360 else it }
        val hp2 = Math.toDegrees(atan2(b2, ap2)).let { if (it < 0) it + 360 else it }
        val dL = l2 - l1
        val dC = cp2 - cp1
        var dh = hp2 - hp1
        if (cp1 * cp2 == 0.0) dh = 0.0 else if (dh > 180) dh -= 360 else if (dh < -180) dh += 360
        val dH = 2 * sqrt(cp1 * cp2) * sin(Math.toRadians(dh / 2))
        val lBar = (l1 + l2) / 2
        val cpBar = (cp1 + cp2) / 2
        val hSum = hp1 + hp2
        val hpBar = when {
            cp1 * cp2 == 0.0 -> hSum
            kotlin.math.abs(hp1 - hp2) <= 180 -> hSum / 2
            hSum < 360 -> (hSum + 360) / 2
            else -> (hSum - 360) / 2
        }
        val t = 1 - 0.17 * cos(Math.toRadians(hpBar - 30)) +
            0.24 * cos(Math.toRadians(2 * hpBar)) +
            0.32 * cos(Math.toRadians(3 * hpBar + 6)) -
            0.20 * cos(Math.toRadians(4 * hpBar - 63))
        val dTheta = 30 * exp(-((hpBar - 275) / 25).pow(2))
        val rc = 2 * sqrt(cpBar.pow(7) / (cpBar.pow(7) + 25.0.pow(7)))
        val sl = 1 + 0.015 * (lBar - 50).pow(2) / sqrt(20 + (lBar - 50).pow(2))
        val sc = 1 + 0.045 * cpBar
        val sh = 1 + 0.015 * cpBar * t
        val rt = -sin(Math.toRadians(2 * dTheta)) * rc
        return sqrt(
            (dL / sl).pow(2) + (dC / sc).pow(2) + (dH / sh).pow(2) +
                rt * (dC / sc) * (dH / sh),
        )
    }
}

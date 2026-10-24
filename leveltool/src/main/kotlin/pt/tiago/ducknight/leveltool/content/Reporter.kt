package pt.tiago.ducknight.leveltool.content
import pt.tiago.ducknight.core.content.*

import pt.tiago.ducknight.core.level.Difficulty
import java.nio.file.Files
import java.nio.file.Path

object Reporter {
    fun write(store: ContentStore, output: Path): String {
        val levels = store.levelFiles()
        val certificates = store.loadCertificates()
        val lines = mutableListOf<String>()
        lines += "# Duck Night content report"
        lines += ""
        lines += "## Levels"
        lines += ""
        lines += "| Metric | Value |"
        lines += "|---|---|"
        lines += "| Levels | ${levels.size} |"
        lines += "| Variants | ${levels.size * 3} |"
        lines += "| Certificates | ${certificates.size} |"
        lines += ""
        lines += "| Type | Levels |"
        lines += "|---|---|"
        for ((type, count) in levels.groupingBy { it.type }.eachCount().toSortedMap()) {
            lines += "| $type | $count |"
        }
        lines += ""
        lines += "## Families"
        lines += ""
        lines += "| Family | Levels |"
        lines += "|---|---|"
        for ((family, count) in levels.groupingBy { it.family }.eachCount().toSortedMap()) {
            lines += "| $family | $count |"
        }
        lines += ""
        lines += "## Calibration"
        lines += ""
        lines += "| Level | Difficulty | Success | Band | p35 | p70 | Mean score |"
        lines += "|---|---|---|---|---|---|---|"
        var outliers = 0
        var above = 0
        var below = 0
        var zeroSuccess = 0
        for (certificate in certificates) {
            val difficulty = runCatching { Difficulty.valueOf(certificate.difficulty) }.getOrNull() ?: continue
            val band = runCatching {
                Calibrator.bandFor(store.loadLevel(certificate.levelId).spec(difficulty))
            }.getOrElse { Calibrator.bandFor(difficulty) }
            val inBand = certificate.human.successRate in band
            if (!inBand) outliers++
            if (certificate.human.successRate > band.endInclusive) above++
            if (certificate.human.successRate < band.start) below++
            if (certificate.human.successRate == 0.0) zeroSuccess++
            lines += "| ${certificate.levelId} | ${certificate.difficulty} | " +
                "${"%.3f".format(certificate.human.successRate)} | " +
                "${"%.2f".format(band.start)}–${"%.2f".format(band.endInclusive)} | " +
                "${certificate.human.p35} | ${certificate.human.p70} | " +
                "${"%.2f".format(certificate.human.meanScore / 1000)}k |"
        }
        lines += ""
        lines += "Outliers outside the bands: $outliers ($above above the bands, $below below the bands, " +
            "$zeroSuccess zero-success variants)"
        if (outliers > 0) {
            lines += ""
            lines += "Justification: these variants stayed outside their band after the tuning steps " +
                "(minimum, then speed in 5 % steps, within 50-160 %); the closest variant was kept and " +
                "certified with a three-star PerfectModel witness (BOT_VERSION 2)."
        }
        lines += ""
        val text = lines.joinToString("\n")
        output.parent?.let { Files.createDirectories(it) }
        Files.writeString(output, text)
        return text
    }
}

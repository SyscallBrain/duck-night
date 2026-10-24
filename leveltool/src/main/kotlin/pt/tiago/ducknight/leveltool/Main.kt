package pt.tiago.ducknight.leveltool

import pt.tiago.ducknight.leveltool.content.Calibrator
import pt.tiago.ducknight.leveltool.content.Certifier
import pt.tiago.ducknight.leveltool.content.ContentStore
import pt.tiago.ducknight.leveltool.content.DailyPoolGenerator
import pt.tiago.ducknight.leveltool.content.Exporter
import pt.tiago.ducknight.leveltool.content.Generator
import pt.tiago.ducknight.leveltool.content.Reporter
import pt.tiago.ducknight.leveltool.content.Validator
import java.nio.file.Path
import kotlin.system.exitProcess

internal const val USAGE = """Duck Night content tool

Usage: leveltool <command> [options]

Commands:
  validate [--strict]                 check the catalog structure, hashes and certificates
  generate --place N [--from L --to L] generate candidate and published levels for a place
  calibrate [--level ID] [--runs N]   measure the human model on one level or all levels
  certify [--level ID] [--runs N]     calibrate, set stars, find a witness and write certificates
  verify-certificates                 replay every witness with the current engine
  report [--output report.md]         write a content and calibration report
  export-assets [--output DIR]        validate and copy the catalog into Android assets
  daily-pool --version V              freeze the 365-entry daily challenge pool

Common options:
  --root DIR                          content root (default: content)
"""

private data class Options(
    var root: String = "content",
    var strict: Boolean = false,
    var place: Int = 1,
    var from: Int = 1,
    var to: Int = 10,
    var level: String? = null,
    var runs: Int = 400,
    var attempts: Int = 5,
    var output: String? = null,
    var version: Int = 1,
    var relaxed: Boolean = false,
)

fun main(args: Array<String>) {
    val command = args.firstOrNull() ?: run {
        System.err.print(USAGE)
        exitProcess(2)
    }
    if (command in setOf("help", "--help", "-h")) {
        print(USAGE)
        return
    }
    val options = runCatching { parse(args.drop(1)) }.getOrElse {
        System.err.println("leveltool: ${it.message}")
        exitProcess(2)
    }
    val store = ContentStore(Path.of(options.root))
    val status = when (command) {
        "validate" -> validate(store, options)
        "generate" -> generate(store, options)
        "calibrate" -> calibrate(store, options)
        "certify" -> certify(store, options)
        "verify-certificates" -> verifyCertificates(store)
        "report" -> report(store, options)
        "export-assets" -> exportAssets(store, options)
        "daily-pool" -> dailyPool(store, options)
        else -> {
            System.err.println("leveltool: unknown command '$command'")
            System.err.print(USAGE)
            2
        }
    }
    exitProcess(status)
}

private fun parse(args: List<String>): Options {
    val options = Options()
    var index = 0
    while (index < args.size) {
        val arg = args[index]
        fun value(): String {
            require(index + 1 < args.size) { "$arg needs a value" }
            index++
            return args[index]
        }
        when (arg) {
            "--root" -> options.root = value()
            "--strict" -> options.strict = true
            "--relaxed" -> options.relaxed = true
            "--place" -> options.place = value().toInt()
            "--from" -> options.from = value().toInt()
            "--to" -> options.to = value().toInt()
            "--level" -> options.level = value()
            "--runs" -> options.runs = value().toInt()
            "--attempts" -> options.attempts = value().toInt()
            "--output" -> options.output = value()
            "--version" -> options.version = value().toInt()
            else -> error("unknown option $arg")
        }
        index++
    }
    require(options.runs > 0)
    require(options.attempts > 0)
    return options
}

private fun validate(store: ContentStore, options: Options): Int {
    val errors = Validator.validate(store, options.strict)
    if (errors.isEmpty()) {
        println("validate: OK (${store.levelFiles().size} levels, ${store.loadCertificates().size} certificates)")
        return 0
    }
    errors.forEach { System.err.println("validate: $it") }
    return 1
}

private fun generate(store: ContentStore, options: Options): Int {
    val levels = Generator.generate(store, options.place, options.from, options.to)
    println("generate: ${levels.size} levels in place ${options.place}")
    return 0
}

private fun calibrate(store: ContentStore, options: Options): Int {
    store.ensureScaffold(options.place)
    val levels = options.level?.let { listOf(store.loadLevel(it)) } ?: store.levelFiles()
    if (levels.isEmpty()) {
        System.err.println("calibrate: no levels")
        return 1
    }
    for (level in levels) {
        val reports = Calibrator.calibrate(store, level, options.runs)
        for (report in reports) {
            println(
                "calibrate: ${report.levelId}-${report.difficulty} " +
                    "${"%.3f".format(report.successRate)} (${report.runs} runs)",
            )
        }
    }
    return 0
}

private fun certify(store: ContentStore, options: Options): Int {
    store.ensureScaffold(options.place)
    val levels = options.level?.let { listOf(store.loadLevel(it)) } ?: store.levelFiles()
    if (levels.isEmpty()) {
        System.err.println("certify: no levels")
        return 1
    }
    var failures = 0
    for (level in levels) {
        val result = Certifier.certify(store, level.id, options.attempts, options.runs, relaxed = options.relaxed)
        if (result.isEmpty()) {
            println("certify: ${level.id} OK")
        } else {
            failures += result.size
            result.forEach { System.err.println("certify: ${it.levelId}-${it.difficulty}: ${it.reason}") }
        }
    }
    return if (failures == 0) 0 else 1
}

private fun verifyCertificates(store: ContentStore): Int {
    val errors = Certifier.verify(store)
    if (errors.isEmpty()) {
        println("verify-certificates: OK (${store.loadCertificates().size} certificates)")
        return 0
    }
    errors.forEach { System.err.println("verify-certificates: $it") }
    return 1
}

private fun report(store: ContentStore, options: Options): Int {
    val output = Path.of(options.output ?: "report.md")
    Reporter.write(store, output)
    println("report: $output")
    return 0
}

private fun exportAssets(store: ContentStore, options: Options): Int {
    val output = Path.of(options.output ?: "app/build/generated/duckAssets")
    val result = runCatching { Exporter.export(store, output) }.getOrElse {
        System.err.println("export-assets: ${it.message}")
        return 1
    }
    println("export-assets: ${result.files} files, ${result.bytes} bytes, ${result.milliseconds} ms -> $output")
    return 0
}

private fun dailyPool(store: ContentStore, options: Options): Int {
    val pool = DailyPoolGenerator.generate(store, options.version)
    println("daily-pool: version ${pool.version}, ${pool.entries.size} entries")
    return 0
}

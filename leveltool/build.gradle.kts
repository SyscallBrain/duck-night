plugins {
    application
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}
kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
java { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
dependencies {
    implementation(project(":core"))
    implementation(libs.serialization.json)
    testImplementation(libs.junit)
}
application { mainClass.set("pt.tiago.ducknight.leveltool.MainKt") }
tasks.named<JavaExec>("run") {
    workingDir = rootProject.projectDir
    maxHeapSize = "2g"
}
tasks.withType<Test>().configureEach {
    maxHeapSize = "2g"
    // Tests read content/ from the project root, like the run task does.
    workingDir = rootProject.projectDir
}

// Content pipeline: the app build validates and exports the catalog; certificates are replayed
// separately by verifyCertificates in verify.sh and CI.
val contentRoot = rootProject.layout.projectDirectory.dir("content")
val assetOutput = rootProject.layout.projectDirectory.dir("app/build/generated/duckAssets")

val validateCatalog = tasks.register<JavaExec>("validateCatalog") {
    group = "verification"
    description = "Validates the published catalog, hashes and certificates."
    dependsOn(tasks.named("classes"))
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set(application.mainClass)
    workingDir = rootProject.projectDir
    maxHeapSize = "2g"
    args("validate", "--root", "content", "--strict")
    inputs.dir(contentRoot)
    outputs.upToDateWhen { false }
}

val verifyCertificates = tasks.register<JavaExec>("verifyCertificates") {
    group = "verification"
    description = "Replays every certificate witness with the current engine."
    dependsOn(tasks.named("classes"))
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set(application.mainClass)
    workingDir = rootProject.projectDir
    maxHeapSize = "2g"
    args("verify-certificates", "--root", "content")
    inputs.dir(contentRoot)
    outputs.upToDateWhen { false }
}

tasks.register<JavaExec>("exportGameAssets") {
    group = "build"
    description = "Validates and exports the catalog into Android assets with a SHA-256 manifest."
    dependsOn(validateCatalog)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set(application.mainClass)
    workingDir = rootProject.projectDir
    maxHeapSize = "2g"
    args("export-assets", "--root", "content", "--output", assetOutput.asFile.absolutePath)
    inputs.dir(contentRoot)
    inputs.files(sourceSets.main.get().runtimeClasspath)
    outputs.dir(assetOutput)
}

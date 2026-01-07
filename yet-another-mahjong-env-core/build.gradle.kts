plugins {
    kotlin("jvm")
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.ksp)
}

group = "xyz.uthofficial"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.dagger)
    ksp(libs.daggerCompiler)
    api(libs.slf4jApi)
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.logbackClassic)
}

tasks.test {
    useJUnitPlatform()
    jvmArgs("-XX:+EnableDynamicAgentLoading")
    jvmArgs("-Djdk.instrument.traceUsage=true")

    val tmpDir = layout.buildDirectory.dir("tmp")
    systemProperty("java.io.tmpdir", tmpDir.map { it.asFile.absolutePath }.get())
    doFirst { tmpDir.get().asFile.mkdirs() }
}
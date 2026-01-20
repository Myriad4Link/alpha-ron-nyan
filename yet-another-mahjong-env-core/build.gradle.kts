plugins {
    kotlin("jvm")
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.ksp)
}

group = "xyz.uthofficial"
version = "1.0-SNAPSHOT"

dependencies {
    api(project(":yet-another-mahjong-env-api"))
    implementation(libs.dagger)
    ksp(libs.daggerCompiler)
    api(libs.slf4jApi)
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.logbackClassic)
}

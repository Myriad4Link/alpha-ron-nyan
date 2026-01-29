plugins {
    id("buildsrc.convention.kotlin-jvm")
    alias(libs.plugins.kover)
}

group = "xyz.uthofficial"
version = "unspecified"

dependencies {
    api(libs.slf4jApi)
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.logbackClassic)
}
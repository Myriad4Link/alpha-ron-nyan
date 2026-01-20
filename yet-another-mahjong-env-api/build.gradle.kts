plugins {
    kotlin("jvm")
    id("buildsrc.convention.kotlin-jvm")
}

group = "xyz.uthofficial"
version = "unspecified"

dependencies {
    api(libs.slf4jApi)
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.logbackClassic)

}
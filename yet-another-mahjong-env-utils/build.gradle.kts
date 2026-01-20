plugins {
    id("buildsrc.convention.kotlin-jvm")
    kotlin("jvm")
}

group = "xyz.uthofficial"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":yet-another-mahjong-env-api"))

    api(libs.slf4jApi)
    testImplementation(libs.bundles.kotest)
    testImplementation(libs.logbackClassic)

    implementation(libs.symbolProcessingApi)
    implementation(libs.kotlinPoet)
    implementation(libs.kotlinPoetKsp)
}

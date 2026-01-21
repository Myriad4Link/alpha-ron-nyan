plugins {
    id("buildsrc.convention.kotlin-jvm")
    kotlin("jvm")
    alias(libs.plugins.ksp)
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
    testImplementation(libs.kotlinCompileTestingKsp)
    testImplementation(libs.kotlinCompileTestingCore)
    testImplementation(kotlin("reflect"))

    implementation(libs.symbolProcessingApi)
    implementation(libs.kotlinPoet)
    implementation(libs.kotlinPoetKsp)

    compileOnly(libs.autoServiceAnnotations)
    ksp(libs.autoServiceKsp)
}


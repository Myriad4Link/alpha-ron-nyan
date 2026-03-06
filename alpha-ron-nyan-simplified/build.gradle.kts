plugins {
    kotlin("jvm")
    id("buildsrc.convention.kotlin-jvm")
}

group = "xyz.uthofficial"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":yet-another-mahjong-env-core"))
    implementation(project(":yet-another-mahjong-env-api"))
    implementation(libs.bundles.djlWithPyTorch)
    runtimeOnly(libs.djlPyTorchJni)
    runtimeOnly(libs.djlPyTorchNativeCpu) {
        artifact {
            classifier = "win-x86_64"
            type = "jar"
        }
    }

    api(libs.slf4jApi)
    testImplementation(libs.logbackClassic)
    testImplementation(libs.bundles.kotest)
    testImplementation(project(":yet-another-mahjong-env-core"))
}

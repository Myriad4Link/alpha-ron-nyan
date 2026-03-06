plugins {
    kotlin("jvm")
    id("buildsrc.convention.kotlin-jvm")
    application
}

application {
    mainClass.set("xyz.uthofficial.arnyan.simplified.EvolutionaryTrainerMainKt")
}

tasks.register<JavaExec>("quickTest") {
    group = "verification"
    description = "Run evolutionary trainer with quick test settings (1 generation, 1 game)"
    mainClass.set("xyz.uthofficial.arnyan.simplified.EvolutionaryTrainerMainKt")
    classpath = sourceSets["main"].runtimeClasspath
    args = listOf("--quickTest")
    jvmArgs = listOf("-Dfile.encoding=UTF-8")
}

group = "xyz.uthofficial"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":yet-another-mahjong-env-core"))
    implementation(project(":yet-another-mahjong-env-api"))
    implementation(project(":yet-another-mahjong-env-demo"))
    implementation(libs.bundles.djlFull)
    runtimeOnly(libs.djlPyTorchJni)
    runtimeOnly(libs.djlPyTorchNativeCpu) {
        artifact {
            classifier = "win-x86_64"
            type = "jar"
        }
    }
    runtimeOnly(libs.djlPyTorchNativeCpu) {
        artifact {
            classifier = "linux-x86_64"
            type = "jar"
        }
    }

    api(libs.slf4jApi)
    implementation(libs.logbackClassic)
    implementation(libs.kotlinxCoroutines)
    testImplementation(libs.bundles.kotest)
    testImplementation(project(":yet-another-mahjong-env-core"))
}

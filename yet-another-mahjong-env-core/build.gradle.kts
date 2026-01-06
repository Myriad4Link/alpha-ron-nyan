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

    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}
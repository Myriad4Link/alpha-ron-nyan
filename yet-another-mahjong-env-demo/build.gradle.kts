plugins {
    kotlin("jvm")
    application
}

group = "xyz.uthofficial"
version = "unspecified"

repositories {
    mavenCentral()
}

dependencies {
    implementation(project(":yet-another-mahjong-env-core"))
    implementation(project(":yet-another-mahjong-env-api"))
    testImplementation(kotlin("test"))
}

kotlin {
    jvmToolchain(25)
}

tasks.withType<JavaExec> {
    jvmArgs = listOf("-Dfile.encoding=UTF-8")
}

tasks.test {
    useJUnitPlatform()
}

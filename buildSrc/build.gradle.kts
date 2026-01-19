plugins {
    `kotlin-dsl`
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

kotlin {
    jvmToolchain(25)
}


dependencies {
    implementation(libs.kotlinGradlePlugin)
}

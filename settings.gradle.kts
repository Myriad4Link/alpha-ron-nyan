// The settings file is the entry point of every Gradle build.
// Its primary purpose is to define the subprojects.
// It is also used for some aspects of project-wide configuration, like managing plugins, dependencies, etc.
// https://docs.gradle.org/current/userguide/settings_file_basics.html

dependencyResolutionManagement {
    // Use Maven Central as the default repository (where Gradle will download dependencies) in all subprojects.
    @Suppress("UnstableApiUsage")
    repositories {
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
    id("de.fayard.refreshVersions") version "0.60.6"
}

// Include the `app` and `utils` subprojects in the build.
// If there are changes in only one of the projects, Gradle will rebuild only the one that has changed.
// Learn more about structuring projects with Gradle - https://docs.gradle.org/8.7/userguide/multi_project_builds.html

rootProject.name = "alpha-ron-nyan"
include("yet-another-mahjong-env-core")
include("yet-another-mahjong-env-utils")
include("yet-another-mahjong-env-api")
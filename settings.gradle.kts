// The settings file is the entry point of every Gradle build.
// Its primary purpose is to define the subprojects.
// It is also used for some aspects of project-wide configuration, like managing plugins, dependencies, etc.
// https://docs.gradle.org/current/userguide/settings_file_basics.html

pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
    plugins {
        // Kotlin plugins
        kotlin("jvm") version "1.9.25"
        kotlin("android") version "1.9.25"
        kotlin("multiplatform") version "1.9.25"

        // Android Gradle Plugin
        id("com.android.application") version "8.5.2"
        id("com.android.library") version "8.5.2"

        // Compose Multiplatform Gradle plugin
        id("org.jetbrains.compose") version "1.7.1"
    }
}

dependencyResolutionManagement {
    // Use Maven Central as the default repository (where Gradle will download dependencies) in all subprojects.
    @Suppress("UnstableApiUsage")
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

plugins {
    // Use the Foojay Toolchains plugin to automatically download JDKs required by subprojects.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

// Include the `app` and `utils` subprojects in the build.
// If there are changes in only one of the projects, Gradle will rebuild only the one that has changed.
// Learn more about structuring projects with Gradle - https://docs.gradle.org/8.7/userguide/multi_project_builds.html
rootProject.name = "Aurora"

include(":app")
include(":utils")

// Add new Aurora modules
include(":shared")
include(":desktopApp")
// Temporarily excluding androidApp due to Kotlin/AGP compatibility issues
// include(":androidApp")
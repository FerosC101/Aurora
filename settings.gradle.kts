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
        // Kotlin plugins - Use 1.9.20 for better compatibility
        kotlin("jvm") version "1.9.20"
        kotlin("android") version "1.9.20"
        kotlin("multiplatform") version "1.9.20"

        // Android Gradle Plugin - Use 8.1.4 for stability with Gradle 8.2
        id("com.android.application") version "8.1.4"
        id("com.android.library") version "8.1.4"

        // Compose Multiplatform Gradle plugin - 1.5.10 is stable with Kotlin 1.9.20 and Gradle 8.2
        id("org.jetbrains.compose") version "1.5.10"
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

// Temporarily excluding these modules
// include(":app")
// include(":utils")

// Add new Aurora modules
// Temporarily excluding shared and desktopApp modules due to Compose Multiplatform/Gradle compatibility issues
// The androidApp is self-contained and doesn't depend on these modules
// include(":shared")
// include(":desktopApp")
include(":androidApp")
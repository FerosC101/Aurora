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
        // Kotlin plugins - Upgraded to 2.0.0 for Gradle 8.12 compatibility
        kotlin("jvm") version "2.0.0"
        kotlin("android") version "2.0.0"
        kotlin("multiplatform") version "2.0.0"
        kotlin("plugin.serialization") version "2.0.0"

        // Android Gradle Plugin
        id("com.android.application") version "8.10.1"
        id("com.android.library") version "8.10.1"

        // Compose Multiplatform Gradle plugin - 1.7.0 for full Gradle 8.12 support
        id("org.jetbrains.compose") version "1.7.0"
        id("org.jetbrains.kotlin.plugin.compose") version "2.0.0"
        
        // Google Services for Firebase
        id("com.google.gms.google-services") version "4.4.0" apply false
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
include(":shared")
include(":desktopApp")
include(":androidApp")
// iOS app will be built separately using Xcode
// See iosApp/README.md for setup instructions
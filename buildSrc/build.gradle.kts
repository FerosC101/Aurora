plugins {
    `kotlin-dsl`
}

repositories {
    // Required for resolving Gradle plugins used by buildSrc convention plugins
    gradlePluginPortal()
    mavenCentral()
    google()
}

dependencies {
    // Make external Gradle plugins available to precompiled script plugins
    implementation(libs.kotlinGradlePlugin)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
    }
}

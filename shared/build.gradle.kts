plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization") version "2.0.0"
    id("org.jetbrains.compose")
    id("org.jetbrains.kotlin.plugin.compose")
}

group = "org.example"
version = "unspecified"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

// Dependencies moved to sourceSets
// dependencies {
//     testImplementation(kotlin("test"))
// }

// Test configuration moved to appropriate targets
// tasks.test {
//     useJUnitPlatform()
// }

kotlin {
    // Configure JVM toolchain at extension level (Kotlin 2.0 requirement)
    jvmToolchain(17)
    
    jvm("desktop")
    
    // iOS targets
    iosX64()
    iosArm64()
    iosSimulatorArm64()
    
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material3)
                implementation(compose.ui)
                implementation(compose.materialIconsExtended)
                
                // Coroutines
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                
                // DateTime
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                
                // Serialization
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
                
                // Ktor Client for HTTP requests
                implementation("io.ktor:ktor-client-core:2.3.7")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
                
                // Security - BCrypt for password hashing
                implementation("org.mindrot:jbcrypt:0.4")
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.common)
                
                // SQLite database
                implementation("org.xerial:sqlite-jdbc:3.45.0.0")
                
                // Ktor Client engine for JVM
                implementation("io.ktor:ktor-client-cio:2.3.7")
                
                // JavaFX for WebView (interactive maps)
                val javaFxVersion = "17.0.2"
                val osName = System.getProperty("os.name").lowercase()
                val platform = when {
                    osName.contains("win") -> "win"
                    osName.contains("mac") -> "mac"
                    else -> "linux"
                }
                implementation("org.openjfx:javafx-base:$javaFxVersion:$platform")
                implementation("org.openjfx:javafx-controls:$javaFxVersion:$platform")
                implementation("org.openjfx:javafx-graphics:$javaFxVersion:$platform")
                implementation("org.openjfx:javafx-web:$javaFxVersion:$platform")
                implementation("org.openjfx:javafx-swing:$javaFxVersion:$platform")
            }
        }
        
        val iosX64Main by getting
        val iosArm64Main by getting
        val iosSimulatorArm64Main by getting
        val iosMain by creating {
            dependsOn(commonMain)
            iosX64Main.dependsOn(this)
            iosArm64Main.dependsOn(this)
            iosSimulatorArm64Main.dependsOn(this)
            dependencies {
                // Ktor Client engine for iOS
                implementation("io.ktor:ktor-client-darwin:2.3.7")
            }
        }
    }
}
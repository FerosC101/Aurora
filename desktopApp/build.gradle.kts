import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm")
    id("org.jetbrains.compose")
}

// Configure JVM toolchain to use Java 17
kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":shared"))
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.8.0")
}

compose.desktop {
    application {
        mainClass = "org.aurora.desktop.MainKt"
        
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "Aurora"
            packageVersion = "1.0.0"
            
            windows {
                iconFile.set(project.file("icon.ico"))
            }
            linux {
                iconFile.set(project.file("icon.png"))
            }
            macOS {
                iconFile.set(project.file("icon.icns"))
            }
        }
    }
}
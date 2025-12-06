plugins {
    kotlin("multiplatform")
    // Temporarily disabled due to BaseVariant API removal in AGP 8.x
    // id("com.android.library")
    id("org.jetbrains.compose")
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
    // Android target temporarily disabled
    // androidTarget {
    //     compilations.all {
    //         kotlinOptions {
    //             jvmTarget = "17"
    //         }
    //     }
    // }
    
    jvm("desktop") {
        jvmToolchain(17)
    }
    
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
                
                // Security - BCrypt for password hashing
                implementation("org.mindrot:jbcrypt:0.4")
            }
        }
        
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
            }
        }
        
        // Android source set temporarily disabled
        // val androidMain by getting {
        //     dependencies {
        //         implementation("androidx.core:core-ktx:1.12.0")
        //     }
        // }
        
        val desktopMain by getting {
            dependencies {
                implementation(compose.desktop.common)
                
                // SQLite database
                implementation("org.xerial:sqlite-jdbc:3.45.0.0")
            }
        }
    }
}

// Android configuration temporarily disabled
// android {
//     namespace = "org.aurora.shared"
//     compileSdk = 34
//     
//     defaultConfig {
//         minSdk = 24
//     }
//     
//     compileOptions {
//         sourceCompatibility = JavaVersion.VERSION_17
//         targetCompatibility = JavaVersion.VERSION_17
//     }
// }
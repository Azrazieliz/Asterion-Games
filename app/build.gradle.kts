plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
val ciDebugKey = rootProject.file(".ci-signing/debug.keystore")
android {
    signingConfigs {
        getByName("debug") {
            if (ciDebugKey.isFile) {
                storeFile = ciDebugKey
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }
    namespace = "com.gamepilot"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.gamepilot"
        minSdk = 29
        targetSdk = 35
        versionCode = 4
        versionName = "0.3.1"
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("com.google.mlkit:text-recognition:16.0.1")
    implementation("com.google.mlkit:text-recognition-chinese:16.0.1")
    implementation("com.google.mlkit:text-recognition-japanese:16.0.1")
    testImplementation("junit:junit:4.13.2")
}

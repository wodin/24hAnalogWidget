plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "info.staticfree.android.twentyfourhour"
    compileSdk = 36

    defaultConfig {
        applicationId = "info.staticfree.android.twentyfourhour"
        minSdk = 26
        targetSdk = 36
        versionCode = 10
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

dependencies {
    // jsuntimes: local jar, no upstream coordinates.
    implementation(fileTree("libs") { include("*.jar") })
    implementation(libs.androidx.annotation)

    testImplementation(libs.junit)
}

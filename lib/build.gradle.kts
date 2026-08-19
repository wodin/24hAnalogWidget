plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "info.staticfree.android.twentyfourhour.lib"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
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
}

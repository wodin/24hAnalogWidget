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

    // Robolectric needs the merged resources and the manifest to inflate
    // drawables; without this the Face PNG resolves to null off-device.
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    // The build refuses to regress. abortOnError is already the default, so
    // errors were never the gap - warnings were, and they exited 0 unread.
    // Anything a later candidate owns is suppressed at its site, naming the
    // candidate, so this list stays short enough to read.
    lint {
        warningsAsErrors = true

        disable += setOf(
            // Fire on the calendar rather than on the code. Candidate A pinned
            // these deliberately; Google shipping a release must not turn the
            // build red on its own.
            "AndroidGradlePluginVersion",
            "GradleDependency",
            "OldTargetApi",

            // Correct, and unfixable without source art this fork does not
            // have. Upscaling the xhdpi set would add bytes and no detail,
            // which is what the runtime already does for free.
            "IconMissingDensityFolder",
        )
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

// Deprecations are made visible, not fatal: a future Android release
// deprecating an API we use must not break the build unattended.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:deprecation")
}

dependencies {
    // jsuntimes: local jar, no upstream coordinates.
    implementation(fileTree("libs") { include("*.jar") })
    implementation(libs.androidx.annotation)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}

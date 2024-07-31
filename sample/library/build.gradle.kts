plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.badoo.marathon")
}

android {
    compileSdk = 34
    namespace = "com.example.library"

    defaultConfig {
        minSdk = 21

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        targetSdk = 34
    }

    testOptions {
        targetSdk = 34
    }
}

dependencies {
    implementation(Libraries.appCompat)
    implementation(Libraries.constraintLayout)
    androidTestImplementation(TestLibraries.androidxTestRunner)
    androidTestImplementation(TestLibraries.androidxTestJUnit)
}

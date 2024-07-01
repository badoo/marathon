plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("marathon")
}

android {
    compileSdk = 34
    namespace = "com.example"

    defaultConfig {
        minSdk = 21
        targetSdk = 34

        applicationId = "com.example"
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles("proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(Libraries.appCompat)
    implementation(Libraries.constraintLayout)
    androidTestImplementation(TestLibraries.androidxTestRunner)
    androidTestImplementation(TestLibraries.androidxTestJUnit)
}

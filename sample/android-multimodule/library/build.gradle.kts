plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("marathon")
}

android {
    compileSdkVersion(28)

    defaultConfig {
        minSdkVersion(16)
        targetSdkVersion(27)

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

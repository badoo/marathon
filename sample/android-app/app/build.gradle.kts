plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("marathon")
}

android {
    compileSdkVersion(28)

    defaultConfig {
        minSdkVersion(16)
        targetSdkVersion(27)

        applicationId = "com.example"
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

marathon {
    instrumentationArgs {
        put("debug", "false")
    }
}

dependencies {
    implementation(Libraries.appCompat)
    implementation(Libraries.constraintLayout)
    androidTestImplementation(TestLibraries.androidxTestRunner)
    androidTestImplementation(TestLibraries.androidxTestJUnit)
    androidTestImplementation(TestLibraries.espressoCore)
    androidTestImplementation(TestLibraries.kakao)
}

plugins {
    id("com.android.library")
    id("com.badoo.marathon")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

android {
    namespace = "com.example.library"

    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["fromDefaultConfig"] = "default"
    }
}

androidComponents {
    onVariants { variant ->
        variant.androidTest?.instrumentationRunnerArguments?.put("fromVariant", variant.name)
    }
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)

    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.junit)
}

object Versions {
    val kotlin = "1.3.61"
    val coroutines = "1.3.3"

    val androidGradleVersion = "4.0.0"

    val kakao = "1.2.1"
    val espresso = "3.0.1"
    val espressoRules = "1.0.1"
    val espressoRunner = "1.0.1"
    val junit = "4.12"
    val appCompat = "28.0.0"
    val constraintLayout = "1.0.2"
}

object BuildPlugins {
    val kotlinPlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}"
    val androidGradle = "com.android.tools.build:gradle:${Versions.androidGradleVersion}"
}

object Libraries {
    val kotlinStdLib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}"
    val kotlinReflect = "org.jetbrains.kotlin:kotlin-reflect:${Versions.kotlin}"
    val kotlinCoroutines = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.coroutines}"
    val appCompat = "com.android.support:appcompat-v7:${Versions.appCompat}"
    val constraintLayout = "com.android.support.constraint:constraint-layout:${Versions.constraintLayout}"
}

object TestLibraries {
    val kakao = "com.agoda.kakao:kakao:${Versions.kakao}"

    val espressoRunner = "com.android.support.test:runner:${Versions.espressoRunner}"
    val espressoRules = "com.android.support.test:rules:${Versions.espressoRules}"
    val espressoCore = "com.android.support.test.espresso:espresso-core:${Versions.espresso}"
    val espressoWeb = "com.android.support.test.espresso:espresso-web:${Versions.espresso}"
    val espressoContrib = "com.android.support.test.espresso:espresso-contrib:${Versions.espresso}"
    val espressoIntents = "com.android.support.test.espresso:espresso-intents:${Versions.espresso}"
    val junit = "junit:junit:${Versions.junit}"
}

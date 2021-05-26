object Versions {
    val kakao = "2.4.0"
    val espressoCore = "3.3.0"
    val androidxTest = "1.3.0"
    val androidxTestJUnit = "1.1.2"
    val appCompat = "1.3.0"
    val constraintLayout = "2.0.4"
}

object Libraries {
    val appCompat = "androidx.appcompat:appcompat:${Versions.appCompat}"
    val constraintLayout = "androidx.constraintlayout:constraintlayout:${Versions.constraintLayout}"
}

object TestLibraries {
    val kakao = "com.agoda.kakao:kakao:${Versions.kakao}"

    val androidxTestRunner = "androidx.test:runner:${Versions.androidxTest}"
    val androidxTestJUnit = "androidx.test.ext:junit:${Versions.androidxTestJUnit}"
    val espressoCore = "androidx.test.espresso:espresso-core:${Versions.espressoCore}"
}

plugins {
    id("com.android.application") apply false
    id("com.android.library") apply false
    id("com.badoo.marathon")
}

marathon {
    ignoreFailures = true
}

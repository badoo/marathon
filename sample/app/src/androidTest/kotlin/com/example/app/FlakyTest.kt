package com.example.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Fails on the first attempt and passes on the second, using a counter file in the
 * target app's private storage that survives across Marathon retries (no pm-clear by default).
 * Reset before a run with: adb shell pm clear com.example.app
 */
@RunWith(AndroidJUnit4::class)
class FlakyTest {
    @Test
    fun flakyPassesOnRetry() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val marker = File(context.filesDir, "flaky_attempt.txt")
        val attempt = (marker.takeIf { it.exists() }?.readText()?.trim()?.toIntOrNull() ?: 0) + 1
        marker.writeText(attempt.toString())
        if (attempt < 2) {
            fail("Deliberate failure on attempt $attempt; expected to pass on retry")
        }
    }
}

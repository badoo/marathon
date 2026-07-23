package com.example.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assume.assumeTrue
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IgnoredTest {
    @Test
    @Ignore
    fun ignoredTest() {
        throw AssertionError("an @Ignore test must never execute")
    }

    @Test
    fun assumedIgnoredTest() {
        assumeTrue(false)
    }
}

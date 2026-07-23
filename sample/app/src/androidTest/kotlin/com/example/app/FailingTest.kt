package com.example.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FailingTest {
    @Test
    fun failing() {
        Thread.sleep(1000)
        throw AssertionError()
    }
}

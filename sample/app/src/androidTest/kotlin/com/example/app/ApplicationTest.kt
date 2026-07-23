package com.example.app

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ApplicationTest {
    @Test
    fun test1() {
        Thread.sleep(1000)
    }

    @Test
    fun test2() {
        Thread.sleep(1000)
    }

    @Test
    fun test3() {
        Thread.sleep(1000)
    }

    @Test
    fun test4() {
        Thread.sleep(1000)
    }

    @Test
    fun instantTest() {
    }

    @Test
    fun instrumentationArgs() {
        val arguments = InstrumentationRegistry.getArguments()
        assertEquals("default", arguments.getString("fromDefaultConfig"))
        assertEquals("debug", arguments.getString("fromVariant"))
    }
}

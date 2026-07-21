package com.library

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LibraryTest {
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
    fun test5Failed() {
        Thread.sleep(1000)
        throw AssertionError()
    }

    @Test
    fun instrumentationArgs() {
        val arguments = InstrumentationRegistry.getArguments()
        assertEquals("default", arguments.getString("fromDefaultConfig"))
        assertEquals("debug", arguments.getString("fromVariant"))
    }
}

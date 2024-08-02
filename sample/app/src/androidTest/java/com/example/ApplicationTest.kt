package com.example

import androidx.test.ext.junit.runners.AndroidJUnit4
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
    fun test5Failed() {
        Thread.sleep(1000)
        throw AssertionError()
    }
}

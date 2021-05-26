package com.example

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.fail
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@Ignore
class ClassIgnoredTest {

    @Rule
    @JvmField
    val rule = ActivityScenarioRule(MainActivity::class.java)

    val screen = MainScreen()

    @Test
    fun testAlwaysIgnored() {
        fail("Should've been ignored because of @Ignore on a class")
    }
}

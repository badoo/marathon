package com.example

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TestWithWrongName {

    @Rule
    @JvmField
    val rule = ActivityScenarioRule(MainActivity::class.java)

    val screen = MainScreen()

    @Test
    fun testAlwaysFailing() {
        assertTrue(false)
    }
}

package com.example

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.qameta.allure.*
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@Epic("Marathon")
@Feature("Flakiness")
@Owner("user2")
@Severity(SeverityLevel.BLOCKER)
@Story("Flaky")
@RunWith(AndroidJUnit4::class)
class MainActivityFlakyTest {

    @Rule
    @JvmField
    val rule = ActivityScenarioRule(MainActivity::class.java)

    val screen = MainScreen()

    @Test
    fun testTextFlaky() {
        assertTrue(Random().nextBoolean())
    }

    @Test
    fun testTextFlaky1() {
        Thread.sleep(100)
        assertTrue(Random().nextBoolean())
    }

    @Test
    fun testTextFlaky2() {
        Thread.sleep(100)
        assertTrue(Random().nextBoolean())
    }

    @Test
    fun testTextFlaky3() {
        Thread.sleep(100)
        assertTrue(Random().nextBoolean())
    }

    @Test
    fun testTextFlaky4() {
        Thread.sleep(100)
        assertTrue(Random().nextBoolean())
    }

    @Test
    fun testTextFlaky5() {
        Thread.sleep(100)
        assertTrue(Random().nextBoolean())
    }

    @Test
    fun testTextFlaky6() {
        Thread.sleep(100)
        assertTrue(Random().nextBoolean())
    }

    @Test
    fun testTextFlaky7() {
        Thread.sleep(100)
        assertTrue(Random().nextBoolean())
    }

    @Test
    fun testTextFlaky8() {
        Thread.sleep(100)
        assertTrue(Random().nextBoolean())
    }
}

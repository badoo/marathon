package com.example

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.qameta.allure.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Epic("Marathon")
@Feature("Slow")
@Owner("user1")
@Severity(SeverityLevel.BLOCKER)
@Story("Slow")
@RunWith(AndroidJUnit4::class)
class MainActivitySlowTest {

    @Rule
    @JvmField
    val rule = ActivityScenarioRule(MainActivity::class.java)

    val screen = MainScreen()

    @Test
    fun testTextSlow() {
        Thread.sleep(5000)
        screen {
            text {
                hasText("Test")
            }
        }
    }

    @Test
    fun testTextSlow1() {
        Thread.sleep(5000)
        screen {
            text {
                hasText("Test")
            }
        }
    }

    @Test
    fun testTextSlow2() {
        Thread.sleep(5000)
        screen {
            text {
                hasText("Test")
            }
        }
    }

    @Test
    fun testTextSlow3() {
        Thread.sleep(5000)
        screen {
            text {
                hasText("Test")
            }
        }
    }

    @Test
    fun testTextSlow4() {
        Thread.sleep(5000)
        screen {
            text {
                hasText("Test")
            }
        }
    }
}

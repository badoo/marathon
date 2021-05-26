package com.example

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.qameta.allure.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Epic("General")
@Feature("Text on main screen")
@Story("Slow")
@Owner("user2")
@Severity(SeverityLevel.CRITICAL)
@RunWith(AndroidJUnit4::class)
class MainActivityTest {

    @Rule
    @JvmField
    val rule = ActivityScenarioRule(MainActivity::class.java)

    val screen = MainScreen()

    @Test
    fun testText() {
        Thread.sleep(500)
        screen {
            text {
                hasText("Test")
            }
        }
    }

    @Test
    fun testText1() {
        Thread.sleep(500)
        screen {
            text {
                hasText("Test")
            }
        }
    }

    @Test
    fun testText2() {
        Thread.sleep(500)
        screen {
            text {
                hasText("Test")
            }
        }
    }

    @Test
    fun testText3() {
        Thread.sleep(500)
        screen {
            text {
                hasText("Test")
            }
        }
    }

    @Test
    fun testText4() {
        Thread.sleep(500)
        screen {
            text {
                hasText("Test")
            }
        }
    }

    @Test
    fun testText5() {
        Thread.sleep(500)
        screen {
            text {
                hasText("Test")
            }
        }
    }

    @Test
    fun testText6() {
        Thread.sleep(500)
        screen {
            text {
                hasText("Test")
            }
        }
    }

    @Test
    fun testText7() {
        Thread.sleep(500)
        screen {
            text {
                hasText("Test")
            }
        }
    }

    @Test
    fun testText8() {
        Thread.sleep(500)
        screen {
            text {
                hasText("Test")
            }
        }
    }

    @Test
    fun testText9() {
        Thread.sleep(500)
        screen {
            text {
                hasText("Test")
            }
        }
    }
}

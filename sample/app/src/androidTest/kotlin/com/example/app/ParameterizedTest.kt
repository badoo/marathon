package com.example.app

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

@RunWith(Parameterized::class)
class ParameterizedTest(private val input: Int) {
    @Test
    fun isPositive() {
        assertTrue("input should be positive", input > 0)
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): List<Int> = listOf(1, 2, 3)
    }
}

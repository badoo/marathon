package com.malinskiy.marathon.time

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.time.Clock

class SystemTimerTest {
    private val clock = mock<Clock>()
    private val timer = SystemTimer(clock)

    @Test
    fun `should call passed clock to get currentTimeMillis`() {
        whenever(clock.millis()).thenReturn(100)

        val result = timer.currentTimeMillis()

        assertThat(result).isEqualTo(100)
    }

    @Test
    fun `should call passed clock to measure`() {
        var counter = 0L
        whenever(clock.millis()).thenAnswer {
            counter++ * 1000L
        }

        val result = timer.measure { }

        assertThat(result).isEqualTo(1000L)
    }
}

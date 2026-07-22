package com.malinskiy.marathon.android.executor.listeners.video

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class ScreenRecorderHandlerTest {
    private val handler = ScreenRecorderHandler()
    private var stopCount = 0

    @Test
    fun `notifies listener on stop`() {
        handler.subscribeOnStop { stopCount++ }
        handler.stop()

        assertThat(stopCount).isEqualTo(1)
    }

    @Test
    fun `notifies listener immediately when subscribing after stop`() {
        handler.stop()
        handler.subscribeOnStop { stopCount++ }

        assertThat(stopCount).isEqualTo(1)
    }

    @Test
    fun `keeps listener subscribed when subscribing after stop`() {
        handler.stop()
        handler.subscribeOnStop { stopCount++ }
        handler.stop()

        assertThat(stopCount).isEqualTo(2)
    }
}

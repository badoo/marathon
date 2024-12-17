package com.malinskiy.marathon.android

import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.android.ddmlib.DdmlibDeviceProvider
import com.malinskiy.marathon.test.factory.ConfigurationFactory
import com.malinskiy.marathon.time.SystemTimer
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import java.time.Clock

class DdmlibDeviceProviderTest {
    @Test
    fun `GIVEN device provider WHEN terminating device provider THEN device events flow gets stopped`() = runTest {
        val config = ConfigurationFactory().build()
        val provider = DdmlibDeviceProvider(Track(), SystemTimer(Clock.systemDefaultZone()), config, mock(), mock(), mock(), mock(), mock())

        val eventReceiver = backgroundScope.launch {
            provider.deviceEvents.collect()
        }

        provider.terminate()

        advanceTimeBy(1L)

        assertTrue(eventReceiver.isCompleted)
    }
}

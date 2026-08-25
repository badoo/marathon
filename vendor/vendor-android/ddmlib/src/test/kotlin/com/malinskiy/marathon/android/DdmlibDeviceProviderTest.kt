package com.malinskiy.marathon.android

import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.android.ddmlib.DdmlibDeviceProvider
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.test.factory.configuration
import com.malinskiy.marathon.time.StubTimer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class DdmlibDeviceProviderTest {
    private val timer = StubTimer()

    @Test
    fun `GIVEN device provider WHEN terminating device provider THEN device events flow gets stopped`() = runTest {
        val config = configuration()
        val provider = createDdmlibDeviceProvider(config)

        val eventReceiver = backgroundScope.launch {
            provider.deviceEvents.collect()
        }

        provider.terminate()

        advanceTimeBy(1.milliseconds)

        assertThat(eventReceiver.isCompleted).isTrue()
    }

    private fun TestScope.createDdmlibDeviceProvider(configuration: Configuration) = DdmlibDeviceProvider(
        track = Track(),
        timer = timer,
        config = configuration,
        androidAppInstaller = mock(),
        fileManager = mock(),
        strictRunChecker = mock(),
        logcatListener = mock(),
        attachmentManager = mock(),
        ioDispatcher = StandardTestDispatcher(testScheduler),
    )
}

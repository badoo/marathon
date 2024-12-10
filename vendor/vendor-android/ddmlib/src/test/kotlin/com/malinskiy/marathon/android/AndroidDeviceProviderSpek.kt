package com.malinskiy.marathon.android

import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.android.ddmlib.DdmlibDeviceProvider
import com.malinskiy.marathon.test.factory.ConfigurationFactory
import com.malinskiy.marathon.time.SystemTimer
import kotlinx.coroutines.DelicateCoroutinesApi
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.Assertions.assertTrue
import org.mockito.kotlin.mock
import java.time.Clock

@OptIn(DelicateCoroutinesApi::class)
class AndroidDeviceProviderSpek : Spek({
    given("A provider") {
        group("terminate") {
            it("should close the channel") {
                val config = ConfigurationFactory().build()
                val provider = DdmlibDeviceProvider(Track(), SystemTimer(Clock.systemDefaultZone()), config, mock(), mock(), mock(), mock(), mock())

                provider.close()

                assertTrue(provider.subscribe().isClosedForReceive)
                assertTrue(provider.subscribe().isClosedForSend)
            }
        }
    }
})

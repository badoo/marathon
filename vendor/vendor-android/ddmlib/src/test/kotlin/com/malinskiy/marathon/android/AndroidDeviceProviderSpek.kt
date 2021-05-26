package com.malinskiy.marathon.android

import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.android.ddmlib.DdmlibDeviceProvider
import com.malinskiy.marathon.test.factory.ConfigurationFactory
import com.malinskiy.marathon.time.SystemTimer
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.mock
import org.amshove.kluent.shouldEqual
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.given
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on
import java.time.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidDeviceProviderSpek : Spek(
    {
        given("A provider") {
            on("terminate") {
                it("should close the channel") {
                    val config = ConfigurationFactory().build()
                    val provider = DdmlibDeviceProvider(Track(), SystemTimer(Clock.systemDefaultZone()), config, mock(), mock(), mock(), mock(), mock())

                    runBlocking {
                        provider.terminate()
                    }

                    provider.subscribe().isClosedForReceive shouldEqual true
                    provider.subscribe().isClosedForSend shouldEqual true
                }
            }
        }
    })

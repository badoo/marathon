package com.malinskiy.marathon.ios

import com.malinskiy.marathon.analytics.internal.pub.Track
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
class IOSDeviceProviderSpek : Spek(
    {
        given("A provider") {
            val provider = IOSDeviceProvider(Track(), mock(), SystemTimer(Clock.systemDefaultZone()))

            on("terminate") {
                it("should close the channel") {
                    runBlocking {
                        provider.terminate()
                    }

                    provider.subscribe().isClosedForReceive shouldEqual true
                    provider.subscribe().isClosedForSend shouldEqual true
                }
            }
        }
    })

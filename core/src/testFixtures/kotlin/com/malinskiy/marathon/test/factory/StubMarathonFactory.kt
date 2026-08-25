package com.malinskiy.marathon.test.factory

import com.malinskiy.marathon.Marathon
import com.malinskiy.marathon.MarathonFactory
import com.malinskiy.marathon.di.DefaultMarathonFactory
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.time.StubTimer
import com.malinskiy.marathon.time.Timer
import kotlinx.coroutines.CoroutineDispatcher

class StubMarathonFactory(private val dispatcher: CoroutineDispatcher) : MarathonFactory {
    internal val configurationFactory = ConfigurationFactory()
    var timer: Timer = StubTimer()

    fun configuration(block: ConfigurationFactory.() -> Unit) = configurationFactory.apply(block)

    override fun createMarathon(configuration: Configuration): Marathon = DefaultMarathonFactory(
        ioDispatcher = dispatcher,
        timer = timer,
    ).createMarathon(configuration)
}

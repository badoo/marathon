package com.malinskiy.marathon.test.factory

import com.malinskiy.marathon.Marathon
import com.malinskiy.marathon.di.marathonStartKoin
import com.malinskiy.marathon.time.Timer

class MarathonFactory {
    private val configurationFactory: ConfigurationFactory = ConfigurationFactory()

    var timer: Timer? = null

    fun configuration(block: ConfigurationFactory.() -> Unit) = configurationFactory.apply(block)

    fun build(): Marathon {
        val marathonStartKoin = marathonStartKoin(configurationFactory.build(), timer)
        return marathonStartKoin.koin.get()
    }
}

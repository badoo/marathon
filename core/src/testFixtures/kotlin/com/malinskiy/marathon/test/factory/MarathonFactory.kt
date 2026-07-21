package com.malinskiy.marathon.test.factory

import com.malinskiy.marathon.Marathon
import com.malinskiy.marathon.di.createMarathon
import com.malinskiy.marathon.time.Timer

class MarathonFactory {
    private val configurationFactory: ConfigurationFactory = ConfigurationFactory()

    var timer: Timer? = null

    fun configuration(block: ConfigurationFactory.() -> Unit) = configurationFactory.apply(block)

    fun build(): Marathon = createMarathon(configurationFactory.build(), timer)
}

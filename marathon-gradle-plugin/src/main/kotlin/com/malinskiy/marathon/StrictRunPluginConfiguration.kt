package com.malinskiy.marathon

import com.malinskiy.marathon.execution.StrictRunConfiguration
import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

interface StrictRunPluginConfiguration {
    val runs: Property<Int>

    @get:Nested
    val filter: FilterConfiguration

    fun filter(action: Action<FilterConfiguration>) {
        action.execute(filter)
    }
}

internal fun StrictRunPluginConfiguration.initDefaults() {
    runs.convention(1)
}

internal fun StrictRunPluginConfiguration.toStrictRunConfiguration(): StrictRunConfiguration =
    StrictRunConfiguration(filter.toList(), runs.get())

package com.malinskiy.marathon

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

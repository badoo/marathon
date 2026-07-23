package com.malinskiy.marathon

import org.gradle.api.Action
import org.gradle.api.tasks.Nested

interface FilteringPluginConfiguration {
    @get:Nested
    val whitelist: FilterConfiguration

    @get:Nested
    val blacklist: FilterConfiguration

    fun whitelist(action: Action<FilterConfiguration>) {
        action.execute(whitelist)
    }

    fun blacklist(action: Action<FilterConfiguration>) {
        action.execute(blacklist)
    }
}

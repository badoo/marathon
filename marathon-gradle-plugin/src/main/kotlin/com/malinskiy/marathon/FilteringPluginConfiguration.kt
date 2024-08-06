package com.malinskiy.marathon

import com.malinskiy.marathon.execution.FilteringConfiguration
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

internal fun FilteringPluginConfiguration.toFilteringConfiguration(): FilteringConfiguration =
    FilteringConfiguration(
        whitelist = whitelist.toList(),
        blacklist = blacklist.toList()
    )

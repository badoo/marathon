package com.malinskiy.marathon

import com.malinskiy.marathon.execution.FilteringConfiguration
import org.gradle.api.Action
import org.gradle.api.tasks.Nested

interface FilteringPluginConfiguration {
    @get:Nested
    val whitelist: FilterWrapper

    @get:Nested
    val blacklist: FilterWrapper

    fun whitelist(action: Action<FilterWrapper>) {
        action.execute(whitelist)
    }

    fun blacklist(action: Action<FilterWrapper>) {
        action.execute(blacklist)
    }
}

internal fun FilteringPluginConfiguration.toFilteringConfiguration(): FilteringConfiguration =
    FilteringConfiguration(
        whitelist = whitelist.toList(),
        blacklist = blacklist.toList()
    )

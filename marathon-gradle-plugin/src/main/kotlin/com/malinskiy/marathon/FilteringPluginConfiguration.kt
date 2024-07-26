package com.malinskiy.marathon

import com.malinskiy.marathon.execution.FilteringConfiguration
import org.gradle.api.Action

open class FilteringPluginConfiguration {
    var whitelist: FilterWrapper? = null
    var blacklist: FilterWrapper? = null

    fun whitelist(action: Action<FilterWrapper>) {
        whitelist = (whitelist ?: FilterWrapper()).also { action.execute(it) }
    }

    fun blacklist(action: Action<FilterWrapper>) {
        blacklist = (whitelist ?: FilterWrapper()).also { action.execute(it) }
    }
}

fun FilteringPluginConfiguration.toFilteringConfiguration(): FilteringConfiguration {
    val white = whitelist?.toList() ?: emptyList()
    val black = blacklist?.toList() ?: emptyList()
    return FilteringConfiguration(white, black)
}

package com.malinskiy.marathon

import com.malinskiy.marathon.execution.StrictRunFilterConfiguration
import org.gradle.api.Action

open class StrictRunFilterPluginConfiguration {
    var filter: FilterWrapper? = null
    var runs: Int = 1

    fun filter(action: Action<FilterWrapper>) {
        filter = (filter ?: FilterWrapper()).also { action.execute(it) }
    }
}

fun StrictRunFilterPluginConfiguration.toStrictRunFilterConfiguration(): StrictRunFilterConfiguration {
    val filter = filter?.toList() ?: emptyList()
    return StrictRunFilterConfiguration(filter, runs)
}

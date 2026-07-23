package com.malinskiy.marathon

import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

interface ShardingStrategyConfiguration {
    @get:Nested
    val countSharding: CountShardingStrategyConfiguration

    fun countSharding(action: Action<CountShardingStrategyConfiguration>) {
        countSharding.count.convention(1)
        action.execute(countSharding)
    }
}

interface CountShardingStrategyConfiguration {
    val count: Property<Int>
}

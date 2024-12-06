package com.malinskiy.marathon

import com.malinskiy.marathon.execution.strategy.ShardingStrategy
import com.malinskiy.marathon.execution.strategy.impl.sharding.CountShardingStrategy
import com.malinskiy.marathon.execution.strategy.impl.sharding.ParallelShardingStrategy
import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

interface ShardingStrategyConfiguration {
    @get:Nested
    val countSharding: CountShardingStrategyConfiguration

    fun countSharding(action: Action<CountShardingStrategyConfiguration>) {
        countSharding.initDefaults()
        action.execute(countSharding)
    }
}

interface CountShardingStrategyConfiguration {
    val count: Property<Int>
}

internal fun CountShardingStrategyConfiguration.initDefaults() {
    count.convention(1)
}

internal fun ShardingStrategyConfiguration.toStrategy(): ShardingStrategy =
    if (countSharding.count.isPresent) countSharding.toStrategy() else ParallelShardingStrategy()

private fun CountShardingStrategyConfiguration.toStrategy(): CountShardingStrategy =
    CountShardingStrategy(count.get())

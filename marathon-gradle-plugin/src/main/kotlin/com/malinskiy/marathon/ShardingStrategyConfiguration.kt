package com.malinskiy.marathon

import com.malinskiy.marathon.execution.strategy.ShardingStrategy
import com.malinskiy.marathon.execution.strategy.impl.sharding.CountShardingStrategy
import com.malinskiy.marathon.execution.strategy.impl.sharding.ParallelShardingStrategy
import org.gradle.api.Action

class ShardingStrategyConfiguration {
    var countSharding: CountShardingStrategyConfiguration? = null

    fun countSharding(action: Action<CountShardingStrategyConfiguration>) {
        countSharding = (countSharding ?: CountShardingStrategyConfiguration()).also { action.execute(it) }
    }
}

class CountShardingStrategyConfiguration {
    var count = 1
}

fun ShardingStrategyConfiguration.toStrategy(): ShardingStrategy = countSharding?.let {
    CountShardingStrategy(it.count)
} ?: ParallelShardingStrategy()

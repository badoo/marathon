package com.malinskiy.marathon

import com.malinskiy.marathon.execution.strategy.BatchingStrategy
import com.malinskiy.marathon.execution.strategy.impl.batching.FixedSizeBatchingStrategy
import com.malinskiy.marathon.execution.strategy.impl.batching.IsolateBatchingStrategy
import org.gradle.api.Action
import java.time.Instant

class BatchingStrategyConfiguration {
    var fixedSize: FixedSizeBatchingStrategyConfiguration? = null

    fun fixedSize(action: Action<FixedSizeBatchingStrategyConfiguration>) {
        fixedSize = (fixedSize ?: FixedSizeBatchingStrategyConfiguration()).also { action.execute(it) }
    }
}

class FixedSizeBatchingStrategyConfiguration {
    var size = 1
    var durationMillis: Long? = null
    var percentile: Double? = null
    var timeLimit: Instant? = null
    var lastMileLength: Int = 0
}

fun BatchingStrategyConfiguration.toStrategy(): BatchingStrategy = fixedSize?.let {
    FixedSizeBatchingStrategy(it.size, it.durationMillis, it.percentile, it.timeLimit, it.lastMileLength)
} ?: IsolateBatchingStrategy()

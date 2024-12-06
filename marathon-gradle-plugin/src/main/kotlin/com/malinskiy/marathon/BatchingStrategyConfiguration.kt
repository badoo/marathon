package com.malinskiy.marathon

import com.malinskiy.marathon.execution.strategy.BatchingStrategy
import com.malinskiy.marathon.execution.strategy.impl.batching.FixedSizeBatchingStrategy
import com.malinskiy.marathon.execution.strategy.impl.batching.IsolateBatchingStrategy
import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import java.time.Duration
import java.time.Instant

interface BatchingStrategyConfiguration {
    @get:Nested
    val fixedSize: FixedSizeBatchingStrategyConfiguration

    fun fixedSize(action: Action<FixedSizeBatchingStrategyConfiguration>) {
        fixedSize.initDefaults()
        action.execute(fixedSize)
    }
}

interface FixedSizeBatchingStrategyConfiguration {
    val size: Property<Int>
    val durationMillis: Property<Long>
    val percentile: Property<Double>
    val timeLimit: Property<Duration>
    val lastMileLength: Property<Int>
}

internal fun FixedSizeBatchingStrategyConfiguration.initDefaults() {
    size.convention(1)
    lastMileLength.convention(0)
}

internal fun BatchingStrategyConfiguration.toStrategy(): BatchingStrategy =
    if (fixedSize.size.isPresent) fixedSize.toStrategy() else IsolateBatchingStrategy()

private fun FixedSizeBatchingStrategyConfiguration.toStrategy(): FixedSizeBatchingStrategy =
    FixedSizeBatchingStrategy(
        size = size.get(),
        durationMillis = durationMillis.orNull,
        percentile = percentile.orNull,
        timeLimit = timeLimit.orNull?.let { Instant.now().minus(it) },
        lastMileLength = lastMileLength.get()
    )

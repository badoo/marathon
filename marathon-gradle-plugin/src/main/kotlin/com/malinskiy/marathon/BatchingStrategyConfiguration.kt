package com.malinskiy.marathon

import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import java.time.Duration

interface BatchingStrategyConfiguration {
    @get:Nested
    val fixedSize: FixedSizeBatchingStrategyConfiguration

    fun fixedSize(action: Action<FixedSizeBatchingStrategyConfiguration>) {
        fixedSize.size.convention(1)
        fixedSize.lastMileLength.convention(0)
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

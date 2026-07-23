package com.malinskiy.marathon

import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import java.time.Duration

interface SortingStrategyConfiguration {
    @get:Nested
    val executionTime: ExecutionTimeSortingStrategyConfiguration

    @get:Nested
    val successRate: SuccessRateSortingStrategyConfiguration

    @get:Nested
    val randomOrder: RandomOrderStrategyConfiguration

    fun executionTime(action: Action<ExecutionTimeSortingStrategyConfiguration>) {
        executionTime.percentile.convention(90.0)
        executionTime.timeLimit.convention(Duration.ofDays(30L))
        action.execute(executionTime)
    }

    fun successRate(action: Action<SuccessRateSortingStrategyConfiguration>) {
        successRate.limit.convention(Duration.ofDays(30L))
        successRate.ascending.convention(false)
        action.execute(successRate)
    }

    fun randomOrder(action: Action<RandomOrderStrategyConfiguration>) {
        randomOrder._initialized.convention(0)
        action.execute(randomOrder)
    }
}

interface ExecutionTimeSortingStrategyConfiguration {
    val percentile: Property<Double>
    val timeLimit: Property<Duration>
}

interface SuccessRateSortingStrategyConfiguration {
    val limit: Property<Duration>
    val ascending: Property<Boolean>
}

interface RandomOrderStrategyConfiguration {
    @Suppress("PropertyName", "VariableNaming")
    val _initialized: Property<Int>
}

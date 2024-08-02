package com.malinskiy.marathon

import com.malinskiy.marathon.execution.strategy.SortingStrategy
import com.malinskiy.marathon.execution.strategy.impl.sorting.ExecutionTimeSortingStrategy
import com.malinskiy.marathon.execution.strategy.impl.sorting.NoSortingStrategy
import com.malinskiy.marathon.execution.strategy.impl.sorting.RandomOrderSortingStrategy
import com.malinskiy.marathon.execution.strategy.impl.sorting.SuccessRateSortingStrategy
import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import java.time.Duration
import java.time.Instant

interface SortingStrategyConfiguration {
    @get:Nested
    val executionTime: ExecutionTimeSortingStrategyConfiguration

    @get:Nested
    val successRate: SuccessRateSortingStrategyConfiguration

    @get:Nested
    val randomOrder: RandomOrderStrategyConfiguration

    fun executionTime(action: Action<ExecutionTimeSortingStrategyConfiguration>) {
        executionTime.initDefaults()
        action.execute(executionTime)
    }

    fun successRate(action: Action<SuccessRateSortingStrategyConfiguration>) {
        successRate.initDefaults()
        action.execute(successRate)
    }

    fun randomOrder(action: Action<RandomOrderStrategyConfiguration>) {
        randomOrder.initDefaults()
        action.execute(randomOrder)
    }
}

interface ExecutionTimeSortingStrategyConfiguration {
    val percentile: Property<Double>
    val timeLimit: Property<Duration>

    fun initDefaults() {
        percentile.convention(DEFAULT_PERCENTILE)
        timeLimit.convention(Duration.ofDays(DEFAULT_DAYS_COUNT))
    }
}

interface SuccessRateSortingStrategyConfiguration {
    val limit: Property<Duration>
    val ascending: Property<Boolean>

    fun initDefaults() {
        limit.convention(Duration.ofDays(DEFAULT_DAYS_COUNT))
        ascending.convention(false)
    }
}

interface RandomOrderStrategyConfiguration {
    @Suppress("PropertyName", "VariableNaming")
    val _initialized: Property<Int>

    fun initDefaults() {
        _initialized.convention(0)
    }
}

internal fun SortingStrategyConfiguration.toStrategy(): SortingStrategy =
    when {
        executionTime.percentile.isPresent -> executionTime.toStrategy()
        successRate.limit.isPresent -> successRate.toStrategy()
        randomOrder._initialized.isPresent -> RandomOrderSortingStrategy()
        else -> NoSortingStrategy()
    }

private fun ExecutionTimeSortingStrategyConfiguration.toStrategy(): ExecutionTimeSortingStrategy =
    ExecutionTimeSortingStrategy(
        percentile = percentile.get(),
        timeLimit = Instant.now().minus(timeLimit.get())
    )

private fun SuccessRateSortingStrategyConfiguration.toStrategy(): SuccessRateSortingStrategy =
    SuccessRateSortingStrategy(
        timeLimit = Instant.now().minus(limit.get()),
        ascending = ascending.get()
    )

private const val DEFAULT_PERCENTILE = 90.0
private const val DEFAULT_DAYS_COUNT = 30L

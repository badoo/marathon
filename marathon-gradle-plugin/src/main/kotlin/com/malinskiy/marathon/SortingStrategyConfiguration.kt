package com.malinskiy.marathon

import com.malinskiy.marathon.execution.strategy.SortingStrategy
import com.malinskiy.marathon.execution.strategy.impl.sorting.ExecutionTimeSortingStrategy
import com.malinskiy.marathon.execution.strategy.impl.sorting.NoSortingStrategy
import com.malinskiy.marathon.execution.strategy.impl.sorting.RandomOrderSortingStrategy
import com.malinskiy.marathon.execution.strategy.impl.sorting.SuccessRateSortingStrategy
import org.gradle.api.Action
import java.time.Instant
import java.time.temporal.ChronoUnit

class SortingStrategyConfiguration {
    var executionTime: ExecutionTimeSortingStrategyConfiguration? = null
    var successRate: SuccessRateSortingStrategyConfiguration? = null
    var randomOrder: RandomOrderStrategyConfiguration? = null

    fun executionTime(action: Action<ExecutionTimeSortingStrategyConfiguration>) {
        executionTime = (executionTime ?: ExecutionTimeSortingStrategyConfiguration()).also { action.execute(it) }
    }

    fun randomOrder(action: Action<RandomOrderStrategyConfiguration>) {
        randomOrder = (randomOrder ?: RandomOrderStrategyConfiguration).also { action.execute(it) }
    }

    fun successRate(action: Action<SuccessRateSortingStrategyConfiguration>) {
        successRate = (successRate ?: SuccessRateSortingStrategyConfiguration()).also { action.execute(it) }
    }
}

private const val DEFAULT_PERCENTILE = 90.0
const val DEFAULT_DAYS_COUNT = 30L

object RandomOrderStrategyConfiguration

class ExecutionTimeSortingStrategyConfiguration {
    var percentile: Double = DEFAULT_PERCENTILE
    var timeLimit: Instant = Instant.now().minus(DEFAULT_DAYS_COUNT, ChronoUnit.DAYS)
}

class SuccessRateSortingStrategyConfiguration {
    var limit: Instant = Instant.now().minus(DEFAULT_DAYS_COUNT, ChronoUnit.DAYS)
    var ascending: Boolean = false
}

fun SortingStrategyConfiguration.toStrategy(): SortingStrategy = executionTime?.let {
    ExecutionTimeSortingStrategy(it.percentile, it.timeLimit)
} ?: successRate?.let {
    SuccessRateSortingStrategy(it.limit, it.ascending)
} ?: randomOrder?.let {
    RandomOrderSortingStrategy()
} ?: NoSortingStrategy()

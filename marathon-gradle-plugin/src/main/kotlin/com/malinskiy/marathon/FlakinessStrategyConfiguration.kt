package com.malinskiy.marathon

import com.malinskiy.marathon.execution.strategy.FlakinessStrategy
import com.malinskiy.marathon.execution.strategy.impl.flakiness.IgnoreFlakinessStrategy
import com.malinskiy.marathon.execution.strategy.impl.flakiness.ProbabilityBasedFlakinessStrategy
import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import java.time.Duration
import java.time.Instant

interface FlakinessStrategyConfiguration {
    @get:Nested
    val probabilityBased: ProbabilityBasedFlakinessStrategyConfiguration

    fun probabilityBased(action: Action<ProbabilityBasedFlakinessStrategyConfiguration>) {
        probabilityBased.initDefaults()
        action.execute(probabilityBased)
    }
}

interface ProbabilityBasedFlakinessStrategyConfiguration {
    val minSuccessRate: Property<Double>
    val maxCount: Property<Int>
    val timeLimit: Property<Duration>
}

internal fun ProbabilityBasedFlakinessStrategyConfiguration.initDefaults() {
    minSuccessRate.convention(DEFAULT_MIN_SUCCESS_RATE)
    maxCount.convention(DEFAULT_MAX_FLAKY_TESTS_COUNT)
    timeLimit.convention(Duration.ofDays(DEFAULT_DAYS_COUNT))
}

internal fun FlakinessStrategyConfiguration.toStrategy(): FlakinessStrategy =
    if (probabilityBased.minSuccessRate.isPresent) probabilityBased.toStrategy() else IgnoreFlakinessStrategy()

private fun ProbabilityBasedFlakinessStrategyConfiguration.toStrategy(): ProbabilityBasedFlakinessStrategy =
    ProbabilityBasedFlakinessStrategy(
        minSuccessRate = minSuccessRate.get(),
        maxCount = maxCount.get(),
        timeLimit = Instant.now().minus(timeLimit.get())
    )

private const val DEFAULT_MIN_SUCCESS_RATE = 0.8
private const val DEFAULT_MAX_FLAKY_TESTS_COUNT = 3
private const val DEFAULT_DAYS_COUNT = 30L

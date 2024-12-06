package com.malinskiy.marathon

import com.malinskiy.marathon.execution.strategy.RetryStrategy
import com.malinskiy.marathon.execution.strategy.impl.retry.NoRetryStrategy
import com.malinskiy.marathon.execution.strategy.impl.retry.fixedquota.FixedQuotaRetryStrategy
import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

interface RetryStrategyConfiguration {
    @get:Nested
    val fixedQuota: FixedQuotaRetryStrategyConfiguration

    fun fixedQuota(action: Action<FixedQuotaRetryStrategyConfiguration>) {
        fixedQuota.initDefaults()
        action.execute(fixedQuota)
    }
}

interface FixedQuotaRetryStrategyConfiguration {
    val totalAllowedRetryQuota: Property<Int>
    val retryPerTestQuota: Property<Int>
}

internal fun FixedQuotaRetryStrategyConfiguration.initDefaults() {
    totalAllowedRetryQuota.convention(200)
    retryPerTestQuota.convention(3)
}

internal fun RetryStrategyConfiguration.toStrategy(): RetryStrategy =
    if (fixedQuota.totalAllowedRetryQuota.isPresent) fixedQuota.toStrategy() else NoRetryStrategy()

private fun FixedQuotaRetryStrategyConfiguration.toStrategy(): FixedQuotaRetryStrategy =
    FixedQuotaRetryStrategy(
        totalAllowedRetryQuota = totalAllowedRetryQuota.get(),
        retryPerTestQuota = retryPerTestQuota.get()
    )

package com.malinskiy.marathon

import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested

interface RetryStrategyConfiguration {
    @get:Nested
    val fixedQuota: FixedQuotaRetryStrategyConfiguration

    fun fixedQuota(action: Action<FixedQuotaRetryStrategyConfiguration>) {
        fixedQuota.totalAllowedRetryQuota.convention(200)
        fixedQuota.retryPerTestQuota.convention(3)
        action.execute(fixedQuota)
    }
}

interface FixedQuotaRetryStrategyConfiguration {
    val totalAllowedRetryQuota: Property<Int>
    val retryPerTestQuota: Property<Int>
}

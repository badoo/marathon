package com.malinskiy.marathon

import org.gradle.api.Action
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import java.time.Duration

interface FlakinessStrategyConfiguration {
    @get:Nested
    val probabilityBased: ProbabilityBasedFlakinessStrategyConfiguration

    fun probabilityBased(action: Action<ProbabilityBasedFlakinessStrategyConfiguration>) {
        probabilityBased.minSuccessRate.convention(0.8)
        probabilityBased.maxCount.convention(3)
        probabilityBased.timeLimit.convention(Duration.ofDays(30L))
        action.execute(probabilityBased)
    }
}

interface ProbabilityBasedFlakinessStrategyConfiguration {
    val minSuccessRate: Property<Double>
    val maxCount: Property<Int>
    val timeLimit: Property<Duration>
}

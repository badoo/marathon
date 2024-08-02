package com.malinskiy.marathon

import com.malinskiy.marathon.execution.strategy.PoolingStrategy
import com.malinskiy.marathon.execution.strategy.impl.pooling.OmniPoolingStrategy
import com.malinskiy.marathon.execution.strategy.impl.pooling.parameterized.AbiPoolingStrategy
import com.malinskiy.marathon.execution.strategy.impl.pooling.parameterized.ComboPoolingStrategy
import com.malinskiy.marathon.execution.strategy.impl.pooling.parameterized.ManufacturerPoolingStrategy
import com.malinskiy.marathon.execution.strategy.impl.pooling.parameterized.ModelPoolingStrategy
import com.malinskiy.marathon.execution.strategy.impl.pooling.parameterized.OperatingSystemVersionPoolingStrategy
import org.gradle.api.provider.Property

interface PoolingStrategyConfiguration {
    val operatingSystem: Property<Boolean>
    val abi: Property<Boolean>
    val manufacturer: Property<Boolean>
    val model: Property<Boolean>

    fun initDefaults() {
        operatingSystem.convention(false)
        abi.convention(false)
        manufacturer.convention(false)
        model.convention(false)
    }
}

internal fun PoolingStrategyConfiguration.toStrategy(): PoolingStrategy {
    val strategies = mutableListOf<PoolingStrategy>()
    when {
        operatingSystem.get() -> strategies.add(OperatingSystemVersionPoolingStrategy())
        abi.get() -> strategies.add(AbiPoolingStrategy())
        manufacturer.get() -> strategies.add(ManufacturerPoolingStrategy())
        model.get() -> strategies.add(ModelPoolingStrategy())
    }
    return if (strategies.isNotEmpty()) {
        ComboPoolingStrategy(strategies)
    } else {
        OmniPoolingStrategy()
    }
}

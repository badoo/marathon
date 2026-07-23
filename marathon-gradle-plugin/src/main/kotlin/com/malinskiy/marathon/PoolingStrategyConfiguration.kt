package com.malinskiy.marathon

import org.gradle.api.provider.Property

interface PoolingStrategyConfiguration {
    val operatingSystem: Property<Boolean>
    val abi: Property<Boolean>
    val manufacturer: Property<Boolean>
    val model: Property<Boolean>
}

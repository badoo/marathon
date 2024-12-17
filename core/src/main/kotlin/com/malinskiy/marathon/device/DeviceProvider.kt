package com.malinskiy.marathon.device

import kotlinx.coroutines.flow.Flow

interface DeviceProvider : AutoCloseable {
    val deviceInitializationTimeoutMillis: Long
    suspend fun initialize()
    suspend fun terminate()

    val deviceEvents: Flow<DeviceEvent>
}

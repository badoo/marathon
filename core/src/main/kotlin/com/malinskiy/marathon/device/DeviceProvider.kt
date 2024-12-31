package com.malinskiy.marathon.device

import kotlinx.coroutines.flow.Flow

interface DeviceProvider : AutoCloseable {
    val deviceEvents: Flow<DeviceEvent>

    suspend fun initialize()
    suspend fun terminate()
}

package com.malinskiy.marathon.analytics.internal.sub

interface TestEventInflator {
    suspend fun inflate(event: TestEvent): TestEvent
}

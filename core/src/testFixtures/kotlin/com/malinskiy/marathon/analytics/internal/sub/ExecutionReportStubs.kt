package com.malinskiy.marathon.analytics.internal.sub

fun stubExecutionReport(
    deviceConnectedEvents: List<DeviceConnectedEvent> = emptyList(),
    devicePreparingEvents: List<DevicePreparingEvent> = emptyList(),
    deviceProviderPreparingEvent: List<DeviceProviderPreparingEvent> = emptyList(),
    installCheckEvent: List<InstallationCheckEvent> = emptyList(),
    installEvent: List<InstallationEvent> = emptyList(),
    executeBatchEvent: List<ExecutingBatchEvent> = emptyList(),
    cacheStoreEvent: List<CacheStoreEvent> = emptyList(),
    cacheLoadEvent: List<CacheLoadEvent> = emptyList(),
    testEvents: List<TestEvent> = emptyList(),
): ExecutionReport = ExecutionReport(
    deviceConnectedEvents = deviceConnectedEvents,
    devicePreparingEvents = devicePreparingEvents,
    deviceProviderPreparingEvent = deviceProviderPreparingEvent,
    installCheckEvent = installCheckEvent,
    installEvent = installEvent,
    executeBatchEvent = executeBatchEvent,
    cacheStoreEvent = cacheStoreEvent,
    cacheLoadEvent = cacheLoadEvent,
    testEvents = testEvents,
)

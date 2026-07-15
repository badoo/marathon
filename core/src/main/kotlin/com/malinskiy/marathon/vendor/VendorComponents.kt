package com.malinskiy.marathon.vendor

import com.malinskiy.marathon.cache.test.key.ComponentCacheKeyProvider
import com.malinskiy.marathon.device.DeviceProvider
import com.malinskiy.marathon.execution.TestParser
import com.malinskiy.marathon.report.logs.LogsProvider

class VendorComponents(
    val deviceProvider: DeviceProvider,
    val testParser: TestParser,
    val logsProvider: LogsProvider,
    val componentCacheKeyProvider: ComponentCacheKeyProvider
)

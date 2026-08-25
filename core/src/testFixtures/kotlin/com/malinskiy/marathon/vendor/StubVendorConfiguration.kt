package com.malinskiy.marathon.vendor

import com.malinskiy.marathon.cache.test.key.StubComponentCacheKeyProvider
import com.malinskiy.marathon.device.DeviceFeature
import com.malinskiy.marathon.device.StubDeviceProvider
import com.malinskiy.marathon.execution.StubTestParser
import com.malinskiy.marathon.report.logs.StubLogsProvider

class StubVendorConfiguration : VendorConfiguration {
    val deviceProvider = StubDeviceProvider()
    val testParser = StubTestParser()

    override fun preferableRecorderType(): DeviceFeature? = null

    override fun createComponents(dependencies: VendorDependencies): VendorComponents = VendorComponents(
        deviceProvider = deviceProvider,
        testParser = testParser,
        logsProvider = StubLogsProvider(),
        componentCacheKeyProvider = StubComponentCacheKeyProvider(),
    )
}

package com.malinskiy.marathon.test

import com.malinskiy.marathon.device.DeviceFeature
import com.malinskiy.marathon.vendor.VendorComponents
import com.malinskiy.marathon.vendor.VendorConfiguration
import com.malinskiy.marathon.vendor.VendorDependencies

class TestVendorConfiguration : VendorConfiguration {
    val deviceProvider = StubDeviceProvider()
    val testParser = StubTestParser()

    override fun preferableRecorderType(): DeviceFeature? = null

    override fun createComponents(dependencies: VendorDependencies): VendorComponents =
        VendorComponents(
            deviceProvider = deviceProvider,
            testParser = testParser,
            logsProvider = StubLogsProvider(),
            componentCacheKeyProvider = StubComponentCacheKeyProvider()
        )
}

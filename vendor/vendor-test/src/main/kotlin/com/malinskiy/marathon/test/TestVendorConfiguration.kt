package com.malinskiy.marathon.test

import com.malinskiy.marathon.cache.test.key.ComponentCacheKeyProvider
import com.malinskiy.marathon.device.DeviceFeature
import com.malinskiy.marathon.device.DeviceProvider
import com.malinskiy.marathon.execution.ComponentInfoExtractor
import com.malinskiy.marathon.execution.TestParser
import com.malinskiy.marathon.log.MarathonLogConfigurator
import com.malinskiy.marathon.report.logs.LogsProvider
import com.malinskiy.marathon.vendor.VendorConfiguration
import org.koin.core.module.Module
import org.koin.dsl.module

class TestVendorConfiguration : VendorConfiguration {
    val deviceProvider = StubDeviceProvider()
    val testParser = Mocks.TestParser.DEFAULT

    private val testModule = module {
        single<ComponentCacheKeyProvider?> { StubComponentCacheKeyProvider() }
        single<ComponentInfoExtractor?> { StubComponentInfoExtractor() }
        single<DeviceProvider?> { deviceProvider }
        single<LogsProvider?> { StubLogsProvider() }
        single<TestParser?> { testParser }
        single<MarathonLogConfigurator?> { StubMarathonLogConfigurator() }
    }

    override fun preferableRecorderType(): DeviceFeature? = null

    override fun modules(): List<Module> = listOf(testModule)
}

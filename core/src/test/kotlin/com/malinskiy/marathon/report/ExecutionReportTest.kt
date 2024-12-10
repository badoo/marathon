package com.malinskiy.marathon.report

import com.malinskiy.marathon.analytics.internal.sub.DeviceConnectedEvent
import com.malinskiy.marathon.analytics.internal.sub.ExecutionReport
import com.malinskiy.marathon.analytics.internal.sub.TestEvent
import com.malinskiy.marathon.device.DeviceFeature
import com.malinskiy.marathon.device.DeviceInfo
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.device.NetworkState
import com.malinskiy.marathon.device.OperatingSystem
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.test.TestComponentInfo
import com.malinskiy.marathon.test.factory.configuration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant
import com.malinskiy.marathon.test.Test as MarathonTest

class ExecutionReportTest {
    val configuration = configuration()

    private val reportWithoutRetries: ExecutionReport by lazy {
        val device = DeviceInfo(
            operatingSystem = OperatingSystem("23"),
            serialNumber = "xxyyzz",
            model = "Android SDK built for x86",
            manufacturer = "unknown",
            networkState = NetworkState.CONNECTED,
            deviceFeatures = listOf(DeviceFeature.SCREENSHOT, DeviceFeature.VIDEO),
            healthy = true
        )
        ExecutionReport(
            deviceProviderPreparingEvent = emptyList(),
            devicePreparingEvents = emptyList(),
            deviceConnectedEvents = listOf(
                DeviceConnectedEvent(Instant.now(), DevicePoolId("myPool"), device)
            ),
            testEvents = listOf(
                createTestEvent(device, "test1", TestStatus.INCOMPLETE),
                createTestEvent(device, "test2", TestStatus.PASSED),
                createTestEvent(device, "test3", TestStatus.FAILURE)
            ),
            installCheckEvent = emptyList(),
            installEvent = emptyList(),
            executeBatchEvent = emptyList(),
            cacheStoreEvent = emptyList(),
            cacheLoadEvent = emptyList()
        )
    }

    private val reportWithRetries: ExecutionReport by lazy {
        val device = DeviceInfo(
            operatingSystem = OperatingSystem("23"),
            serialNumber = "xxyyzz",
            model = "Android SDK built for x86",
            manufacturer = "unknown",
            networkState = NetworkState.CONNECTED,
            deviceFeatures = listOf(DeviceFeature.SCREENSHOT, DeviceFeature.VIDEO),
            healthy = true
        )
        ExecutionReport(
            deviceProviderPreparingEvent = emptyList(),
            devicePreparingEvents = emptyList(),
            deviceConnectedEvents = listOf(
                DeviceConnectedEvent(Instant.now(), DevicePoolId("myPool"), device)
            ),
            testEvents = listOf(
                createTestEvent(device, "test2", TestStatus.FAILURE, false),
                createTestEvent(device, "test2", TestStatus.FAILURE, false),
                createTestEvent(device, "test2", TestStatus.PASSED, true),
                createTestEvent(device, "test3", TestStatus.FAILURE, false),
                createTestEvent(device, "test3", TestStatus.FAILURE, false),
                createTestEvent(device, "test3", TestStatus.FAILURE, true)
            ),
            installCheckEvent = emptyList(),
            installEvent = emptyList(),
            executeBatchEvent = emptyList(),
            cacheStoreEvent = emptyList(),
            cacheLoadEvent = emptyList()
        )
    }

    private fun createTestEvent(deviceInfo: DeviceInfo, methodName: String, status: TestStatus, final: Boolean = true): TestEvent {
        return TestEvent(
            instant = Instant.now(),
            poolId = DevicePoolId("myPool"),
            device = deviceInfo,
            testResult = TestResult(
                test = MarathonTest("com", "example", methodName, emptyList(), TestComponentInfo()),
                device = deviceInfo,
                status = status,
                startTime = 0,
                endTime = 100,
                batchId = ""
            ),
            final = final
        )
    }

    @Test
    fun `without retries should not include the INCOMPLETE test`() {
        val summary = reportWithoutRetries.summary
        val tests = summary.pools.flatMap { it.tests }
        assertEquals(0, tests.count { it.status == TestStatus.INCOMPLETE })
    }

    @Test
    fun `without retries should include 1 PASSED test`() {
        val summary = reportWithoutRetries.summary
        val tests = summary.pools.flatMap { it.tests }
        assertEquals(1, tests.count { it.status == TestStatus.PASSED })
    }

    @Test
    fun `without retries should include 1 FAILED test`() {
        val summary = reportWithoutRetries.summary
        val tests = summary.pools.flatMap { it.tests }
        assertEquals(1, tests.count { it.status == TestStatus.FAILURE })
    }

    @Test
    fun `with retries should include only one instance of test2 and it's PASSED`() {
        val summary = reportWithRetries.summary
        val tests = summary.pools.flatMap { it.tests }.filter { it.test.method == "test2" }
        assertEquals(1, tests.size)
        assertEquals(TestStatus.PASSED, tests.first().status)
    }

    @Test
    fun `with retries should include only one instance of test3 and it's FAILED`() {
        val summary = reportWithRetries.summary
        val tests = summary.pools.flatMap { it.tests }.filter { it.test.method == "test3" }
        assertEquals(1, tests.size)
        assertEquals(TestStatus.FAILURE, tests.first().status)
    }
}

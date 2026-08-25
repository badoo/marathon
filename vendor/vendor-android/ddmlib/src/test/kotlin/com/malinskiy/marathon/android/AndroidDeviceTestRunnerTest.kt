package com.malinskiy.marathon.android

import com.android.ddmlib.IDevice
import com.android.ddmlib.IShellOutputReceiver
import com.android.ddmlib.testrunner.ITestRunListener
import com.android.sdklib.AndroidVersion
import com.malinskiy.marathon.android.ddmlib.AndroidDeviceTestRunner
import com.malinskiy.marathon.android.ddmlib.DdmlibDeviceProviderFactory
import com.malinskiy.marathon.android.ddmlib.stubDdmlibAndroidDevice
import com.malinskiy.marathon.android.ddmlib.toTestIdentifier
import com.malinskiy.marathon.test.TestBatch
import com.malinskiy.marathon.test.factory.configuration
import com.malinskiy.marathon.test.stubTest
import com.malinskiy.marathon.test.stubTestBatch
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.io.File
import java.util.concurrent.TimeUnit

class AndroidDeviceTestRunnerTest {
    private val ddmsDevice = mock<IDevice>()

    @Test
    fun `should handle ignored tests before execution`() = runTest {
        whenever(ddmsDevice.serialNumber).doReturn("testSerial")
        whenever(ddmsDevice.version).doReturn(AndroidVersion(26))
        val device = stubDdmlibAndroidDevice(ddmsDevice = ddmsDevice)
        val androidDeviceTestRunner = AndroidDeviceTestRunner(device)
        val apkFile = File(javaClass.classLoader.getResource("android_test_1.apk").file)
        val componentInfo = stubAndroidComponentInfo(name = "", applicationOutput = File(""), testApplicationOutput = apkFile)
        val ignoredTest = stubTest(
            pkg = "ignored",
            clazz = "ignored",
            method = "ignored",
            componentInfo = componentInfo,
            annotations = arrayOf("org.junit.Ignore"),
        )
        val identifier = ignoredTest.toTestIdentifier()
        val validTest = stubTest(pkg = "test", clazz = "test", method = "test", componentInfo = componentInfo)
        val batch = TestBatch(id = "123", tests = listOf(ignoredTest, validTest), componentInfo)
        val listener = mock<ITestRunListener>()
        androidDeviceTestRunner.execute(createConfiguration(), batch, listener)

        verify(listener).testStarted(eq(identifier))
        verify(listener).testIgnored(eq(identifier))
        verify(listener).testEnded(eq(identifier), eq(hashMapOf()))
        verifyNoMoreInteractions(listener)
    }

    @Test
    fun `should pass component instrumentation args to the test runner`() = runTest {
        whenever(ddmsDevice.serialNumber).doReturn("testSerial")
        whenever(ddmsDevice.version).doReturn(AndroidVersion(26))
        val device = stubDdmlibAndroidDevice(ddmsDevice = ddmsDevice)
        val androidDeviceTestRunner = AndroidDeviceTestRunner(device)
        val apkFile = File(javaClass.classLoader.getResource("android_test_1.apk").file)
        val componentInfo = stubAndroidComponentInfo(
            name = "",
            applicationOutput = File(""),
            testApplicationOutput = apkFile,
            instrumentationArgs = mapOf("fromComponent" to "componentValue"),
        )
        val test = stubTest(pkg = "test", clazz = "test", method = "test", componentInfo = componentInfo)
        val batch = TestBatch(id = "123", tests = listOf(test), componentInfo)
        val listener = mock<ITestRunListener>()
        androidDeviceTestRunner.execute(createConfiguration(), batch, listener)

        val commandCaptor = argumentCaptor<String>()
        verify(ddmsDevice).executeShellCommand(commandCaptor.capture(), any<IShellOutputReceiver>(), any<Long>(), any<Long>(), any<TimeUnit>())
        assertThat(commandCaptor.firstValue).contains("-e fromComponent componentValue")
    }

    @Test
    fun `should not leak instrumentation args between components`() = runTest {
        whenever(ddmsDevice.serialNumber).doReturn("testSerial")
        whenever(ddmsDevice.version).doReturn(AndroidVersion(26))
        val device = stubDdmlibAndroidDevice(ddmsDevice = ddmsDevice)
        val androidDeviceTestRunner = AndroidDeviceTestRunner(device)
        val apkFile = File(javaClass.classLoader.getResource("android_test_1.apk").file)
        val firstComponent = stubAndroidComponentInfo(
            name = "first",
            applicationOutput = File(""),
            testApplicationOutput = apkFile,
            instrumentationArgs = mapOf("firstArg" to "firstValue"),
        )
        val secondComponent = stubAndroidComponentInfo(
            name = "second",
            applicationOutput = File(""),
            testApplicationOutput = apkFile,
            instrumentationArgs = mapOf("secondArg" to "secondValue"),
        )
        val firstTest = stubTest(pkg = "test", clazz = "test", method = "test", componentInfo = firstComponent)
        val secondTest = stubTest(pkg = "test", clazz = "test", method = "test", componentInfo = secondComponent)
        val firstBatch = stubTestBatch(firstTest, id = "1")
        val secondBatch = stubTestBatch(secondTest, id = "2")
        val listener = mock<ITestRunListener>()
        val configuration = createConfiguration()
        androidDeviceTestRunner.execute(configuration, firstBatch, listener)
        androidDeviceTestRunner.execute(configuration, secondBatch, listener)

        val commandCaptor = argumentCaptor<String>()
        verify(ddmsDevice, times(2))
            .executeShellCommand(commandCaptor.capture(), any<IShellOutputReceiver>(), any<Long>(), any<Long>(), any<TimeUnit>())
        assertThat(commandCaptor.firstValue).contains("-e firstArg firstValue").doesNotContain("secondArg")
        assertThat(commandCaptor.secondValue).contains("-e secondArg secondValue").doesNotContain("firstArg")
    }

    private fun createConfiguration() = configuration {
        vendorConfiguration = AndroidConfiguration(
            adbPath = File("adb"),
            deviceProviderFactory = DdmlibDeviceProviderFactory(),
        )
    }
}

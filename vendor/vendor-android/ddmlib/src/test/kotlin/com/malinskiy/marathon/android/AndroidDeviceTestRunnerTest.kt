package com.malinskiy.marathon.android

import com.android.ddmlib.IDevice
import com.android.ddmlib.testrunner.ITestRunListener
import com.android.sdklib.AndroidVersion
import com.malinskiy.marathon.android.ddmlib.AndroidDeviceTestRunner
import com.malinskiy.marathon.android.ddmlib.DdmlibDeviceProviderFactory
import com.malinskiy.marathon.android.ddmlib.stubDdmlibAndroidDevice
import com.malinskiy.marathon.android.ddmlib.toTestIdentifier
import com.malinskiy.marathon.test.TestBatch
import com.malinskiy.marathon.test.factory.configuration
import com.malinskiy.marathon.test.stubTest
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.io.File

class AndroidDeviceTestRunnerTest {
    @Test
    fun `should handle ignored tests before execution`() {
        val ddmsDevice = mock<IDevice>()
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
            annotations = arrayOf("org.junit.Ignore")
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

    private fun createConfiguration() = configuration {
        vendorConfiguration = AndroidConfiguration(
            adbPath = File("adb"),
            deviceProviderFactory = DdmlibDeviceProviderFactory()
        )
    }
}

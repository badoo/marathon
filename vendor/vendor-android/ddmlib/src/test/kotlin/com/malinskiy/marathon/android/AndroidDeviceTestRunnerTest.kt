package com.malinskiy.marathon.android

import com.android.ddmlib.IDevice
import com.android.ddmlib.testrunner.ITestRunListener
import com.android.sdklib.AndroidVersion
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.android.ddmlib.AndroidDeviceTestRunner
import com.malinskiy.marathon.android.ddmlib.DdmlibAndroidDevice
import com.malinskiy.marathon.android.ddmlib.toTestIdentifier
import com.malinskiy.marathon.android.serial.SerialStrategy
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.test.MetaProperty
import com.malinskiy.marathon.test.TestBatch
import com.malinskiy.marathon.time.SystemTimer
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.io.File
import java.time.Clock
import com.malinskiy.marathon.test.Test as MarathonTest

class AndroidDeviceTestRunnerTest {
    @Test
    fun `should handle ignored tests before execution`() {
        val ddmsDevice = mock<IDevice>()
        whenever(ddmsDevice.serialNumber).doReturn("testSerial")
        whenever(ddmsDevice.version).doReturn(AndroidVersion(26))
        val device = DdmlibAndroidDevice(
            ddmsDevice = ddmsDevice,
            adbPath = File("adb"),
            track = Track(),
            timer = SystemTimer(Clock.systemDefaultZone()),
            androidAppInstaller = mock<AndroidAppInstaller>(),
            attachmentManager = mock(),
            reportsFileManager = mock(),
            serialStrategy = SerialStrategy.AUTOMATIC,
            logcatListener = mock(),
            strictRunChecker = mock()
        )
        val androidDeviceTestRunner = AndroidDeviceTestRunner(device)
        val apkFile = File(javaClass.classLoader.getResource("android_test_1.apk").file)
        val componentInfo = AndroidComponentInfo(
            name = "",
            applicationId = null,
            testApplicationId = "com.example.test",
            applicationOutput = File(""),
            testApplicationOutput = apkFile,
        )
        val ignoredTest = MarathonTest(
            pkg = "ignored",
            clazz = "ignored",
            method = "ignored",
            metaProperties = listOf(MetaProperty("org.junit.Ignore")),
            componentInfo = componentInfo
        )
        val identifier = ignoredTest.toTestIdentifier()
        val validTest = MarathonTest(pkg = "test", clazz = "test", method = "test", metaProperties = emptyList(), componentInfo)
        val batch = TestBatch(id = "123", tests = listOf(ignoredTest, validTest), componentInfo)
        val listener = mock<ITestRunListener>()
        androidDeviceTestRunner.execute(createConfiguration(), batch, listener)

        verify(listener).testStarted(eq(identifier))
        verify(listener).testIgnored(eq(identifier))
        verify(listener).testEnded(eq(identifier), eq(hashMapOf()))
        verifyNoMoreInteractions(listener)
    }

    private fun createConfiguration() = Configuration(
        outputDir = File(""),
        cache = null,
        poolingStrategy = null,
        shardingStrategy = null,
        sortingStrategy = null,
        batchingStrategy = null,
        flakinessStrategy = null,
        retryStrategy = null,
        filteringConfiguration = null,
        strictRunConfiguration = null,
        ignoreFailures = null,
        strictMode = null,
        uncompletedTestRetryQuota = null,
        testClassRegexes = null,
        includeSerialRegexes = null,
        excludeSerialRegexes = null,
        ignoreFailureRegexes = null,
        failFastFailureRegexes = null,
        appModuleRegexes = null,
        testOutputTimeoutMillis = null,
        noDevicesTimeoutMillis = null,
        analyticsTracker = null,
        listener = null,
        testOwnerProvider = null,
        vendorConfiguration = AndroidConfiguration(
            adbPath = File("adb"),
            implementationModules = emptyList()
        )
    )
}

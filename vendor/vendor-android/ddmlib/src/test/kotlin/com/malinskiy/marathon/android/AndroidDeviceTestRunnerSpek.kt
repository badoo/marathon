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
import com.malinskiy.marathon.spek.initKoin
import com.malinskiy.marathon.test.MetaProperty
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestBatch
import com.malinskiy.marathon.time.SystemTimer
import org.amshove.kluent.mock
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import java.io.File
import java.time.Clock

class AndroidDeviceTestRunnerSpek : Spek(
    {
        initKoin()

        describe("AndroidDeviceTestRunner") {
            it("should handle ignored tests before execution") {
                val ddmsDevice = mock<IDevice>()
                whenever(ddmsDevice.serialNumber).doReturn("testSerial")
                whenever(ddmsDevice.version).doReturn(AndroidVersion(26))
                val appInstaller = mock<AndroidAppInstaller>()
                val device = DdmlibAndroidDevice(
                    ddmsDevice,
                    "",
                    Track(),
                    SystemTimer(Clock.systemDefaultZone()),
                    appInstaller,
                    mock(),
                    mock(),
                    SerialStrategy.AUTOMATIC,
                    mock(),
                    mock()
                )
                val androidDeviceTestRunner = AndroidDeviceTestRunner(device)
                val apkFile = File(javaClass.classLoader.getResource("android_test_1.apk").file)
                val output = File("")
                val configuration = Configuration(
                    name = "",
                    outputDir = output,
                    customAnalyticsTracker = null,
                    poolingStrategy = null,
                    shardingStrategy = null,
                    sortingStrategy = null,
                    batchingStrategy = null,
                    flakinessStrategy = null,
                    retryStrategy = null,
                    filteringConfiguration = null,
                    strictRunFilterConfiguration = null,
                    cache = null,
                    ignoreFailures = null,
                    isCodeCoverageEnabled = null,
                    fallbackToScreenshots = null,
                    strictMode = null,
                    listener = null,
                    uncompletedTestRetryQuota = null,
                    testClassRegexes = null,
                    includeSerialRegexes = null,
                    excludeSerialRegexes = null,
                    ignoreFailureRegexes = null,
                    failFastFailureRegexes = null,
                    testBatchTimeoutMillis = null,
                    testOutputTimeoutMillis = null,
                    noDevicesTimeoutMillis = null,
                    debug = null,
                    vendorConfiguration = AndroidConfiguration(
                        File(""),
                        applicationOutput = File(""),
                        testApplicationOutput = apkFile,
                        implementationModules = emptyList()
                    )
                )
                val componentInfo = AndroidComponentInfoExtractor().extract(configuration)
                val ignoredTest =
                    Test("ignored", "ignored", "ignored", listOf(MetaProperty("org.junit.Ignore")), componentInfo)
                val identifier = ignoredTest.toTestIdentifier()
                val validTest = Test("test", "test", "test", emptyList(), componentInfo)
                val batch = TestBatch("123", listOf(ignoredTest, validTest), componentInfo)
                val listener = mock<ITestRunListener>()
                androidDeviceTestRunner.execute(configuration, batch, listener)
                verify(listener).testStarted(eq(identifier))
                verify(listener).testIgnored(eq(identifier))
                verify(listener).testEnded(eq(identifier), eq(hashMapOf()))
                verifyNoMoreInteractions(listener)

            }
        }
    })

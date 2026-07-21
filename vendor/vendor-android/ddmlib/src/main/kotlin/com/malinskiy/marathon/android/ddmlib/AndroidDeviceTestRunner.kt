package com.malinskiy.marathon.android.ddmlib

import com.android.ddmlib.AdbCommandRejectedException
import com.android.ddmlib.ShellCommandUnresponsiveException
import com.android.ddmlib.TimeoutException
import com.android.ddmlib.testrunner.ITestRunListener
import com.android.ddmlib.testrunner.RemoteAndroidTestRunner
import com.android.ddmlib.testrunner.TestIdentifier
import com.malinskiy.marathon.android.AndroidComponentInfo
import com.malinskiy.marathon.android.AndroidConfiguration
import com.malinskiy.marathon.android.ApkParser
import com.malinskiy.marathon.android.InstrumentationInfo
import com.malinskiy.marathon.exceptions.DeviceLostException
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestBatch
import com.malinskiy.marathon.test.toTestName
import java.io.IOException
import java.util.concurrent.TimeUnit

const val JUNIT_IGNORE_META_PROPERY = "org.junit.Ignore"

class AndroidDeviceTestRunner(private val device: DdmlibAndroidDevice) {

    private val logger = MarathonLogging.getLogger(AndroidDeviceTestRunner::class.java)

    fun execute(
        configuration: Configuration,
        rawTestBatch: TestBatch,
        listener: ITestRunListener
    ) {
        val androidComponentInfo = rawTestBatch.componentInfo as AndroidComponentInfo

        val ignoredTests = rawTestBatch.tests.filter { test ->
            test.metaProperties.any { it.name == JUNIT_IGNORE_META_PROPERY }
        }

        val testBatch = TestBatch(
            id = rawTestBatch.id,
            tests = rawTestBatch.tests - ignoredTests,
            componentInfo = rawTestBatch.componentInfo
        )

        val androidConfiguration = configuration.vendorConfiguration as AndroidConfiguration
        val info = ApkParser().parseInstrumentationInfo(androidComponentInfo.testApplicationOutput)
        val runner = prepareTestRunner(configuration, androidComponentInfo, info, testBatch)

        try {
            notifyIgnoredTest(ignoredTests, listener)
            if (testBatch.tests.isNotEmpty()) {
                clearData(androidConfiguration, info)
                runner.run(listener)
            } else {
                listener.testRunEnded(0, emptyMap())
            }
        } catch (e: ShellCommandUnresponsiveException) {
            val errorMessage = "ADB unresponsive while running tests ${testBatch.tests.map { it.toTestName() }}"
            logger.error(errorMessage, e)
            listener.testRunFailed(errorMessage)
        } catch (e: TimeoutException) {
            val errorMessage = "ADB timed out while running tests ${testBatch.tests.map { it.toTestName() }}"
            logger.error(errorMessage, e)
            listener.testRunFailed(errorMessage)
        } catch (e: AdbCommandRejectedException) {
            val errorMessage = "ADB error while running tests ${testBatch.tests.map { it.toTestName() }}"
            logger.error(errorMessage, e)
            listener.testRunFailed(errorMessage)
            if (e.isDeviceOffline) {
                throw DeviceLostException(e)
            }
        } catch (e: IOException) {
            val errorMessage = "ADB error while running tests ${testBatch.tests.map { it.toTestName() }}"
            logger.error(errorMessage, e)
            listener.testRunFailed(errorMessage)
        }
    }

    private fun notifyIgnoredTest(ignoredTests: List<Test>, listeners: ITestRunListener) {
        ignoredTests.forEach {
            val identifier = it.toTestIdentifier()
            listeners.testStarted(identifier)
            listeners.testIgnored(identifier)
            listeners.testEnded(identifier, hashMapOf())
        }
    }

    private fun clearData(androidConfiguration: AndroidConfiguration, info: InstrumentationInfo) {
        if (androidConfiguration.applicationPmClear) {
            device.ddmsDevice.safeClearPackage(info.applicationPackage)?.let {
                logger.debug("[{}] App package {} cleared: {}", device.serialNumber, info.applicationPackage, it)
            }
        }
        if (androidConfiguration.testApplicationPmClear) {
            device.ddmsDevice.safeClearPackage(info.instrumentationPackage)?.let {
                logger.debug("[{}] Instrumentation package {} cleared: {}", device.serialNumber, info.instrumentationPackage, it)
            }
        }
    }

    private fun prepareTestRunner(
        configuration: Configuration,
        androidComponentInfo: AndroidComponentInfo,
        info: InstrumentationInfo,
        testBatch: TestBatch
    ): RemoteAndroidTestRunner {

        val runner = RemoteAndroidTestRunner(info.instrumentationPackage, info.testRunnerClass, device.ddmsDevice)

        val tests = testBatch.tests.map {
            val pkg = when {
                it.pkg.isNotEmpty() -> "${it.pkg}."
                else -> ""
            }
            val clazz = it.clazz
            val method = it.method
            if (it.method != "null") {
                "${pkg}${clazz}#$method"
            } else {
                /**
                 * Special case for tests without any methods
                 */
                "${pkg}${clazz}"
            }.bashEscape()
        }.toTypedArray()

        logger.debug("[{}] tests = {}", device.serialNumber, tests)

        runner.setRunName("TestRunName")
        runner.setMaxTimeToOutputResponse(configuration.testOutputTimeoutMillis * testBatch.tests.size, TimeUnit.MILLISECONDS)
        runner.setClassNames(tests)

        androidComponentInfo.instrumentationArgs.forEach { key, value ->
            runner.addInstrumentationArg(key, value)
        }

        return runner
    }
}

internal fun String.bashEscape(): String = replace(" ", "\\ ")

internal fun Test.toTestIdentifier(): TestIdentifier = TestIdentifier("$pkg.$clazz", method)

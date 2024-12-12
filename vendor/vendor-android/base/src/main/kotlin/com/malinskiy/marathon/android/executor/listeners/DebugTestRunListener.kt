package com.malinskiy.marathon.android.executor.listeners

import com.malinskiy.marathon.android.AndroidDevice
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.toSimpleSafeTestName

class DebugTestRunListener(private val device: AndroidDevice) : TestRunListener {

    private val logger = MarathonLogging.getLogger(DebugTestRunListener::class.java)

    override fun testRunStarted(runName: String, testCount: Int) {
        logger.info("[{}] testRunStarted", device.serialNumber)
    }

    override fun testStarted(test: Test) {
        logger.info("[{}] testStarted {}", device.serialNumber, test.toSimpleSafeTestName())
    }

    override fun testAssumptionFailure(test: Test, trace: String) {
        logger.info("[{}] testAssumptionFailure {} trace = {}", device.serialNumber, test.toSimpleSafeTestName(), trace)
    }

    override fun testRunStopped(elapsedTime: Long) {
        logger.info("[{}] testRunStopped elapsedTime = {}ms", device.serialNumber, elapsedTime)
    }

    override fun testFailed(test: Test, trace: String) {
        logger.info("[{}] testFailed {} trace = {}", device.serialNumber, test.toSimpleSafeTestName(), trace)
    }

    override fun testEnded(test: Test, testMetrics: Map<String, String>) {
        logger.info("[{}] testEnded {}", device.serialNumber, test.toSimpleSafeTestName())
    }

    override fun testIgnored(test: Test) {
        logger.info("[{}] testIgnored {}", device.serialNumber, test.toSimpleSafeTestName())
    }

    override fun testRunFailed(errorMessage: String) {
        logger.info("[{}] testRunFailed errorMessage = {}", device.serialNumber, errorMessage)
    }

    override fun testRunEnded(elapsedTime: Long, runMetrics: Map<String, String>) {
        logger.info("[{}] testRunEnded elapsedTime = {}ms", device.serialNumber, elapsedTime)
    }
}

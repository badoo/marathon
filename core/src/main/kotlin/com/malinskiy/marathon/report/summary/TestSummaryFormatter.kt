package com.malinskiy.marathon.report.summary

import com.google.gson.Gson
import com.malinskiy.marathon.execution.ComponentInfo
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.report.summary.json.BatchInfo
import com.malinskiy.marathon.report.summary.json.TestInfo
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.toSimpleSafeTestName

class TestSummaryFormatter {

    private val gson: Gson by lazy { Gson() }

    fun formatTestResultSummary(currentTestResult: TestResult, testSummary: TestSummary?): String {
        val stringBuilder = StringBuilder()
        stringBuilder.appendLine("Test status: ${currentTestResult.status}")
        stringBuilder.appendLine("Strict run: ${currentTestResult.isStrictRun}")
        stringBuilder.appendLine("From cache: ${currentTestResult.isFromCache}")

        testSummary?.let { summary ->
            stringBuilder.appendLine()
            stringBuilder.appendLine("There were ${summary.results.size} runs:")

            summary.results.forEachIndexed { index, testResult ->
                val bulletSymbol = "${(index + 1)})"

                stringBuilder.append("\u00a0\u00a0$bulletSymbol ${testResult.status}")

                val additionalInfo = listOfNotNull(
                    testResult.device.serialNumber,
                    "batch: " + testResult.batchId.createShortBatchId(),
                    testResult.createShortFailureDescription()
                )
                stringBuilder.append(" (${additionalInfo.joinToString()})")

                val isCurrent = testResult == currentTestResult
                if (isCurrent) {
                    stringBuilder.append(" - current")
                }

                stringBuilder.appendLine()
            }

            stringBuilder.appendLine()

            stringBuilder.appendLine("Test runs details:")

            summary.batches.forEachIndexed { index, batch ->
                val batchSummary = batch.toSummaryString(index, currentTestResult.test)
                stringBuilder.append(batchSummary)
                stringBuilder.appendLine()
            }
        }

        return stringBuilder.toString()
    }

    private fun Batch.toSummaryString(index: Int, currentTest: Test): String {
        val stringBuilder = StringBuilder()
        val deviceSerial = testResults.first().device.serialNumber

        val testStatus = testResults
            .firstOrNull { it.test == currentTest }
            ?.status
            ?.toString() ?: "UNKNOWN"

        val batchBullet = "${index + 1})"

        stringBuilder.appendLine("=".repeat(80))
        stringBuilder.appendLine("\u00a0\u00a0$batchBullet $testStatus in batch #${batchId.createShortBatchId()} (${testResults.size} tests in batch, device: $deviceSerial)")
        stringBuilder.appendLine("=".repeat(80))
        stringBuilder.appendLine("Tests in the batch (executed in the same process):")

        testResults.forEach { testResult ->
            val isCurrentTest = testResult.test == currentTest
            val testBulletSymbol = if (isCurrentTest) ">" else "*"

            stringBuilder.append("\u00a0\u00a0$testBulletSymbol ${testResult.test.toSimpleSafeTestName()}")

            val additionalInfo = listOfNotNull(
                testResult.status.toString(),
                testResult.createShortFailureDescription()
            )
            stringBuilder.append(" (${additionalInfo.joinToString()})")
            stringBuilder.appendLine()
        }

        stringBuilder.appendLine()
        stringBuilder.appendLine("Copy JSON to run this batch locally:")
        stringBuilder.appendLine(toJsonString())
        stringBuilder.appendLine()

        return stringBuilder.toString()
    }

    private fun Batch.toJsonString(): String {
        val component = this.testResults.first().test.componentInfo.gradleModulePath
        val jsonTests = testResults.map { TestInfo(it.test.pkg, it.test.clazz, it.test.method) }
        val jsonBatch = BatchInfo(this.batchId, component, jsonTests)
        return gson.toJson(jsonBatch)
    }

    private val ComponentInfo.gradleModulePath: String
        get() = name.substringBeforeLast(":")

    private fun TestResult.createShortFailureDescription(): String? =
        stacktrace
            ?.lineSequence()
            ?.take(SHORT_FAILURE_MAX_LINES)
            ?.joinToString(separator = " ")
            ?.take(SHORT_FAILURE_DESCRIPTION_LIMIT)
            ?.let { "$it..." }

    private fun String.createShortBatchId(): String =
        take(SHORT_BATCH_ID_SIZE)

    private companion object {
        private const val SHORT_FAILURE_DESCRIPTION_LIMIT = 80
        private const val SHORT_FAILURE_MAX_LINES = 3
        private const val SHORT_BATCH_ID_SIZE = 8
    }
}

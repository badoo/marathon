package com.malinskiy.marathon.android.executor.listeners

import com.malinskiy.marathon.android.model.AndroidTestResult
import com.malinskiy.marathon.android.model.AndroidTestStatus
import com.malinskiy.marathon.android.model.TestRunResultsAccumulator
import com.malinskiy.marathon.device.Device
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.device.toDeviceInfo
import com.malinskiy.marathon.execution.Attachment
import com.malinskiy.marathon.execution.StrictRunChecker
import com.malinskiy.marathon.execution.TestBatchResults
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.execution.progress.ProgressReporter
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.report.attachment.AttachmentListener
import com.malinskiy.marathon.report.attachment.AttachmentProvider
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestBatch
import com.malinskiy.marathon.test.toTestName
import com.malinskiy.marathon.time.Timer
import kotlinx.coroutines.CompletableDeferred

class TestRunResultsListener(
    private val testBatch: TestBatch,
    private val device: Device,
    private val deferred: CompletableDeferred<TestBatchResults>,
    private val timer: Timer,
    private val progressReporter: ProgressReporter,
    private val poolId: DevicePoolId,
    private val strictRunChecker: StrictRunChecker,
    attachmentProviders: List<AttachmentProvider>
) : AbstractTestRunResultListener(), AttachmentListener {

    private val logger = MarathonLogging.getLogger(TestRunResultsListener::class.java)
    private val attachments: MutableMap<Test, MutableList<Attachment>> = mutableMapOf()
    private val creationTime = timer.currentTimeMillis()

    init {
        attachmentProviders.forEach {
            it.registerListener(this)
        }
    }

    override fun onAttachment(test: Test, attachment: Attachment) {
        val testAttachments = attachments.getOrPut(test) { mutableListOf() }
        testAttachments.add(attachment)
    }

    override fun handleTestRunResults(runResult: TestRunResultsAccumulator) {
        val results = mergeParameterisedResults(runResult.testResults)
        val tests = testBatch.tests

        val testResults = results.map {
            it.toTestResult(device)
        }

        val nonNullTestResults = testResults.filter {
            /**
             * If we have a result with null method, then we ignore it unless explicitly requested to
             *
             * An example of null method response is @BeforeClass failure
             * An example of null method request is an ignored test class with remote parser
             */
            it.test.method != "null" || testBatch.tests.contains(it.test)
        }

        val finished = nonNullTestResults.filter {
            results[it.test]?.isSuccessful() ?: false
        }

        val (reportedIncompleteTests, reportedNonNullTests) = nonNullTestResults.partition { it.status == TestStatus.INCOMPLETE }

        val failed = reportedNonNullTests.filterNot {
            val status = results[it.test]
            when {
                status?.isSuccessful() == true -> true
                else -> false
            }
        }

        val uncompleted = reportedIncompleteTests + tests
            .filterNot { expectedTest ->
                results.containsKey(expectedTest)
            }
            .createUncompletedTestResults(runResult, device)

        if (uncompleted.isNotEmpty()) {
            logger.warn("[{}] Uncompleted tests {}", device.serialNumber, uncompleted.joinToString(", ") { it.test.toTestName() })
        }

        deferred.complete(TestBatchResults(testBatch.id, device, testBatch.componentInfo, finished, failed, uncompleted))
    }

    private fun Collection<Test>.createUncompletedTestResults(
        testRunResult: TestRunResultsAccumulator,
        device: Device
    ): Collection<TestResult> {

        val lastCompletedTestEndTime = testRunResult
            .testResults
            .values
            .maxByOrNull { it.endTime }
            ?.endTime
            ?: creationTime

        return map {
            TestResult(
                it,
                device.toDeviceInfo(),
                TestStatus.INCOMPLETE,
                lastCompletedTestEndTime,
                timer.currentTimeMillis(),
                batchId = testBatch.id,
                isStrictRun = strictRunChecker.isStrictRun(it),
                isFromCache = false,
                stacktrace = testRunResult.runFailureMessage
            )
        }
    }

    private fun mergeParameterisedResults(results: MutableMap<Test, AndroidTestResult>): Map<Test, AndroidTestResult> {

        /**
         * If we explicitly requested parameterized tests - skip merging
         */
        if (testBatch.tests.any { it.method.contains('[') && it.method.contains(']') }) return results

        val result = mutableMapOf<Test, AndroidTestResult>()
        for (e in results) {
            val test = e.key
            if (test.method.matches(""".+\[\d+]""".toRegex())) {
                val realIdentifier = test.copy(clazz = e.key.clazz, method = e.key.method.split("[")[0])
                val maybeExistingParameterizedResult = result[realIdentifier]
                if (maybeExistingParameterizedResult == null) {
                    result[realIdentifier] = e.value
                } else {
                    result[realIdentifier]?.status = maybeExistingParameterizedResult.status + e.value.status
                    //Needed for proper result aggregation
                    progressReporter.addTestDiscoveredDuringRuntime(poolId, test)
                }
            } else {
                result[test] = e.value
            }
        }

        return result.toMap()
    }

    private fun Map.Entry<Test, AndroidTestResult>.toTestResult(device: Device): TestResult {
        val testInstanceFromBatch = testBatch.tests.find { it == key }
        val test = key
        val attachments = attachments[test].orEmpty()
        val resultTest = testInstanceFromBatch ?: test

        return TestResult(
            test = resultTest,
            device = device.toDeviceInfo(),
            status = value.status.toMarathonStatus(),
            startTime = value.startTime,
            endTime = value.endTime,
            batchId = testBatch.id,
            isStrictRun = strictRunChecker.isStrictRun(resultTest),
            stacktrace = value.stackTrace,
            attachments = attachments
        )
    }

    private fun AndroidTestResult.isSuccessful(): Boolean =
        when (status) {
            AndroidTestStatus.PASSED, AndroidTestStatus.IGNORED, AndroidTestStatus.ASSUMPTION_FAILURE -> true
            else -> false
        }
}

private operator fun AndroidTestStatus.plus(value: AndroidTestStatus): AndroidTestStatus {
    return when (this) {
        AndroidTestStatus.FAILURE -> AndroidTestStatus.FAILURE
        AndroidTestStatus.PASSED -> value
        AndroidTestStatus.IGNORED -> AndroidTestStatus.IGNORED
        AndroidTestStatus.INCOMPLETE -> AndroidTestStatus.INCOMPLETE
        AndroidTestStatus.ASSUMPTION_FAILURE -> AndroidTestStatus.ASSUMPTION_FAILURE
    }
}

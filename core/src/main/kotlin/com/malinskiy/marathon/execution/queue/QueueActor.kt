package com.malinskiy.marathon.execution.queue

import com.malinskiy.marathon.actor.Actor
import com.malinskiy.marathon.analytics.external.Analytics
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.device.DeviceInfo
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.DevicePoolMessage.FromQueue
import com.malinskiy.marathon.execution.StrictRunChecker
import com.malinskiy.marathon.execution.TestBatchResults
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.execution.TestShard
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.execution.progress.ProgressReporter
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.report.logs.BatchLogs
import com.malinskiy.marathon.report.logs.Log
import com.malinskiy.marathon.report.logs.LogEvent
import com.malinskiy.marathon.report.logs.LogsProvider
import com.malinskiy.marathon.report.logs.toLogTest
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestBatch
import com.malinskiy.marathon.test.toTestName
import com.malinskiy.marathon.time.Timer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.SendChannel
import java.util.PriorityQueue
import java.util.Queue
import kotlin.coroutines.CoroutineContext

class QueueActor(
    private val configuration: Configuration,
    private val analytics: Analytics,
    private val pool: SendChannel<FromQueue>,
    private val poolId: DevicePoolId,
    private val progressReporter: ProgressReporter,
    private val track: Track,
    private val timer: Timer,
    private val logProvider: LogsProvider,
    private val strictRunChecker: StrictRunChecker,
    poolJob: Job,
    context: CoroutineContext,
) : Actor<QueueMessage>(name = "QueueActor[$poolId]", context, parent = poolJob) {

    private val logger = MarathonLogging.getLogger("QueueActor[$poolId]")

    private val sorting = configuration.sortingStrategy

    private val queue: Queue<Test> = PriorityQueue<Test>(sorting.process(analytics))
    private val batching = configuration.batchingStrategy
    private val retry = configuration.retryStrategy

    private val activeBatches = mutableMapOf<String, TestBatch>()
    private val uncompletedTestsRetryCount = mutableMapOf<Test, Int>()

    private val testResultReporter = TestResultReporter(poolId, configuration, track)
    private var flakyTests: List<Test> = emptyList()

    var stopRequested: Boolean = false
        private set

    override suspend fun receive(msg: QueueMessage) {
        when (msg) {
            is QueueMessage.AddShard -> {
                testResultReporter.addShard(msg.shard)
                val testsToAdd = msg.shard.tests + msg.shard.flakyTests
                queue.addAll(testsToAdd)
                progressReporter.addTests(poolId, testsToAdd.size)
                flakyTests = flakyTests + msg.shard.flakyTests

                if (queue.isNotEmpty()) {
                    pool.send(FromQueue.Notify)
                }
            }
            is QueueMessage.RequestBatch -> {
                onRequestBatch(msg.device)
            }
            is QueueMessage.IsEmpty -> {
                msg.deferred.complete(queue.isEmpty() && activeBatches.isEmpty())
            }
            is QueueMessage.Stop -> {
                stopRequested = true

                if (queue.isEmpty() && activeBatches.isEmpty()) {
                    logger.debug("Stop requested, queue is empty and no active batches present, terminating")
                    terminate()
                }
            }
            is QueueMessage.Terminate -> {
                onTerminate()
            }
            is QueueMessage.Completed -> {
                onBatchCompleted(msg.device, msg.results)
            }
            is QueueMessage.ReturnBatch -> {
                onReturnBatch(msg.device, msg.batch)
            }
        }
    }

    private suspend fun onBatchCompleted(device: DeviceInfo, results: TestBatchResults) {
        logger.debug("[{}] Handling test results", device.serialNumber)

        val updatedResults = updateUncompletedTests(results)
        val finished = updatedResults.finished
        val failed = updatedResults.failed

        if (finished.isNotEmpty()) {
            handleFinishedTests(finished, device)
        }
        if (updatedResults.uncompleted.isNotEmpty()) {
            handleUncompletedTests(updatedResults.uncompleted, device)
        }
        if (failed.isNotEmpty()) {
            handleFailedTests(failed, device)
        }
        activeBatches.remove(device.serialNumber)
        if (queue.isNotEmpty()) {
            pool.send(FromQueue.Notify)
        }
    }

    private suspend fun updateUncompletedTests(results: TestBatchResults): TestBatchResults {
        val batchId = results.batchId
        val device = results.device
        val batchLogs = logProvider.getBatchReport(batchId) ?: null
            .also {
                logger.warn("[{}] No logs for batch {}", device.serialNumber, batchId)
            }

        val (failedFromUncompleted, uncompleted) = results
            .uncompleted
            .partitionFastFailures(batchLogs)

        failedFromUncompleted.forEach {
            logger.warn(
                "[{}] Uncompleted test run marked as failed for {} as error message matches to fast test failures",
                device.serialNumber,
                it.test.toTestName(),
            )
        }

        val (newUncompleted, failed) = results
            .failed
            .partitionIgnoredFailures(batchLogs)

        newUncompleted.forEach {
            logger.debug(
                "[{}] Failed test run marked as uncompleted for {} as error message matches to ignored test failures",
                device.serialNumber,
                it.test.toTestName(),
            )
        }

        return results.copy(
            failed = failed + failedFromUncompleted,
            uncompleted = uncompleted + newUncompleted,
        )
    }

    private fun Iterable<TestResult>.partitionFastFailures(batchLogs: BatchLogs?): Pair<List<TestResult>, List<TestResult>> = partition {
        if (it.hasFailFastFailureStackTrace()) {
            true
        } else {
            val log = batchLogs?.tests?.get(it.test.toLogTest())
            log?.hasFailFastFailureCrashLogEvent() ?: false
        }
    }

    private fun Iterable<TestResult>.partitionIgnoredFailures(batchLogs: BatchLogs?): Pair<List<TestResult>, List<TestResult>> = partition {
        if (it.hasIgnoredFailureStackTrace()) {
            true
        } else {
            val log = batchLogs?.tests?.get(it.test.toLogTest())
            log?.hasIgnoredCrashLogEvent() ?: false
        }
    }

    private fun TestResult.hasIgnoredFailureStackTrace(): Boolean = stacktrace
        ?.let { stacktrace ->
            configuration.ignoreFailureRegexes.any { regexp -> regexp.matches(stacktrace) }
        }
        ?: false

    private fun TestResult.hasFailFastFailureStackTrace(): Boolean = stacktrace
        ?.let { stacktrace ->
            configuration.failFastFailureRegexes.any { regexp -> regexp.matches(stacktrace) }
        }
        ?: false

    private fun Log.hasIgnoredCrashLogEvent(): Boolean = events
        .any { logEvent ->
            logEvent is LogEvent.Crash && configuration.ignoreFailureRegexes.any { regexp -> regexp.matches(logEvent.message) }
        }

    private fun Log.hasFailFastFailureCrashLogEvent(): Boolean = events
        .any { logEvent ->
            logEvent is LogEvent.Crash && configuration.failFastFailureRegexes.any { regexp -> regexp.matches(logEvent.message) }
        }

    private fun handleUncompletedTests(uncompletedTests: Collection<TestResult>, device: DeviceInfo) {
        val (uncompletedFailFastFailed, uncompletedCleaned) = uncompletedTests.partition { it.hasFailFastFailureStackTrace() }

        if (uncompletedFailFastFailed.isNotEmpty()) {
            logger.debug(
                "[{}] Uncompleted test failed because of stacktrace for {}",
                device.serialNumber,
                uncompletedFailFastFailed.joinToString(separator = ", ") { it.test.toTestName() },
            )
            val uncompletedToFailed = uncompletedFailFastFailed.map {
                it.copy(status = TestStatus.FAILURE)
            }
            for (test in uncompletedToFailed) {
                testResultReporter.testIncomplete(device, test, final = true)
            }
        }

        val (uncompletedRetryQuotaExceeded, uncompleted) = uncompletedCleaned.partition {
            (uncompletedTestsRetryCount[it.test] ?: 0) >= configuration.uncompletedTestRetryQuota
        }

        uncompletedTests.forEach {
            uncompletedTestsRetryCount[it.test] = (uncompletedTestsRetryCount[it.test] ?: 0) + 1
        }

        if (uncompletedRetryQuotaExceeded.isNotEmpty()) {
            logger.debug(
                "[{}] Uncompleted test retry quota exceeded for {}",
                device.serialNumber,
                uncompletedRetryQuotaExceeded.joinToString(separator = ", ") { it.test.toTestName() },
            )
            val uncompletedToFailed = uncompletedRetryQuotaExceeded.map {
                it.copy(status = TestStatus.FAILURE)
            }
            for (test in uncompletedToFailed) {
                testResultReporter.testIncomplete(device, test, final = true)
            }
        }

        if (uncompleted.isNotEmpty()) {
            for (test in uncompleted) {
                testResultReporter.testIncomplete(device, test, final = false)
            }
            returnTests(uncompleted.map { it.test })
            progressReporter.addRetries(poolId, uncompleted.size)
        }
        activeBatches.remove(device.serialNumber)
    }

    private suspend fun onReturnBatch(device: DeviceInfo, batch: TestBatch) {
        logger.debug("[{}] Batch returned", device.serialNumber)

        val uncompletedTests = batch.tests
        val results = uncompletedTests.map {
            val currentTimeMillis = timer.currentTimeMillis()
            TestResult(
                test = it,
                device = device,
                status = TestStatus.INCOMPLETE,
                startTime = currentTimeMillis,
                endTime = currentTimeMillis + 1,
                batchId = batch.id,
            )
        }

        handleUncompletedTests(results, device)
        activeBatches.remove(device.serialNumber)
        if (queue.isNotEmpty()) {
            pool.send(FromQueue.Notify)
        }
    }

    private fun returnTests(tests: Collection<Test>) {
        queue.addAll(tests)
    }

    private fun onTerminate() {
        close()
    }

    private fun handleFinishedTests(finished: Collection<TestResult>, device: DeviceInfo) {
        finished.filter { flakyTests.contains(it.test) }.let {
            it.forEach { testResult ->
                val oldSize = queue.size
                queue.removeAll(listOf(testResult.test))
                val diff = oldSize - queue.size
                testResultReporter.removeTest(testResult.test, diff)
                progressReporter.removeTests(poolId, diff)
                flakyTests = flakyTests.filter { item -> item != testResult.test }
            }
        }
        finished.forEach {
            testResultReporter.testFinished(device, it)
        }
    }

    private fun handleFailedTests(failed: Collection<TestResult>, device: DeviceInfo) {
        logger.debug("[{}] Handling failed tests", device.serialNumber)
        val retryList = retry
            .process(poolId, failed, flakyTests)
            .filter {
                // strict run tests and tests with fail fast failure stack traces should not be re-run in a batch
                !strictRunChecker.isStrictRun(it.test) && !strictRunChecker.hasFailFastFailures(it.stacktrace)
            }

        progressReporter.addRetries(poolId, retryList.size)
        queue.addAll(retryList.map { it.test })
        retryList.forEach {
            testResultReporter.retryTest(device, it)
        }

        failed.filterNot { testResult ->
            retryList.map { it.test }.contains(testResult.test)
        }.forEach {
            testResultReporter.testFailed(device, it)
        }
    }

    private suspend fun onRequestBatch(device: DeviceInfo) {
        logger.debug("[{}] Requested next batch", device.serialNumber)
        val queueIsEmpty = queue.isEmpty()
        if (queue.isNotEmpty() && !activeBatches.containsKey(device.serialNumber)) {
            logger.debug("[{}] Sending next batch", device.serialNumber)
            sendBatch(device)
            return
        }
        if (queueIsEmpty && activeBatches.isEmpty()) {
            if (stopRequested) {
                logger.debug("[{}] Queue is empty and stop requested. Terminating", device.serialNumber)
                terminate()
            } else {
                logger.debug("[{}] Queue is empty and stop is not requested yet, no batches available", device.serialNumber)
            }
        } else if (queueIsEmpty) {
            logger.debug("[{}] Queue is empty but there are active batches present for {}", device.serialNumber, activeBatches.keys.joinToString { it })
        }
    }

    private suspend fun terminate() {
        pool.send(FromQueue.Terminated)
        onTerminate()
    }

    private suspend fun sendBatch(device: DeviceInfo) {
        val batch = batching.process(queue, analytics)
        activeBatches[device.serialNumber] = batch
        pool.send(FromQueue.ExecuteBatch(device, batch))
    }
}

sealed class QueueMessage {
    data class AddShard(val shard: TestShard) : QueueMessage()
    data class RequestBatch(val device: DeviceInfo) : QueueMessage()
    data class IsEmpty(val deferred: CompletableDeferred<Boolean>) : QueueMessage()
    data class Completed(
        val device: DeviceInfo,
        val results: TestBatchResults,
    ) : QueueMessage()
    data class ReturnBatch(
        val device: DeviceInfo,
        val batch: TestBatch,
        val reason: String,
    ) : QueueMessage()

    data object Stop : QueueMessage()
    data object Terminate : QueueMessage()
}

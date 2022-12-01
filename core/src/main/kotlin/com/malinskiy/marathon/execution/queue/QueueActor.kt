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
import java.util.*
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
    coroutineContext: CoroutineContext
) :
    Actor<QueueMessage>(parent = poolJob, context = coroutineContext) {

    private val logger = MarathonLogging.logger("QueueActor[$poolId]")

    private val sorting = configuration.sortingStrategy

    private val queue: Queue<Test> = PriorityQueue<Test>(sorting.process(analytics))
    private val batching = configuration.batchingStrategy
    private val retry = configuration.retryStrategy

    private val activeBatches = mutableMapOf<String, TestBatch>()
    private val uncompletedTestsRetryCount = mutableMapOf<Test, Int>()

    private val testResultReporter = TestResultReporter(poolId, analytics, configuration, track)
    private var flakyTests: List<Test> = emptyList()

    var stopRequested: Boolean = false
        private set

    override suspend fun receive(msg: QueueMessage) {
        when (msg) {
            is QueueMessage.AddShard -> {
                testResultReporter.addShard(msg.shard)
                val testsToAdd = msg.shard.tests + msg.shard.flakyTests
                queue.addAll(testsToAdd)
                progressReporter.addRetries(poolId, testsToAdd.size)
                flakyTests = flakyTests + msg.shard.flakyTests

                if (queue.isNotEmpty()) {
                    pool.send(FromQueue.Notify)
                }
            }

            is QueueMessage.RequestBatch -> {
                onRequestBatch(msg.device)
                if (queue.isNotEmpty()) {
                    logger.info("Queue it not empty, trying to request an emulator")
                    pool.send(FromQueue.Notify)
                }
            }

            is QueueMessage.IsEmpty -> {
                msg.deferred.complete(queue.isEmpty() && activeBatches.isEmpty())
            }

            is QueueMessage.Stop -> {
                stopRequested = true

                if (queue.isEmpty() && activeBatches.isEmpty()) {
                    logger.debug { "Stop requested, queue is empty and no active batches present, terminating" }
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
        val updatedResults = updateUncompletedTests(results)

        val finished = updatedResults.finished
        val failed = updatedResults.failed

        logger.debug { "handle test results ${device.serialNumber}" }
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
        val batchLogs = logProvider.getBatchReport(batchId) ?: null
            .also {
                logger.warn { "no logs for batch = $batchId" }
            }

        val (failedFromUncompleted, uncompleted) = results
            .uncompleted
            .partitionFastFailures(batchLogs)

        failedFromUncompleted.forEach {
            logger.warn { "uncompleted test run marked as failed for ${it.test.toTestName()} as error message matches to fast test failures" }
        }

        val (newUncompleted, failed) = results
            .failed
            .partitionIgnoredFailures(batchLogs)

        newUncompleted.forEach {
            logger.debug { "failed test run marked as uncompleted for ${it.test.toTestName()} as error message matches to ignored test failures" }
        }

        return results.copy(
            failed = failed + failedFromUncompleted,
            uncompleted = uncompleted + newUncompleted
        )
    }

    private fun Iterable<TestResult>.partitionFastFailures(batchLogs: BatchLogs?): Pair<List<TestResult>, List<TestResult>> =
        partition {
            if (it.hasFailFastFailureStackTrace()) {
                true
            } else {
                val log = batchLogs?.tests?.get(it.test.toLogTest())
                log?.hasFailFastFailureCrashLogEvent() ?: false
            }
        }

    private fun Iterable<TestResult>.partitionIgnoredFailures(batchLogs: BatchLogs?): Pair<List<TestResult>, List<TestResult>> =
        partition {
            if (it.hasIgnoredFailureStackTrace()) {
                true
            } else {
                val log = batchLogs?.tests?.get(it.test.toLogTest())
                log?.hasIgnoredCrashLogEvent() ?: false
            }
        }

    private fun TestResult.hasIgnoredFailureStackTrace(): Boolean =
        stacktrace
            ?.let { stacktrace ->
                configuration.ignoreFailureRegexes.any { regexp -> regexp.matches(stacktrace) }
            }
            ?: false

    private fun TestResult.hasFailFastFailureStackTrace(): Boolean =
        stacktrace
            ?.let { stacktrace ->
                configuration.failFastFailureRegexes.any { regexp -> regexp.matches(stacktrace) }
            }
            ?: false

    private fun Log.hasIgnoredCrashLogEvent(): Boolean =
        events
            .any { logEvent ->
                logEvent is LogEvent.Crash && configuration.ignoreFailureRegexes.any { regexp -> regexp.matches(logEvent.message) }
            }

    private fun Log.hasFailFastFailureCrashLogEvent(): Boolean =
        events
            .any { logEvent ->
                logEvent is LogEvent.Crash && configuration.failFastFailureRegexes.any { regexp -> regexp.matches(logEvent.message) }
            }

    private fun handleUncompletedTests(uncompletedTests: Collection<TestResult>, device: DeviceInfo) {
        val (uncompletedFailFastFailed, uncompletedCleaned) = uncompletedTests.partition { it.hasFailFastFailureStackTrace() }

        if (uncompletedFailFastFailed.isNotEmpty()) {
            logger.debug { "uncompleted test failed because of stacktrace for ${uncompletedFailFastFailed.joinToString(separator = ", ") { it.test.toTestName() }}" }
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
            logger.debug { "uncompletedRetryQuotaExceeded for ${uncompletedRetryQuotaExceeded.joinToString(separator = ", ") { it.test.toTestName() }}" }
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
        logger.debug { "onReturnBatch ${device.serialNumber}" }

        val uncompletedTests = batch.tests
        val results = uncompletedTests.map {
            val currentTimeMillis = timer.currentTimeMillis()
            TestResult(
                it,
                device,
                TestStatus.INCOMPLETE,
                currentTimeMillis,
                currentTimeMillis + 1,
                batch.id
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
            it.forEach {
                val oldSize = queue.size
                queue.removeAll(listOf(it.test))
                val diff = oldSize - queue.size
                testResultReporter.removeTest(it.test, diff)
                progressReporter.removeTests(poolId, diff)
                flakyTests = flakyTests.filter { item -> item != it.test }
            }
        }
        finished.forEach {
            testResultReporter.testFinished(device, it)
        }
    }

    private suspend fun handleFailedTests(
        failed: Collection<TestResult>,
        device: DeviceInfo
    ) {
        logger.debug { "handle failed tests ${device.serialNumber}" }
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

        failed.filterNot {
            retryList.map { it.test }.contains(it.test)
        }.forEach {
            testResultReporter.testFailed(device, it)
        }
    }


    private suspend fun onRequestBatch(device: DeviceInfo) {
        logger.debug { "request next batch for device ${device.serialNumber}" }
        val queueIsEmpty = queue.isEmpty()
        if (queue.isNotEmpty() && !activeBatches.containsKey(device.serialNumber)) {
            logger.debug { "sending next batch for device ${device.serialNumber}" }
            sendBatch(device)
            return
        }
        if (queueIsEmpty && activeBatches.isEmpty()) {
            if (stopRequested) {
                logger.debug { "queue is empty and stop requested, terminating ${device.serialNumber}" }
                terminate()
            } else {
                logger.debug { "queue is empty and stop is not requested yet, no batches available for ${device.serialNumber}" }
            }
        } else if (queueIsEmpty) {
            logger.debug {
                "queue is empty but there are active batches present for " +
                    activeBatches.keys.joinToString { it }
            }
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
    data class Completed(val device: DeviceInfo, val results: TestBatchResults) : QueueMessage()
    data class ReturnBatch(val device: DeviceInfo, val batch: TestBatch, val reason: String) : QueueMessage()

    object Stop : QueueMessage()
    object Terminate : QueueMessage()
}

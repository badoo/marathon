package com.malinskiy.marathon.execution.queue

import com.malinskiy.marathon.analytics.external.Analytics
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.device.StubDevice
import com.malinskiy.marathon.device.toDeviceInfo
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.ConfigurationStrictRunChecker
import com.malinskiy.marathon.execution.DevicePoolMessage.FromQueue
import com.malinskiy.marathon.execution.TestBatchResults
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.execution.TestShard
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.execution.strategy.impl.batching.FixedSizeBatchingStrategy
import com.malinskiy.marathon.execution.stubTestBatchResults
import com.malinskiy.marathon.execution.stubTestResult
import com.malinskiy.marathon.report.logs.BatchLogs
import com.malinskiy.marathon.report.logs.Log
import com.malinskiy.marathon.report.logs.LogEvent
import com.malinskiy.marathon.report.logs.LogsProvider
import com.malinskiy.marathon.report.logs.StubLogsProvider
import com.malinskiy.marathon.report.logs.toLogTest
import com.malinskiy.marathon.test.factory.configuration
import com.malinskiy.marathon.test.stubTest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.io.File

class QueueActorTest {

    private lateinit var track: Track
    lateinit var job: Job
    lateinit var poolChannel: Channel<FromQueue>
    lateinit var analytics: Analytics

    lateinit var actor: QueueActor
    lateinit var testResultCaptor: KArgumentCaptor<TestResult>
    lateinit var testBatchResults: TestBatchResults

    @BeforeEach
    fun setup() {
        track = mock()
        analytics = mock()
        job = Job()
        poolChannel = Channel()
    }

    @AfterEach
    fun teardown() {
        reset(track, analytics)
        job.cancel()
    }

    @Test
    fun `setup 1 should have empty queue`() = runTest {
        setup_1___uncompleted_retry_quota_0_and_batch_size_1()

        val isEmptyDeferred = CompletableDeferred<Boolean>()

        actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
        poolChannel.receive()
        actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))
        actor.send(QueueMessage.IsEmpty(isEmptyDeferred))
        assertTrue(isEmptyDeferred.await())
    }

    @Test
    fun `setup 1 should report failure`() = runTest {
        setup_1___uncompleted_retry_quota_0_and_batch_size_1()

        actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
        poolChannel.receive()
        actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))
        verify(track).test(any(), any(), testResultCaptor.capture(), any())
        assertEquals(TEST_1, testResultCaptor.firstValue.test)
        assertEquals(TestStatus.FAILURE, testResultCaptor.firstValue.status)
    }

    @Test
    fun `setup 2 should have non empty queue`() = runTest {
        setup_2___uncompleted_retry_quota_1_and_batch_size_1()

        val isEmptyDeferred = CompletableDeferred<Boolean>()
        actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
        assertInstanceOf(FromQueue.ExecuteBatch::class.java, poolChannel.receive())
        actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))
        assertInstanceOf(FromQueue.Notify::class.java, poolChannel.receive())
        actor.send(QueueMessage.IsEmpty(isEmptyDeferred))
        assertFalse(isEmptyDeferred.await())
    }

    @Test
    fun `setup 2 should report test failed`() = runTest {
        setup_2___uncompleted_retry_quota_1_and_batch_size_1()

        actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
        poolChannel.receive()
        actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))

        actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
        poolChannel.receive()
        actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))

        verify(track, times(1)).test(any(), any(), testResultCaptor.capture(), any())
        assertEquals(TEST_1, testResultCaptor.firstValue.test)
        assertEquals(TestStatus.FAILURE, testResultCaptor.firstValue.status)
    }

    @Test
    fun `setup 2 should provide uncompleted test in the batch`() = runTest {
        setup_2___uncompleted_retry_quota_1_and_batch_size_1()

        actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
        assertInstanceOf(FromQueue.ExecuteBatch::class.java, poolChannel.receive())
        actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))
        assertInstanceOf(FromQueue.Notify::class.java, poolChannel.receive())
        actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
        val actual = poolChannel.receive()

        assertInstanceOf(FromQueue.ExecuteBatch::class.java, actual)
        assertIterableEquals(listOf(TEST_1), (actual as FromQueue.ExecuteBatch).batch.tests)
    }

    @Test
    fun `setup 2 should have empty queue`() = runTest {
        setup_2___uncompleted_retry_quota_1_and_batch_size_1()
        val isEmptyDeferred = CompletableDeferred<Boolean>()
        actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
        assertInstanceOf(FromQueue.ExecuteBatch::class.java, poolChannel.receive())
        actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))

        actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
        assertInstanceOf(FromQueue.Notify::class.java, poolChannel.receive())
        assertInstanceOf(FromQueue.ExecuteBatch::class.java, poolChannel.receive())
        actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))

        actor.send(QueueMessage.IsEmpty(isEmptyDeferred))
        assertTrue(isEmptyDeferred.await())
    }

    @Test
    fun `setup 2 should report test as failed`() = runTest {
        setup_2___uncompleted_retry_quota_1_and_batch_size_1()

        actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
        poolChannel.receive()
        actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))

        actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
        poolChannel.receive()
        actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))

        verify(track).test(any(), any(), testResultCaptor.capture(), any())
        assertEquals(TEST_1, testResultCaptor.firstValue.test)
        assertEquals(TestStatus.FAILURE, testResultCaptor.firstValue.status)
    }

    @Test
    fun `failed test with log event that matches crash filter - crashes after uncompleted quota reached - should report test as failed`() = runTest {
        failed_test_with_crash_log_matches_crash_filter_with_uncompleted_retry_quota_1_and_batch_size_1()

        actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
        poolChannel.receive()
        actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))

        actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
        poolChannel.receive()
        actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))

        verify(track).test(any(), any(), testResultCaptor.capture(), any())
        assertEquals(TEST_1, testResultCaptor.firstValue.test)
        assertEquals(TestStatus.FAILURE, testResultCaptor.firstValue.status)
    }

    @Test
    fun `failed test with log event that matches crash filter - should provide uncompleted test in the batch`() = runTest {
        failed_test_with_crash_log_matches_crash_filter_with_uncompleted_retry_quota_1_and_batch_size_1()

        actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
        poolChannel.receive()
        actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))
        actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
        assertInstanceOf(FromQueue.Notify::class.java, poolChannel.receive())
        val response = poolChannel.receive()
        assertInstanceOf(FromQueue.ExecuteBatch::class.java, response)
        assertIterableEquals(listOf(TEST_1), (response as FromQueue.ExecuteBatch).batch.tests)
    }

    @Test
    fun `failed test with stacktrace that matches crash filter - crashes after uncompleted quota reached - should report test as failed`() = runTest {
        failed_test_with_stacktrace_matches_crash_filter_with_uncompleted_retry_quota_1_and_batch_size_1()

        actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
        poolChannel.receive()
        actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))

        actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
        poolChannel.receive()
        actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))

        verify(track).test(any(), any(), testResultCaptor.capture(), any())
        assertEquals(TEST_1, testResultCaptor.firstValue.test)
        assertEquals(TestStatus.FAILURE, testResultCaptor.firstValue.status)
    }

    @Test
    fun `failed test with stacktrace that matches crash filter - should provide uncompleted test in the batch`() = runTest {
        failed_test_with_stacktrace_matches_crash_filter_with_uncompleted_retry_quota_1_and_batch_size_1()

        actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
        poolChannel.receive()
        actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))
        actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
        assertInstanceOf(FromQueue.Notify::class.java, poolChannel.receive())
        val response = poolChannel.receive()
        assertInstanceOf(FromQueue.ExecuteBatch::class.java, response)
        assertIterableEquals(listOf(TEST_1), (response as FromQueue.ExecuteBatch).batch.tests)
    }

    /**
     * uncompleted tests retry quota is 0, max batch size is 1 and one test in the shard and processing finished
     */
    private suspend fun setup_1___uncompleted_retry_quota_0_and_batch_size_1() {
        actor = createQueueActor(
            configuration = DEFAULT_CONFIGURATION.copy(
                uncompletedTestRetryQuota = 0,
                batchingStrategy = FixedSizeBatchingStrategy(size = 1)
            ),
            tests = listOf(TEST_1),
            poolChannel = poolChannel,
            analytics = analytics,
            job = job,
            track = track
        )
        testResultCaptor = argumentCaptor<TestResult>()
        testBatchResults = createBatchResult(
            uncompleted = listOf(
                createTestResult(TEST_1, TestStatus.FAILURE)
            )
        )
    }

    /**
     * uncompleted tests retry quota is 1, max batch size is 1 and one test in the shard
     */
    private suspend fun setup_2___uncompleted_retry_quota_1_and_batch_size_1() {
        actor = createQueueActor(
            configuration = DEFAULT_CONFIGURATION.copy(
                uncompletedTestRetryQuota = 1,
                batchingStrategy = FixedSizeBatchingStrategy(size = 1)
            ),
            tests = listOf(TEST_1),
            poolChannel = poolChannel,
            analytics = analytics,
            job = job,
            track = track
        )
        testResultCaptor = argumentCaptor<TestResult>()
        testBatchResults = createBatchResult(
            uncompleted = listOf(
                createTestResult(TEST_1, TestStatus.FAILURE)
            )
        )
    }

    private suspend fun failed_test_with_crash_log_matches_crash_filter_with_uncompleted_retry_quota_1_and_batch_size_1() {
        val crashEvent = LogEvent.Crash(message = "Process exited with signal 11 (SIGSEGV)")
        val log = Log(File(""), listOf(crashEvent))
        val logsProvider = StubLogsProvider(
            mapOf(
                TEST_BATCH_ID to BatchLogs(
                    tests = mapOf(
                        TEST_1.toLogTest() to log
                    ),
                    log = log
                )
            )
        )
        actor = createQueueActor(
            configuration = DEFAULT_CONFIGURATION.copy(
                uncompletedTestRetryQuota = 1,
                batchingStrategy = FixedSizeBatchingStrategy(size = 1),
                ignoreFailureRegexes = listOf(".*SIGSEGV.*".toRegex(RegexOption.DOT_MATCHES_ALL))
            ),
            tests = listOf(TEST_1),
            poolChannel = poolChannel,
            analytics = analytics,
            logsProvider = logsProvider,
            job = job,
            track = track
        )
        testResultCaptor = argumentCaptor()
        testBatchResults = createBatchResult(
            failed = listOf(
                createTestResult(TEST_1, TestStatus.FAILURE)
            )
        )
    }

    private suspend fun failed_test_with_stacktrace_matches_crash_filter_with_uncompleted_retry_quota_1_and_batch_size_1() {
        val logsProvider = StubLogsProvider()
        actor = createQueueActor(
            configuration = DEFAULT_CONFIGURATION.copy(
                uncompletedTestRetryQuota = 1,
                batchingStrategy = FixedSizeBatchingStrategy(size = 1),
                ignoreFailureRegexes = listOf(".*UiAutomation not connected.*".toRegex(RegexOption.DOT_MATCHES_ALL))
            ),
            tests = listOf(TEST_1),
            poolChannel = poolChannel,
            analytics = analytics,
            logsProvider = logsProvider,
            job = job,
            track = track
        )
        testResultCaptor = argumentCaptor()
        testBatchResults = createBatchResult(
            failed = listOf(
                createTestResult(TEST_1, TestStatus.FAILURE, stacktrace = "java.lang.IllegalStateException: UiAutomation not connected!")
            )
        )
    }
}

private const val TEST_BATCH_ID = "test-batch"
private val TEST_DEVICE = StubDevice()
private val TEST_DEVICE_INFO = TEST_DEVICE.toDeviceInfo()
private val TEST_1 = stubTest(pkg = "", clazz = "", method = "test1")

private fun createBatchResult(
    finished: List<TestResult> = emptyList(),
    failed: List<TestResult> = emptyList(),
    uncompleted: List<TestResult> = emptyList()
): TestBatchResults = stubTestBatchResults(
    batchId = TEST_BATCH_ID,
    device = TEST_DEVICE,
    finished = finished,
    failed = failed,
    uncompleted = uncompleted
)

private fun createTestResult(test: com.malinskiy.marathon.test.Test, status: TestStatus, stacktrace: String? = null) =
    stubTestResult(test = test, device = TEST_DEVICE_INFO, status = status, endTime = 0, stacktrace = stacktrace)

private suspend fun createQueueActor(
    configuration: Configuration,
    tests: List<com.malinskiy.marathon.test.Test>,
    poolChannel: Channel<FromQueue>,
    analytics: Analytics,
    track: Track,
    logsProvider: LogsProvider = mock(),
    job: Job
) = QueueActor(
    configuration,
    analytics,
    poolChannel,
    DevicePoolId("test"),
    mock(),
    track,
    mock(),
    logsProvider,
    ConfigurationStrictRunChecker(configuration),
    job,
    Dispatchers.Unconfined
)
    .apply {
        send(QueueMessage.AddShard(TestShard(tests, emptyList())))
        poolChannel.receive()
    }

private val DEFAULT_CONFIGURATION = configuration()

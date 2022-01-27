package com.malinskiy.marathon.execution.queue

import com.malinskiy.marathon.analytics.external.Analytics
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.device.DeviceStub
import com.malinskiy.marathon.device.toDeviceInfo
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.ConfigurationStrictRunChecker
import com.malinskiy.marathon.execution.DevicePoolMessage.FromQueue
import com.malinskiy.marathon.execution.TestBatchResults
import com.malinskiy.marathon.execution.TestLogsProvider
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.execution.TestShard
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.execution.strategy.impl.batching.FixedSizeBatchingStrategy
import com.malinskiy.marathon.report.logs.BatchLogs
import com.malinskiy.marathon.report.logs.Log
import com.malinskiy.marathon.report.logs.LogEvent
import com.malinskiy.marathon.report.logs.LogsProvider
import com.malinskiy.marathon.report.logs.toLogTest
import com.malinskiy.marathon.test.TestComponentInfo
import com.malinskiy.marathon.test.factory.configuration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.amshove.kluent.shouldBe
import org.amshove.kluent.shouldContainSame
import org.junit.jupiter.api.AfterEach
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
    fun `setup 1 should have empty queue`() {
        setup_1___uncompleted_retry_quota_0_and_batch_size_1()

        val isEmptyDeferred = CompletableDeferred<Boolean>()
        runBlocking {
            actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
            poolChannel.receive()
            actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))
            actor.send(QueueMessage.IsEmpty(isEmptyDeferred))
            isEmptyDeferred.await() shouldBe true
        }
    }

    @Test
    fun `setup 1 should report failure`() {
        setup_1___uncompleted_retry_quota_0_and_batch_size_1()

        runBlocking {
            actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
            poolChannel.receive()
            actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))
            verify(track).test(any(), any(), testResultCaptor.capture(), any())
            testResultCaptor.firstValue.test shouldBe TEST_1
            testResultCaptor.firstValue.status shouldBe TestStatus.FAILURE
        }
    }

    @Test
    fun `setup 2 should have non empty queue`() {
        setup_2___uncompleted_retry_quota_1_and_batch_size_1()

        val isEmptyDeferred = CompletableDeferred<Boolean>()
        runBlocking {
            actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
            poolChannel.receive()
            actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))
            actor.send(QueueMessage.IsEmpty(isEmptyDeferred))
            isEmptyDeferred.await() shouldBe false
        }
    }

    @Test
    fun `setup 2 should report test failed`() {
        setup_2___uncompleted_retry_quota_1_and_batch_size_1()

        runBlocking {
            actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
            poolChannel.receive()
            actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))

            actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
            poolChannel.receive()
            actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))

            verify(track, times(1)).test(any(), any(), testResultCaptor.capture(), any())
            testResultCaptor.firstValue.test shouldBe TEST_1
            testResultCaptor.firstValue.status shouldBe TestStatus.FAILURE
        }
    }

    @Test
    fun `setup 2 should provide uncompleted test in the batch`() {
        setup_2___uncompleted_retry_quota_1_and_batch_size_1()
        runBlocking {
            actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
            poolChannel.receive()
            actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))
            actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
            val response = poolChannel.receive()
            response::class shouldBe FromQueue.ExecuteBatch::class
            (response as FromQueue.ExecuteBatch).batch.tests shouldContainSame listOf(TEST_1)
        }
    }

    @Test
    fun `setup 2 should have empty queue`() {
        setup_2___uncompleted_retry_quota_1_and_batch_size_1()
        val isEmptyDeferred = CompletableDeferred<Boolean>()
        runBlocking {
            actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
            poolChannel.receive()
            actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))

            actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
            poolChannel.receive()
            actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))

            actor.send(QueueMessage.IsEmpty(isEmptyDeferred))
            isEmptyDeferred.await() shouldBe true
        }
    }

    @Test
    fun `setup 2 should report test as failed`() {
        setup_2___uncompleted_retry_quota_1_and_batch_size_1()

        runBlocking {
            actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
            poolChannel.receive()
            actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))

            actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
            poolChannel.receive()
            actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))

            verify(track).test(any(), any(), testResultCaptor.capture(), any())
            testResultCaptor.firstValue.test shouldBe TEST_1
            testResultCaptor.firstValue.status shouldBe TestStatus.FAILURE
        }
    }

    @Test
    fun `failed test with log event that matches crash filter - crashes after uncompleted quota reached - should report test as failed`() {
        failed_test_with_crash_log_matches_crash_filter_with_uncompleted_retry_quota_1_and_batch_size_1()

        runBlocking {
            actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
            poolChannel.receive()
            actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))

            actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
            poolChannel.receive()
            actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))

            verify(track).test(any(), any(), testResultCaptor.capture(), any())
            testResultCaptor.firstValue.test shouldBe TEST_1
            testResultCaptor.firstValue.status shouldBe TestStatus.FAILURE
        }
    }

    @Test
    fun `failed test with log event that matches crash filter - should provide uncompleted test in the batch`() {
        failed_test_with_crash_log_matches_crash_filter_with_uncompleted_retry_quota_1_and_batch_size_1()

        runBlocking {
            actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
            poolChannel.receive()
            actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))
            actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
            val response = poolChannel.receive()
            response::class shouldBe FromQueue.ExecuteBatch::class
            (response as FromQueue.ExecuteBatch).batch.tests shouldContainSame listOf(TEST_1)
        }
    }

    @Test
    fun `failed test with stacktrace that matches crash filter - crashes after uncompleted quota reached - should report test as failed`() {
        failed_test_with_stacktrace_matches_crash_filter_with_uncompleted_retry_quota_1_and_batch_size_1()

        runBlocking {
            actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
            poolChannel.receive()
            actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))

            actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
            poolChannel.receive()
            actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))

            verify(track).test(any(), any(), testResultCaptor.capture(), any())
            testResultCaptor.firstValue.test shouldBe TEST_1
            testResultCaptor.firstValue.status shouldBe TestStatus.FAILURE
        }
    }

    @Test
    fun `failed test with stacktrace that matches crash filter - should provide uncompleted test in the batch`() {
        failed_test_with_stacktrace_matches_crash_filter_with_uncompleted_retry_quota_1_and_batch_size_1()

        runBlocking {
            actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
            poolChannel.receive()
            actor.send(QueueMessage.Completed(TEST_DEVICE_INFO, testBatchResults))
            actor.send(QueueMessage.RequestBatch(TEST_DEVICE_INFO))
            val response = poolChannel.receive()
            response::class shouldBe FromQueue.ExecuteBatch::class
            (response as FromQueue.ExecuteBatch).batch.tests shouldContainSame listOf(TEST_1)
        }
    }

    /**
     * uncompleted tests retry quota is 0, max batch size is 1 and one test in the shard and processing finished
     */
    private fun setup_1___uncompleted_retry_quota_0_and_batch_size_1() {
        actor =
            createQueueActor(
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
    private fun setup_2___uncompleted_retry_quota_1_and_batch_size_1() {
        actor =
            createQueueActor(
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

    private fun failed_test_with_crash_log_matches_crash_filter_with_uncompleted_retry_quota_1_and_batch_size_1() {
        val crashEvent = LogEvent.Crash(message = "Process exited with signal 11 (SIGSEGV)")
        val log = Log(File(""), listOf(crashEvent))
        val logsProvider = TestLogsProvider(
            mapOf(
                TEST_BATCH_ID to BatchLogs(
                    tests = mapOf(
                        TEST_1.toLogTest() to log
                    ),
                    log = log
                )
            )
        )
        actor =
            createQueueActor(
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

    private fun failed_test_with_stacktrace_matches_crash_filter_with_uncompleted_retry_quota_1_and_batch_size_1() {
        val logsProvider = TestLogsProvider(emptyMap())
        actor =
            createQueueActor(
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
private val TEST_DEVICE = DeviceStub()
private val TEST_DEVICE_INFO = TEST_DEVICE.toDeviceInfo()
private val TEST_1 = com.malinskiy.marathon.test.Test("", "", "test1", emptyList(), TestComponentInfo())

private fun createBatchResult(
    finished: List<TestResult> = emptyList(),
    failed: List<TestResult> = emptyList(),
    uncompleted: List<TestResult> = emptyList()
): TestBatchResults = TestBatchResults(
    TEST_BATCH_ID,
    TEST_DEVICE,
    TestComponentInfo(),
    finished,
    failed,
    uncompleted
)

private fun createTestResult(test: com.malinskiy.marathon.test.Test, status: TestStatus, stacktrace: String? = null) = TestResult(
    test = test,
    device = TEST_DEVICE_INFO,
    status = status,
    startTime = 0,
    endTime = 0,
    stacktrace = stacktrace,
    attachments = emptyList(),
    batchId = "test_batch_id"
)

private fun createQueueActor(
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
    logsProvider,
    ConfigurationStrictRunChecker(configuration),
    job,
    Dispatchers.Unconfined
)
    .apply {
        runBlocking {
            send(QueueMessage.AddShard(TestShard(tests, emptyList())))
            poolChannel.receive()
        }
    }

private val DEFAULT_CONFIGURATION = configuration()

package com.malinskiy.marathon.execution.queue

import com.malinskiy.marathon.analytics.external.Analytics
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.device.DeviceInfo
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
import com.malinskiy.marathon.execution.progress.ProgressReporter
import com.malinskiy.marathon.execution.strategy.impl.batching.FixedSizeBatchingStrategy
import com.malinskiy.marathon.execution.stubTestBatchResults
import com.malinskiy.marathon.execution.stubTestResult
import com.malinskiy.marathon.report.logs.BatchLogs
import com.malinskiy.marathon.report.logs.Log
import com.malinskiy.marathon.report.logs.LogEvent
import com.malinskiy.marathon.report.logs.StubLogsProvider
import com.malinskiy.marathon.report.logs.toLogTest
import com.malinskiy.marathon.test.factory.configuration
import com.malinskiy.marathon.test.stubTest
import com.malinskiy.marathon.time.StubTimer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.KArgumentCaptor
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import java.io.File
import com.malinskiy.marathon.test.Test as MarathonTest

class QueueActorTest {
    private val analytics = mock<Analytics>()
    private val device = StubDevice()
    private val deviceInfo: DeviceInfo get() = device.toDeviceInfo()
    private val logsProvider = StubLogsProvider()
    private val progressReporter = mock<ProgressReporter>()
    private val timer = StubTimer()
    private val track = mock<Track>()
    private lateinit var job: Job
    private lateinit var poolChannel: Channel<FromQueue>

    private lateinit var actor: QueueActor
    private lateinit var testResultCaptor: KArgumentCaptor<TestResult>
    private lateinit var testBatchResults: TestBatchResults

    @BeforeEach
    fun setup() {
        job = Job()
        poolChannel = Channel()
    }

    @AfterEach
    fun teardown() {
        job.cancel()
    }

    @Test
    fun `setup 1 should have empty queue`() = runTest {
        setup_1___uncompleted_retry_quota_0_and_batch_size_1()

        val isEmptyDeferred = CompletableDeferred<Boolean>()

        actor.send(QueueMessage.RequestBatch(deviceInfo))
        poolChannel.receive()
        actor.send(QueueMessage.Completed(deviceInfo, testBatchResults))
        actor.send(QueueMessage.IsEmpty(isEmptyDeferred))
        assertThat(isEmptyDeferred.await()).isTrue()
    }

    @Test
    fun `setup 1 should report failure`() = runTest {
        setup_1___uncompleted_retry_quota_0_and_batch_size_1()

        actor.send(QueueMessage.RequestBatch(deviceInfo))
        poolChannel.receive()
        actor.send(QueueMessage.Completed(deviceInfo, testBatchResults))
        verify(track).test(any(), any(), testResultCaptor.capture(), any())
        assertThat(testResultCaptor.firstValue.test).isEqualTo(TEST_1)
        assertThat(testResultCaptor.firstValue.status).isEqualTo(TestStatus.FAILURE)
    }

    @Test
    fun `setup 2 should have non empty queue`() = runTest {
        setup_2___uncompleted_retry_quota_1_and_batch_size_1()

        val isEmptyDeferred = CompletableDeferred<Boolean>()
        actor.send(QueueMessage.RequestBatch(deviceInfo))
        assertThat(poolChannel.receive()).isInstanceOf(FromQueue.ExecuteBatch::class.java)
        actor.send(QueueMessage.Completed(deviceInfo, testBatchResults))
        assertThat(poolChannel.receive()).isInstanceOf(FromQueue.Notify::class.java)
        actor.send(QueueMessage.IsEmpty(isEmptyDeferred))
        assertThat(isEmptyDeferred.await()).isFalse()
    }

    @Test
    fun `setup 2 should report test failed`() = runTest {
        setup_2___uncompleted_retry_quota_1_and_batch_size_1()

        actor.send(QueueMessage.RequestBatch(deviceInfo))
        poolChannel.receive()
        actor.send(QueueMessage.Completed(deviceInfo, testBatchResults))

        actor.send(QueueMessage.RequestBatch(deviceInfo))
        poolChannel.receive()
        actor.send(QueueMessage.Completed(deviceInfo, testBatchResults))

        verify(track, times(1)).test(any(), any(), testResultCaptor.capture(), any())
        assertThat(testResultCaptor.firstValue.test).isEqualTo(TEST_1)
        assertThat(testResultCaptor.firstValue.status).isEqualTo(TestStatus.FAILURE)
    }

    @Test
    fun `setup 2 should provide uncompleted test in the batch`() = runTest {
        setup_2___uncompleted_retry_quota_1_and_batch_size_1()

        actor.send(QueueMessage.RequestBatch(deviceInfo))
        assertThat(poolChannel.receive()).isInstanceOf(FromQueue.ExecuteBatch::class.java)
        actor.send(QueueMessage.Completed(deviceInfo, testBatchResults))
        assertThat(poolChannel.receive()).isInstanceOf(FromQueue.Notify::class.java)
        actor.send(QueueMessage.RequestBatch(deviceInfo))
        val actual = poolChannel.receive()

        assertThat(actual).isInstanceOf(FromQueue.ExecuteBatch::class.java)
        assertThat((actual as FromQueue.ExecuteBatch).batch.tests).containsExactly(TEST_1)
    }

    @Test
    fun `setup 2 should have empty queue`() = runTest {
        setup_2___uncompleted_retry_quota_1_and_batch_size_1()
        val isEmptyDeferred = CompletableDeferred<Boolean>()
        actor.send(QueueMessage.RequestBatch(deviceInfo))
        assertThat(poolChannel.receive()).isInstanceOf(FromQueue.ExecuteBatch::class.java)
        actor.send(QueueMessage.Completed(deviceInfo, testBatchResults))

        actor.send(QueueMessage.RequestBatch(deviceInfo))
        assertThat(poolChannel.receive()).isInstanceOf(FromQueue.Notify::class.java)
        assertThat(poolChannel.receive()).isInstanceOf(FromQueue.ExecuteBatch::class.java)
        actor.send(QueueMessage.Completed(deviceInfo, testBatchResults))

        actor.send(QueueMessage.IsEmpty(isEmptyDeferred))
        assertThat(isEmptyDeferred.await()).isTrue()
    }

    @Test
    fun `setup 2 should report test as failed`() = runTest {
        setup_2___uncompleted_retry_quota_1_and_batch_size_1()

        actor.send(QueueMessage.RequestBatch(deviceInfo))
        poolChannel.receive()
        actor.send(QueueMessage.Completed(deviceInfo, testBatchResults))

        actor.send(QueueMessage.RequestBatch(deviceInfo))
        poolChannel.receive()
        actor.send(QueueMessage.Completed(deviceInfo, testBatchResults))

        verify(track).test(any(), any(), testResultCaptor.capture(), any())
        assertThat(testResultCaptor.firstValue.test).isEqualTo(TEST_1)
        assertThat(testResultCaptor.firstValue.status).isEqualTo(TestStatus.FAILURE)
    }

    @Test
    fun `failed test with log event that matches crash filter - crashes after uncompleted quota reached - should report test as failed`() = runTest {
        failed_test_with_crash_log_matches_crash_filter_with_uncompleted_retry_quota_1_and_batch_size_1()

        actor.send(QueueMessage.RequestBatch(deviceInfo))
        poolChannel.receive()
        actor.send(QueueMessage.Completed(deviceInfo, testBatchResults))

        actor.send(QueueMessage.RequestBatch(deviceInfo))
        poolChannel.receive()
        actor.send(QueueMessage.Completed(deviceInfo, testBatchResults))

        verify(track).test(any(), any(), testResultCaptor.capture(), any())
        assertThat(testResultCaptor.firstValue.test).isEqualTo(TEST_1)
        assertThat(testResultCaptor.firstValue.status).isEqualTo(TestStatus.FAILURE)
    }

    @Test
    fun `failed test with log event that matches crash filter - should provide uncompleted test in the batch`() = runTest {
        failed_test_with_crash_log_matches_crash_filter_with_uncompleted_retry_quota_1_and_batch_size_1()

        actor.send(QueueMessage.RequestBatch(deviceInfo))
        poolChannel.receive()
        actor.send(QueueMessage.Completed(deviceInfo, testBatchResults))
        actor.send(QueueMessage.RequestBatch(deviceInfo))
        assertThat(poolChannel.receive()).isInstanceOf(FromQueue.Notify::class.java)
        val response = poolChannel.receive()
        assertThat(response).isInstanceOf(FromQueue.ExecuteBatch::class.java)
        assertThat((response as FromQueue.ExecuteBatch).batch.tests).containsExactly(TEST_1)
    }

    @Test
    fun `failed test with stacktrace that matches crash filter - crashes after uncompleted quota reached - should report test as failed`() = runTest {
        failed_test_with_stacktrace_matches_crash_filter_with_uncompleted_retry_quota_1_and_batch_size_1()

        actor.send(QueueMessage.RequestBatch(deviceInfo))
        poolChannel.receive()
        actor.send(QueueMessage.Completed(deviceInfo, testBatchResults))

        actor.send(QueueMessage.RequestBatch(deviceInfo))
        poolChannel.receive()
        actor.send(QueueMessage.Completed(deviceInfo, testBatchResults))

        verify(track).test(any(), any(), testResultCaptor.capture(), any())
        assertThat(testResultCaptor.firstValue.test).isEqualTo(TEST_1)
        assertThat(testResultCaptor.firstValue.status).isEqualTo(TestStatus.FAILURE)
    }

    @Test
    fun `failed test with stacktrace that matches crash filter - should provide uncompleted test in the batch`() = runTest {
        failed_test_with_stacktrace_matches_crash_filter_with_uncompleted_retry_quota_1_and_batch_size_1()

        actor.send(QueueMessage.RequestBatch(deviceInfo))
        poolChannel.receive()
        actor.send(QueueMessage.Completed(deviceInfo, testBatchResults))
        actor.send(QueueMessage.RequestBatch(deviceInfo))
        assertThat(poolChannel.receive()).isInstanceOf(FromQueue.Notify::class.java)
        val response = poolChannel.receive()
        assertThat(response).isInstanceOf(FromQueue.ExecuteBatch::class.java)
        assertThat((response as FromQueue.ExecuteBatch).batch.tests).containsExactly(TEST_1)
    }

    /**
     * uncompleted tests retry quota is 0, max batch size is 1 and one test in the shard and processing finished
     */
    private suspend fun TestScope.setup_1___uncompleted_retry_quota_0_and_batch_size_1() {
        actor = createQueueActor(
            configuration = configuration {
                uncompletedTestRetryQuota = 0
                batchingStrategy = FixedSizeBatchingStrategy(size = 1)
            },
            tests = listOf(TEST_1),
        )
        testResultCaptor = argumentCaptor<TestResult>()
        testBatchResults = createBatchResult(
            uncompleted = listOf(
                createTestResult(TEST_1, TestStatus.FAILURE),
            ),
        )
    }

    /**
     * uncompleted tests retry quota is 1, max batch size is 1 and one test in the shard
     */
    private suspend fun TestScope.setup_2___uncompleted_retry_quota_1_and_batch_size_1() {
        actor = createQueueActor(
            configuration = configuration {
                uncompletedTestRetryQuota = 1
                batchingStrategy = FixedSizeBatchingStrategy(size = 1)
            },
            tests = listOf(TEST_1),
        )
        testResultCaptor = argumentCaptor<TestResult>()
        testBatchResults = createBatchResult(
            uncompleted = listOf(
                createTestResult(TEST_1, TestStatus.FAILURE),
            ),
        )
    }

    private suspend fun TestScope.failed_test_with_crash_log_matches_crash_filter_with_uncompleted_retry_quota_1_and_batch_size_1() {
        val crashEvent = LogEvent.Crash(message = "Process exited with signal 11 (SIGSEGV)")
        val log = Log(File(""), listOf(crashEvent))
        logsProvider.logs = mapOf(
            TEST_BATCH_ID to BatchLogs(
                tests = mapOf(
                    TEST_1.toLogTest() to log,
                ),
                log = log,
            ),
        )
        actor = createQueueActor(
            configuration = configuration {
                uncompletedTestRetryQuota = 1
                batchingStrategy = FixedSizeBatchingStrategy(size = 1)
                ignoreFailureRegexes = listOf(".*SIGSEGV.*".toRegex(RegexOption.DOT_MATCHES_ALL))
            },
            tests = listOf(TEST_1),
        )
        testResultCaptor = argumentCaptor()
        testBatchResults = createBatchResult(
            failed = listOf(
                createTestResult(TEST_1, TestStatus.FAILURE),
            ),
        )
    }

    private suspend fun TestScope.failed_test_with_stacktrace_matches_crash_filter_with_uncompleted_retry_quota_1_and_batch_size_1() {
        actor = createQueueActor(
            configuration = configuration {
                uncompletedTestRetryQuota = 1
                batchingStrategy = FixedSizeBatchingStrategy(size = 1)
                ignoreFailureRegexes = listOf(".*UiAutomation not connected.*".toRegex(RegexOption.DOT_MATCHES_ALL))
            },
            tests = listOf(TEST_1),
        )
        testResultCaptor = argumentCaptor()
        testBatchResults = createBatchResult(
            failed = listOf(
                createTestResult(TEST_1, TestStatus.FAILURE, stacktrace = "java.lang.IllegalStateException: UiAutomation not connected!"),
            ),
        )
    }

    private fun createBatchResult(
        finished: List<TestResult> = emptyList(),
        failed: List<TestResult> = emptyList(),
        uncompleted: List<TestResult> = emptyList(),
    ): TestBatchResults = stubTestBatchResults(
        batchId = TEST_BATCH_ID,
        device = device,
        finished = finished,
        failed = failed,
        uncompleted = uncompleted,
    )

    private fun createTestResult(test: MarathonTest, status: TestStatus, stacktrace: String? = null) =
        stubTestResult(test = test, device = device.toDeviceInfo(), status = status, endTime = 0, stacktrace = stacktrace)

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun TestScope.createQueueActor(configuration: Configuration, tests: List<MarathonTest>) = QueueActor(
        configuration = configuration,
        analytics = analytics,
        pool = poolChannel,
        poolId = DevicePoolId("test"),
        progressReporter = progressReporter,
        track = track,
        timer = timer,
        logProvider = logsProvider,
        strictRunChecker = ConfigurationStrictRunChecker(configuration),
        poolJob = job,
        context = UnconfinedTestDispatcher(testScheduler),
    )
        .apply {
            send(QueueMessage.AddShard(TestShard(tests, emptyList())))
            poolChannel.receive()
        }

    companion object {
        private const val TEST_BATCH_ID = "test-batch"
        private val TEST_1 = stubTest(pkg = "", clazz = "", method = "test1")
    }
}

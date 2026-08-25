package com.malinskiy.marathon.device

import com.malinskiy.marathon.exceptions.TestBatchExecutionException
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.TestBatchResults
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.execution.progress.ProgressReporter
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.TestBatch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay

class StubDevice(
    private val prepareTimeMillis: Long = 5000L,
    private val testTimeMillis: Long = 5000L,
    override val operatingSystem: OperatingSystem = OperatingSystem("25"),
    override val model: String = "test",
    override val manufacturer: String = "test",
    override val networkState: NetworkState = NetworkState.CONNECTED,
    override val deviceFeatures: Collection<DeviceFeature> = listOf(),
    override val abi: String = "test",
    override val serialNumber: String = "serial-1",
    override val healthy: Boolean = true,
    val crashWithTestBatchException: Boolean = false,
) : Device {

    lateinit var executionResults: Map<Test, Array<TestStatus>>
    val executionIndexMap: MutableMap<Test, Int> = mutableMapOf()
    var timeCounter: Long = 0

    var prepareAction: (suspend () -> Unit)? = null
    var executeAction: (suspend (CompletableDeferred<TestBatchResults>) -> Unit)? = null
    var prepareCount: Int = 0
        private set

    override suspend fun execute(
        configuration: Configuration,
        devicePoolId: DevicePoolId,
        testBatch: TestBatch,
        deferred: CompletableDeferred<TestBatchResults>,
        progressReporter: ProgressReporter,
    ) {
        executeAction?.let {
            it(deferred)
            return
        }

        delay(testTimeMillis)

        if (crashWithTestBatchException) {
            throw TestBatchExecutionException("user requested the device to crash via crashWithTestBatchException parameter")
        }

        val results = testBatch.tests.map {
            val i = executionIndexMap.getOrDefault(it, 0)
            val result = executionResults.getValue(it)[i]
            executionIndexMap[it] = i + 1
            val testResult = TestResult(it, toDeviceInfo(), result, timeCounter, timeCounter + 1, testBatch.id)
            timeCounter += 1
            testResult
        }

        deferred.complete(
            TestBatchResults(
                batchId = testBatch.id,
                device = this,
                componentInfo = testBatch.componentInfo,
                finished = results.filter { it.status == TestStatus.PASSED },
                failed = results.filter { it.status == TestStatus.FAILURE },
                uncompleted = results.filter { it.status == TestStatus.INCOMPLETE },
            ),
        )
    }

    override suspend fun prepare(configuration: Configuration) {
        prepareCount += 1

        prepareAction?.let {
            it()
            return
        }

        delay(prepareTimeMillis)
    }
}

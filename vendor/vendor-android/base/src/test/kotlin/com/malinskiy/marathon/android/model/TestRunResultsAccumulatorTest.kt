package com.malinskiy.marathon.android.model

import com.malinskiy.marathon.android.AndroidComponentInfo
import com.malinskiy.marathon.time.Timer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import com.malinskiy.marathon.test.Test as MarathonTest

class TestRunResultsAccumulatorTest {

    private val componentInfo = AndroidComponentInfo(
        name = "component",
        applicationId = null,
        testApplicationId = "com.example.test",
        applicationOutput = null,
        testApplicationOutput = File("test.apk")
    )
    private val test1 = createTest("test1")
    private val test2 = createTest("test2")
    private val test3 = createTest("test3")

    private val timer = mock<Timer>()
    private val accumulator = TestRunResultsAccumulator(timer)

    @Test
    fun `initial state has no tests and a default run name`() {
        assertThat(accumulator.name).isEqualTo("not started")
        assertThat(accumulator.numTests).isEqualTo(0)
        assertThat(accumulator.isRunComplete).isFalse()
        assertThat(accumulator.isRunFailure).isFalse()
        assertThat(accumulator.getNumTestsInState(AndroidTestStatus.PASSED)).isEqualTo(0)
    }

    @Test
    fun `testRunStarted sets the run name and resets the failure state`() {
        accumulator.testRunStarted("first run", 1)
        accumulator.testRunFailed("boom")
        accumulator.testRunStarted("second run", 1)

        assertThat(accumulator.name).isEqualTo("second run")
        assertThat(accumulator.runFailureMessage).isNull()
        assertThat(accumulator.isRunFailure).isFalse()
        assertThat(accumulator.isRunComplete).isFalse()
    }

    @Test
    fun `passed test is completed with metrics and timings`() {
        whenever(timer.currentTimeMillis()).thenReturn(1000L, 2000L)

        accumulator.testRunStarted("run", 1)
        accumulator.testStarted(test1)
        accumulator.testEnded(test1, mapOf("metric" to "value"))
        accumulator.testRunEnded(100, emptyMap())

        val result = accumulator.testResults.getValue(test1)
        assertThat(result.status).isEqualTo(AndroidTestStatus.PASSED)
        assertThat(result.startTime).isEqualTo(1000)
        assertThat(result.endTime).isEqualTo(2000)
        assertThat(result.metrics).isEqualTo(mapOf("metric" to "value"))
        assertThat(accumulator.completedTests).containsExactly(test1)
        assertThat(accumulator.hasFailedTests()).isFalse()
        assertThat(accumulator.isRunComplete).isTrue()
        assertThat(accumulator.elapsedTime).isEqualTo(100)
    }

    @Test
    fun `failed test keeps the failure status after testEnded`() {
        accumulator.testRunStarted("run", 1)
        accumulator.testStarted(test1)
        accumulator.testFailed(test1, "trace")
        accumulator.testEnded(test1, emptyMap())
        accumulator.testRunEnded(100, emptyMap())

        val result = accumulator.testResults.getValue(test1)
        assertThat(result.status).isEqualTo(AndroidTestStatus.FAILURE)
        assertThat(result.stackTrace).isEqualTo("trace")
        assertThat(accumulator.numAllFailedTests).isEqualTo(1)
        assertThat(accumulator.hasFailedTests()).isTrue()
        assertThat(accumulator.completedTests).containsExactly(test1)
    }

    @Test
    fun `ignored test is completed and not counted as failed`() {
        accumulator.testRunStarted("run", 1)
        accumulator.testStarted(test1)
        accumulator.testIgnored(test1)
        accumulator.testEnded(test1, emptyMap())
        accumulator.testRunEnded(100, emptyMap())

        val result = accumulator.testResults.getValue(test1)
        assertThat(result.status).isEqualTo(AndroidTestStatus.IGNORED)
        assertThat(result.stackTrace).isNull()
        assertThat(accumulator.hasFailedTests()).isFalse()
        assertThat(accumulator.completedTests).containsExactly(test1)
    }

    @Test
    fun `assumption failure is completed and counted as failed`() {
        accumulator.testRunStarted("run", 1)
        accumulator.testStarted(test1)
        accumulator.testAssumptionFailure(test1, "assumption trace")
        accumulator.testEnded(test1, emptyMap())
        accumulator.testRunEnded(100, emptyMap())

        val result = accumulator.testResults.getValue(test1)
        assertThat(result.status).isEqualTo(AndroidTestStatus.ASSUMPTION_FAILURE)
        assertThat(result.stackTrace).isEqualTo("assumption trace")
        assertThat(accumulator.numAllFailedTests).isEqualTo(1)
        assertThat(accumulator.hasFailedTests()).isTrue()
    }

    @Test
    fun `test started but never ended remains incomplete`() {
        accumulator.testRunStarted("run", 2)
        accumulator.testStarted(test1)
        accumulator.testRunFailed("process crashed")

        val result = accumulator.testResults.getValue(test1)
        assertThat(result.status).isEqualTo(AndroidTestStatus.INCOMPLETE)
        assertThat(result.endTime).isEqualTo(0)
        assertThat(accumulator.completedTests).isEmpty()
        assertThat(accumulator.numCompleteTests).isEqualTo(0)
        assertThat(accumulator.isRunFailure).isTrue()
        assertThat(accumulator.runFailureMessage).isEqualTo("process crashed")
    }

    @Test
    fun `testEnded without testStarted marks the test as passed`() {
        accumulator.testRunStarted("run", 1)
        accumulator.testEnded(test1, emptyMap())

        val result = accumulator.testResults.getValue(test1)
        assertThat(result.status).isEqualTo(AndroidTestStatus.PASSED)
        assertThat(accumulator.numTests).isEqualTo(1)
    }

    @Test
    fun `testFailed without testStarted registers a failed test`() {
        accumulator.testRunStarted("run", 1)
        accumulator.testFailed(test1, "trace")

        val result = accumulator.testResults.getValue(test1)
        assertThat(result.status).isEqualTo(AndroidTestStatus.FAILURE)
        assertThat(result.stackTrace).isEqualTo("trace")
        assertThat(accumulator.numTests).isEqualTo(1)
    }

    @Test
    fun `duplicate testStarted resets the previous result`() {
        accumulator.testRunStarted("run", 1)
        accumulator.testStarted(test1)
        accumulator.testFailed(test1, "trace")
        accumulator.testStarted(test1)

        val result = accumulator.testResults.getValue(test1)
        assertThat(result.status).isEqualTo(AndroidTestStatus.INCOMPLETE)
        assertThat(result.stackTrace).isNull()
        assertThat(accumulator.numTests).isEqualTo(1)
    }

    @Test
    fun `testRunStopped completes the run`() {
        accumulator.testRunStarted("run", 1)
        accumulator.testRunStopped(100)

        assertThat(accumulator.isRunComplete).isTrue()
        assertThat(accumulator.elapsedTime).isEqualTo(100)
    }

    @Test
    fun `elapsed time accumulates across testRunStopped and testRunEnded`() {
        accumulator.testRunStarted("run", 1)
        accumulator.testRunStopped(100)
        accumulator.testRunEnded(50, emptyMap())

        assertThat(accumulator.elapsedTime).isEqualTo(150)
        assertThat(accumulator.isRunComplete).isTrue()
    }

    @Test
    fun `testRunEnded without metric aggregation keeps the latest reported metrics`() {
        accumulator.testRunStarted("run", 1)
        accumulator.testRunEnded(100, mapOf("long" to "10", "text" to "abc"))
        accumulator.testRunEnded(50, mapOf("long" to "20"))

        assertThat(accumulator.runMetrics).isEqualTo(mapOf("long" to "20", "text" to "abc"))
    }

    @Test
    fun `testRunEnded with metric aggregation stores the first run metrics as reported`() {
        accumulator.aggregateMetrics = true

        accumulator.testRunStarted("run", 1)
        accumulator.testRunEnded(100, mapOf("long" to "10", "double" to "1.5", "text" to "abc"))

        assertThat(accumulator.runMetrics).isEqualTo(mapOf("long" to "10", "double" to "1.5", "text" to "abc"))
    }

    @Test
    fun `testRunEnded with metric aggregation sums numeric metrics across runs`() {
        accumulator.aggregateMetrics = true

        accumulator.testRunStarted("run", 1)
        accumulator.testRunEnded(100, mapOf("long" to "10", "double" to "1.5", "text" to "abc"))
        accumulator.testRunEnded(50, mapOf("long" to "20", "double" to "2.5", "text" to "def"))

        assertThat(accumulator.runMetrics).isEqualTo(mapOf("long" to "30", "double" to "4.0", "text" to "def"))
        assertThat(accumulator.isRunComplete).isTrue()
        assertThat(accumulator.elapsedTime).isEqualTo(150)
    }

    @Test
    fun `testRunEnded with metric aggregation combines long and double values for the same key`() {
        accumulator.aggregateMetrics = true

        accumulator.testRunStarted("run", 1)
        accumulator.testRunEnded(100, mapOf("metric" to "10"))
        accumulator.testRunEnded(50, mapOf("metric" to "1.5"))

        assertThat(accumulator.runMetrics).isEqualTo(mapOf("metric" to "11.5"))
    }

    @Test
    fun `testRunEnded with metric aggregation keeps keys missing from a later run`() {
        accumulator.aggregateMetrics = true

        accumulator.testRunStarted("run", 1)
        accumulator.testRunEnded(100, mapOf("long" to "10", "double" to "1.5"))
        accumulator.testRunEnded(50, mapOf("long" to "20"))

        assertThat(accumulator.runMetrics).isEqualTo(mapOf("long" to "30", "double" to "1.5"))
    }

    @Test
    fun `getNumTestsInState counts tests per status`() {
        accumulator.testRunStarted("run", 3)
        accumulator.testStarted(test1)
        accumulator.testEnded(test1, emptyMap())
        accumulator.testStarted(test2)
        accumulator.testFailed(test2, "trace")
        accumulator.testEnded(test2, emptyMap())
        accumulator.testStarted(test3)
        accumulator.testRunFailed("crashed")

        assertThat(accumulator.getNumTestsInState(AndroidTestStatus.PASSED)).isEqualTo(1)
        assertThat(accumulator.getNumTestsInState(AndroidTestStatus.FAILURE)).isEqualTo(1)
        assertThat(accumulator.getNumTestsInState(AndroidTestStatus.INCOMPLETE)).isEqualTo(1)
        assertThat(accumulator.numTests).isEqualTo(3)
        assertThat(accumulator.numCompleteTests).isEqualTo(2)
        assertThat(accumulator.completedTests).containsExactly(test1, test2)
    }

    private fun createTest(method: String): MarathonTest = MarathonTest(
        pkg = "com.example",
        clazz = "SimpleTest",
        method = method,
        metaProperties = emptyList(),
        componentInfo = componentInfo
    )
}

package com.malinskiy.marathon.report.summary

import com.malinskiy.marathon.createDeviceInfo
import com.malinskiy.marathon.execution.ComponentInfo
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.test.TestComponentInfo
import org.amshove.kluent.shouldBeEqualTo
import org.junit.jupiter.api.Test
import com.malinskiy.marathon.test.Test as MarathonTest

class TestSummaryFormatterTest {

    private val formatter = TestSummaryFormatter()

    @Test
    fun `format test result summary - 2 batches and 3 tests (test1 - test2 - test3, test1 - test2 - test3)`() {
        val tests = listOf(
            "test1" to "batch1" to TestStatus.PASSED,
            "test1" to "batch2" to TestStatus.FAILURE,
            "test2" to "batch1" to TestStatus.PASSED,
            "test2" to "batch2" to TestStatus.PASSED,
            "test3" to "batch1" to TestStatus.FAILURE,
            "test3" to "batch2" to TestStatus.FAILURE
        )
        val summary = createTestSummary(tests, forTest = "test1")
        val currentResult = summary.results.first()

        val formattedSummary = formatter.formatTestResultSummary(
            currentResult,
            summary
        )

        formattedSummary shouldBeEqualTo """
            Test status: PASSED
            Strict run: false
            From cache: false
            
            There were 2 runs:
              1) PASSED (serial-number, batch: batch1) - current
              2) FAILURE (serial-number, batch: batch2)
            
            Test runs details:
            ================================================================================
              1) PASSED in batch #batch1 (3 tests in batch, device: serial-number)
            ================================================================================
            Tests in the batch (executed in the same process):
              > Test.test1 (PASSED)
              * Test.test2 (PASSED)
              * Test.test3 (FAILURE)
            
            Copy JSON to run this batch locally:
            {"id":"batch1","component":":app","tests":[{"pkg":"com.test","clazz":"Test","method":"test1"},{"pkg":"com.test","clazz":"Test","method":"test2"},{"pkg":"com.test","clazz":"Test","method":"test3"}]}
            
            
            ================================================================================
              2) FAILURE in batch #batch2 (3 tests in batch, device: serial-number)
            ================================================================================
            Tests in the batch (executed in the same process):
              > Test.test1 (FAILURE)
              * Test.test2 (PASSED)
              * Test.test3 (FAILURE)
            
            Copy JSON to run this batch locally:
            {"id":"batch2","component":":app","tests":[{"pkg":"com.test","clazz":"Test","method":"test1"},{"pkg":"com.test","clazz":"Test","method":"test2"},{"pkg":"com.test","clazz":"Test","method":"test3"}]}
            
            
            
""".trimIndent()
    }

    private fun createTestSummary(tests: List<Pair<Pair<String, String>, TestStatus>>, forTest: String): TestSummary {
        val testResults = mutableListOf<TestResult>()

        tests.forEach { (testInfo, testStatus) ->
            val (testName, batchId) = testInfo
            testResults.add(createTestResult(createTest(method = testName), batchId = batchId, status = testStatus))
        }

        val batches = testResults
            .groupBy { it.batchId }
            .values
            .map { createBatch(it.first().batchId, it) }

        val test = createTest(method = forTest)

        val results = testResults.filter { it.test == test }

        return TestSummary(test, results, batches)
    }

    private fun createBatch(
        batchId: String = "abc",
        testResults: List<TestResult> = emptyList()
    ): Batch = Batch(batchId, testResults)

    private fun createTestResult(test: MarathonTest, batchId: String = "abc", status: TestStatus = TestStatus.PASSED) = TestResult(
        test = test,
        device = createDeviceInfo(serialNumber = "serial-number"),
        status = status,
        startTime = 123,
        endTime = 456,
        attachments = emptyList(),
        batchId = batchId
    )

    private fun createTest(method: String, component: ComponentInfo = TestComponentInfo(name = ":app:release")) = MarathonTest(
        pkg = "com.test",
        clazz = "Test",
        method = method,
        componentInfo = component,
        metaProperties = emptyList()
    )
}

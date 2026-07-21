package com.malinskiy.marathon.android.model

import com.malinskiy.marathon.android.executor.listeners.TestRunListener
import com.malinskiy.marathon.log.MarathonLogging
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.toSimpleSafeTestName
import com.malinskiy.marathon.time.Timer

/**
 * Holds results from a single test run.
 * Maintains an accurate count of tests, and tracks incomplete tests.
 *
 * Not thread safe! The test* callbacks must be called in order
 */
class TestRunResultsAccumulator(private val timer: Timer) : TestRunListener {

    private val logger = MarathonLogging.getLogger(TestRunResultsAccumulator::class.java)

    var name: String = "not started"
        private set
    val testResults = LinkedHashMap<Test, AndroidTestResult>()
    internal val runMetrics = HashMap<String, String>()
    var isRunComplete = false
    var isCountDirty = false
    var elapsedTime: Long = 0
        private set

    private var statusCounts: Map<AndroidTestStatus, Int> = mutableMapOf()

    /**
     * Return the run failure error message, `null` if run did not fail.
     */
    var runFailureMessage: String? = null
        private set

    var aggregateMetrics = false

    val completedTests: Set<Test>
        get() {
            val completedTests = LinkedHashSet<Test>()
            for ((key, value) in testResults) {
                if (value.status != AndroidTestStatus.INCOMPLETE) {
                    completedTests.add(key)
                }
            }
            return completedTests
        }

    val isRunFailure: Boolean
        get() = runFailureMessage != null

    val numTests: Int
        get() = testResults.size

    val numCompleteTests: Int
        get() = numTests - getNumTestsInState(AndroidTestStatus.INCOMPLETE)

    /**
     * Return total number of tests in a failure state (failed, assumption failure)
     */
    val numAllFailedTests: Int
        get() = getNumTestsInState(AndroidTestStatus.FAILURE) + getNumTestsInState(AndroidTestStatus.ASSUMPTION_FAILURE)

    /**
     * Gets the number of tests in given state for this run.
     */
    fun getNumTestsInState(status: AndroidTestStatus): Int {
        if (isCountDirty) {
            statusCounts = testResults.values.groupingBy { it.status }.eachCount()
        }

        return statusCounts[status] ?: 0
    }

    /**
     * @return `true` if test run had any failed or error tests.
     */
    fun hasFailedTests(): Boolean =
        numAllFailedTests > 0

    override fun testRunStarted(runName: String, testCount: Int) {
        name = runName
        isRunComplete = false
        runFailureMessage = null
    }

    override fun testStarted(test: Test) {
        testStarted(test, timer.currentTimeMillis())
    }

    private fun testStarted(test: Test, startTime: Long) {
        addTestResult(test, AndroidTestResult(startTime = startTime))
    }

    private fun addTestResult(test: Test, testResult: AndroidTestResult) {
        isCountDirty = true
        testResults[test] = testResult
    }

    private fun updateTestResult(test: Test, status: AndroidTestStatus, trace: String?) {
        val result = testResults[test] ?: run {
            logger.debug("Received test event without test start for {}", test.toSimpleSafeTestName())
            AndroidTestResult(startTime = timer.currentTimeMillis())
        }
        addTestResult(test, result.copy(status = status, stackTrace = trace))
    }

    override fun testFailed(test: Test, trace: String) {
        updateTestResult(test, AndroidTestStatus.FAILURE, trace)
    }

    override fun testAssumptionFailure(test: Test, trace: String) {
        updateTestResult(test, AndroidTestStatus.ASSUMPTION_FAILURE, trace)
    }

    override fun testIgnored(test: Test) {
        updateTestResult(test, AndroidTestStatus.IGNORED, null)
    }

    override fun testEnded(test: Test, testMetrics: Map<String, String>) {
        testEnded(test, timer.currentTimeMillis(), testMetrics)
    }

    private fun testEnded(test: Test, endTime: Long, testMetrics: Map<String, String>) {
        val result = testResults[test] ?: AndroidTestResult(startTime = timer.currentTimeMillis())
        val status = if (result.status == AndroidTestStatus.INCOMPLETE) AndroidTestStatus.PASSED else result.status
        addTestResult(test, result.copy(status = status, endTime = endTime, metrics = testMetrics))
    }

    override fun testRunFailed(errorMessage: String) {
        runFailureMessage = errorMessage
    }

    override fun testRunStopped(elapsedTime: Long) {
        this.elapsedTime += elapsedTime
        isRunComplete = true
    }

    override fun testRunEnded(elapsedTime: Long, runMetrics: Map<String, String>) {
        if (aggregateMetrics) {
            for ((key, value) in runMetrics) {
                combineValues(this.runMetrics[key], value)?.let {
                    this.runMetrics[key] = it
                }
            }
        } else {
            this.runMetrics.putAll(runMetrics)
        }
        this.elapsedTime += elapsedTime
        isRunComplete = true
    }

    /**
     * Combine old and new metrics value
     *
     * @param existingValue
     * @param newValue
     * @return the combination of the two string as Long or Double value.
     */
    private fun combineValues(existingValue: String?, newValue: String): String? {
        if (existingValue != null) {
            try {
                val existingLong = java.lang.Long.parseLong(existingValue)
                val newLong = java.lang.Long.parseLong(newValue)
                return java.lang.Long.toString(existingLong + newLong)
            } catch (e: NumberFormatException) {
                // not a long, skip to next
            }

            try {
                val existingDouble = java.lang.Double.parseDouble(existingValue)
                val newDouble = java.lang.Double.parseDouble(newValue)
                return java.lang.Double.toString(existingDouble + newDouble)
            } catch (e: NumberFormatException) {
                // not a double either, fall through
            }
        }
        // default to overriding existingValue
        return newValue
    }
}

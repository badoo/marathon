package com.malinskiy.marathon

import com.malinskiy.marathon.analytics.external.Analytics
import com.malinskiy.marathon.analytics.external.NoOpMetricsProvider
import com.malinskiy.marathon.analytics.internal.sub.TrackerInternal
import com.malinskiy.marathon.exceptions.ReportGenerationException
import com.malinskiy.marathon.execution.Scheduler
import com.malinskiy.marathon.execution.progress.ProgressReporter
import com.malinskiy.marathon.test.factory.configuration
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File

class MarathonTest {
    private val configuration = configuration()
    private val scheduler = mock<Scheduler>()
    private val tracker = mock<TrackerInternal>()
    private val marathon = Marathon(
        configuration = configuration,
        tracker = tracker,
        analytics = Analytics(NoOpMetricsProvider()),
        testParser = mock(),
        progressReporter = ProgressReporter(configuration),
        scheduler = scheduler
    )

    @Test
    fun `GIVEN report generation fails WHEN stopping THEN throws ReportGenerationException`() = runTest {
        whenever(tracker.finish()).thenThrow(RuntimeException("Simulated report failure"))

        assertThrows<ReportGenerationException> {
            marathon.stopAndWaitForCompletion()
        }
    }

    @Test
    fun `GIVEN calling coroutine is cancelled WHEN report generation fails THEN rethrows the cancellation`() = runTest {
        whenever(tracker.finish()).thenThrow(RuntimeException("Simulated report failure"))
        var thrown: Throwable? = null

        val job = launch {
            cancel()
            try {
                marathon.stopAndWaitForCompletion()
            } catch (e: Throwable) {
                thrown = e
                throw e
            }
        }
        job.join()

        assertThat(job.isCancelled).isTrue()
        assertThat(thrown).isInstanceOf(CancellationException::class.java)
        assertThat(thrown).isNotInstanceOf(ReportGenerationException::class.java)
    }

    @Test
    fun `GIVEN test run completes WHEN stopping THEN deletes the temp directory`() = runTest {
        givenTempDirWithContent()

        marathon.stopAndWaitForCompletion()

        assertThat(configuration.tempDir).doesNotExist()
    }

    @Test
    fun `GIVEN report generation fails WHEN stopping THEN deletes the temp directory`() = runTest {
        whenever(tracker.finish()).thenThrow(RuntimeException("Simulated report failure"))
        givenTempDirWithContent()

        assertThrows<ReportGenerationException> {
            marathon.stopAndWaitForCompletion()
        }

        assertThat(configuration.tempDir).doesNotExist()
    }

    @Test
    fun `GIVEN calling coroutine is cancelled WHEN stopping THEN leaves the temp directory untouched`() = runTest {
        whenever(tracker.finish()).thenThrow(RuntimeException("Simulated report failure"))
        givenTempDirWithContent()

        val job = launch {
            cancel()
            marathon.stopAndWaitForCompletion()
        }
        job.join()

        assertThat(File(configuration.tempDir, "leftover.tmp")).exists()
    }

    private fun givenTempDirWithContent() {
        configuration.tempDir.mkdirs()
        File(configuration.tempDir, "leftover.tmp").writeText("leftover")
    }
}

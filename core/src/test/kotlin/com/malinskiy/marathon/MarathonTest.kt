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
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock

class MarathonTest {
    private val configuration = configuration()
    private val scheduler = mock<Scheduler>()
    private val tracker = mock<TrackerInternal> {
        on { finish() } doThrow RuntimeException("Simulated report failure")
    }
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
        assertThrows<ReportGenerationException> {
            marathon.stopAndWaitForCompletion()
        }
    }

    @Test
    fun `GIVEN calling coroutine is cancelled WHEN report generation fails THEN rethrows the cancellation`() = runTest {
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
}

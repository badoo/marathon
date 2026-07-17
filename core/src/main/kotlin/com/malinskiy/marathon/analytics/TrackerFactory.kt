package com.malinskiy.marathon.analytics

import com.google.gson.Gson
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.analytics.internal.sub.DelegatingTrackerInternal
import com.malinskiy.marathon.analytics.internal.sub.ExecutionReportGenerator
import com.malinskiy.marathon.analytics.internal.sub.TrackerInternal
import com.malinskiy.marathon.cache.test.CacheTestResultsTracker
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.io.AttachmentManager
import com.malinskiy.marathon.io.FileManager
import com.malinskiy.marathon.io.FileType
import com.malinskiy.marathon.report.allure.AllureReporter
import com.malinskiy.marathon.report.attachment.AttachmentTestEventInflator
import com.malinskiy.marathon.report.device.DeviceInfoJsonReporter
import com.malinskiy.marathon.report.html.HtmlSummaryReporter
import com.malinskiy.marathon.report.junit.FinalJUnitReporter
import com.malinskiy.marathon.report.junit.JUnitReporter
import com.malinskiy.marathon.report.junit.JUnitWriter
import com.malinskiy.marathon.report.listener.ListenerReporter
import com.malinskiy.marathon.report.logs.LogReportTestEventInflator
import com.malinskiy.marathon.report.logs.LogsProvider
import com.malinskiy.marathon.report.raw.RawJsonReporter
import com.malinskiy.marathon.report.stdout.StdoutReporter
import com.malinskiy.marathon.report.summary.TestSummaryFormatter
import com.malinskiy.marathon.report.test.TestJsonReporter
import com.malinskiy.marathon.report.timeline.TimelineReporter
import com.malinskiy.marathon.report.timeline.TimelineSummaryProvider
import com.malinskiy.marathon.report.trace.TraceReporter
import com.malinskiy.marathon.time.Timer
import java.io.File

internal class TrackerFactory(
    private val configuration: Configuration,
    private val fileManager: FileManager,
    private val attachmentManager: AttachmentManager,
    private val cacheTestResultsTracker: CacheTestResultsTracker,
    private val logsProvider: LogsProvider,
    private val gson: Gson,
    private val timer: Timer,
    private val track: Track
) {
    fun create(): TrackerInternal {
        val defaultTrackers = mutableListOf<TrackerInternal>(createExecutionReportGenerator())

        val delegatingTrackerInternal = DelegatingTrackerInternal(defaultTrackers)
        val mappingTracker = MappingTracker(delegatingTrackerInternal)

        track + mappingTracker
        if (configuration.cache.isPushEnabled) {
            track + cacheTestResultsTracker
        }
        configuration.analyticsTracker?.let { track + it }

        return delegatingTrackerInternal
    }

    private fun createExecutionReportGenerator(): ExecutionReportGenerator {
        val testResultDescriptionFactory = TestSummaryFormatter()
        val testEventInflators = listOf(
            LogReportTestEventInflator(logsProvider),
            AttachmentTestEventInflator(attachmentManager)
        )

        return ExecutionReportGenerator(
            reporters = listOfNotNull(
                DeviceInfoJsonReporter(fileManager, gson),
                JUnitReporter(JUnitWriter(fileManager, FileType.TEST, testResultDescriptionFactory)),
                FinalJUnitReporter(JUnitWriter(fileManager, FileType.TEST_FINAL, testResultDescriptionFactory)),
                TimelineReporter(TimelineSummaryProvider(), gson, configuration.outputDir),
                TraceReporter(configuration.outputDir),
                RawJsonReporter(fileManager, gson),
                TestJsonReporter(fileManager, gson),
                AllureReporter(configuration, File(configuration.outputDir, "allure-results"), testResultDescriptionFactory, configuration.testOwnerProvider),
                HtmlSummaryReporter(gson, configuration.outputDir, testResultDescriptionFactory),
                StdoutReporter(timer),
                configuration.listener?.let { ListenerReporter(it) }
            ),
            testEventInflators = testEventInflators
        )
    }
}

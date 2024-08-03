package com.malinskiy.marathon.report.timeline

import com.google.gson.Gson
import com.malinskiy.marathon.analytics.internal.sub.ExecutionReport
import com.malinskiy.marathon.report.Reporter
import java.io.File
import java.io.InputStream

internal class TimelineReporter(
    private val provider: TimelineSummaryProvider,
    private val gson: Gson,
    private val rootOutput: File
) : Reporter {

    override fun generate(executionReport: ExecutionReport) {
        val htmlDir = File(rootOutput, "/html")
        htmlDir.mkdirs()
        val timelineDir = File(htmlDir, "/timeline")
        timelineDir.mkdirs()
        val indexHtmlFile = File(timelineDir, "index.html")

        val chartCss = File(timelineDir, "chart.css")
        inputStreamFromResources("timeline/chart.css").use { input ->
            chartCss.outputStream().use { input.copyTo(it) }
        }

        val chartJs = File(timelineDir, "chart.js")
        inputStreamFromResources("timeline/chart.js").use { input ->
            chartJs.outputStream().use { input.copyTo(it) }
        }

        val json = gson.toJson(provider.generate(executionReport))
        inputStreamFromResources("timeline/index.html").use { input ->
            val indexText = input.reader().readText()
            indexHtmlFile.writeText(indexText.replace("\${dataset}", json))
        }
    }

    private fun inputStreamFromResources(path: String): InputStream =
        TimelineExecutionResult::class.java.classLoader.getResourceAsStream(path)
}

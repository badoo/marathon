package com.malinskiy.marathon.report.allure

import com.github.automatedowl.tools.AllureEnvironmentWriter.allureEnvironmentWriter
import com.google.common.collect.ImmutableMap
import com.malinskiy.marathon.analytics.internal.sub.ExecutionReport
import com.malinskiy.marathon.device.DeviceInfo
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.extension.relativePathTo
import com.malinskiy.marathon.report.Reporter
import com.malinskiy.marathon.report.summary.TestSummary
import com.malinskiy.marathon.report.summary.TestSummaryFormatter
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.toSimpleSafeTestName
import io.qameta.allure.AllureLifecycle
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.FileSystemResultsWriter
import io.qameta.allure.Issue
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Story
import io.qameta.allure.TmsLink
import io.qameta.allure.model.Attachment
import io.qameta.allure.model.Label
import io.qameta.allure.model.Status
import io.qameta.allure.model.StatusDetails
import io.qameta.allure.util.ResultsUtils
import java.io.File
import java.util.*

class AllureReporter(
    val configuration: Configuration,
    private val outputDirectory: File,
    private val testSummaryFormatter: TestSummaryFormatter
) : Reporter {

    private val lifecycle: AllureLifecycle by lazy { AllureLifecycle(FileSystemResultsWriter(outputDirectory.toPath())) }

    override fun generate(executionReport: ExecutionReport) {
        outputDirectory.mkdirs()

        val summaries: Map<Test, TestSummary> = executionReport.testSummaries

        executionReport.testEvents.forEach { testEvent ->
            val uuid = UUID.randomUUID().toString()
            val summary = summaries[testEvent.testResult.test]
            val allureResults = createTestResult(uuid, testEvent.device, testEvent.testResult, summary)
            lifecycle.scheduleTestCase(uuid, allureResults)
            lifecycle.writeTestCase(uuid)
        }

        val params = configuration.toMap()
        val builder = ImmutableMap.builder<String, String>()
        params.forEach {
            builder.put(it.key, it.value)
        }

        allureEnvironmentWriter(
            builder.build(), outputDirectory.absolutePath + File.separator
        )
    }

    private fun createTestResult(
        uuid: String,
        device: DeviceInfo,
        testResult: TestResult,
        summary: TestSummary?
    ): io.qameta.allure.model.TestResult {
        val test = testResult.test
        val fullName = if (summary?.isFlaky == true) {
            // TODO: remove this when flaky reporting will be fixed (https://github.com/allure-framework/allure2/pull/1135)
            "[flaky] " + test.toSimpleSafeTestName()
        } else {
            test.toSimpleSafeTestName()
        }
        val suite = "${test.pkg}.${test.clazz}"

        val status: Status =
            when (testResult.status) {
                TestStatus.FAILURE -> Status.FAILED
                TestStatus.PASSED -> Status.PASSED
                TestStatus.INCOMPLETE -> Status.BROKEN
                TestStatus.ASSUMPTION_FAILURE -> Status.SKIPPED
                TestStatus.IGNORED -> Status.SKIPPED
            }

        val summaryFile = outputDirectory
            .resolve("$uuid-summary.log")
            .apply { writeText(testSummaryFormatter.formatTestResultSummary(testResult, summary)) }

        val summaryAttachment = Attachment()
            .setName("Summary")
            .setSource(summaryFile.relativePathTo(outputDirectory))
            .setType("text/plain")

        val testAttachments: List<Attachment> = testResult
            .attachments
            .map {
                Attachment()
                    .setName(it.type.name.toLowerCase().capitalize())
                    .setSource(it.file.relativePathTo(outputDirectory))
                    .setType(it.type.toMimeType())
            }

        val allAttachments = listOf(summaryAttachment) + testAttachments

        val allureTestResult = io.qameta.allure.model.TestResult()
            .setUuid(uuid)
            .setFullName(fullName)
            .setHistoryId(getHistoryId(test))
            .setStatus(status)
            .setStart(testResult.startTime)
            .setStop(testResult.endTime)
            .setAttachments(allAttachments)
            .setParameters()
            .setLabels(
                ResultsUtils.createHostLabel().setValue(device.serialNumber),
                ResultsUtils.createPackageLabel(test.pkg),
                ResultsUtils.createTestClassLabel(test.clazz),
                ResultsUtils.createTestMethodLabel(test.method),
                ResultsUtils.createSuiteLabel(suite)
            )

        val shortStacktrace = testResult.stacktrace?.lines()?.take(MESSAGE_LINES_COUNT)?.joinToString(separator = "\n")
        val isFlaky = summary?.isFlaky ?: false

        allureTestResult.statusDetails = StatusDetails()
            .setMessage(shortStacktrace)
            .setFlaky(isFlaky)
            .setTrace(testResult.stacktrace)

        test.findValue<String>(Description::class.java.canonicalName)?.let { allureTestResult.setDescription(it) }
        test.findValue<String>(Issue::class.java.canonicalName)?.let { allureTestResult.links.add(it.toLink()) }
        test.findValue<String>(TmsLink::class.java.canonicalName)?.let { allureTestResult.links.add(it.toLink()) }

        allureTestResult.labels.addAll(test.getOptionalLabels())

        return allureTestResult
    }

    private fun getHistoryId(test: Test) =
        ResultsUtils.generateMethodSignatureHash(test.clazz, test.method, emptyList())

    private fun Test.getOptionalLabels(): Collection<Label> {
        val list = mutableListOf<Label>()

        findValue<String>(Epic::class.java.canonicalName)?.let { list.add(ResultsUtils.createEpicLabel(it)) }
        findValue<String>(Feature::class.java.canonicalName)?.let { list.add(ResultsUtils.createFeatureLabel(it)) }
        findValue<String>(Story::class.java.canonicalName)?.let { list.add(ResultsUtils.createStoryLabel(it)) }
        findValue<SeverityLevel>(Severity::class.java.canonicalName)?.let { list.add(ResultsUtils.createSeverityLabel(it)) }
        findValue<String>(Owner::class.java.canonicalName)?.let { list.add(ResultsUtils.createOwnerLabel(it)) }

        return list
    }

    private fun String.toLink(): io.qameta.allure.model.Link {
        return io.qameta.allure.model.Link().also {
            it.name = "Issue"
            it.url = this
        }
    }

    private inline fun <reified T> Test.findValue(name: String): T? {
        metaProperties.find { it.name == name }?.let { property ->
            return property.values["value"] as? T
        }

        return null
    }

    private companion object {
        private const val MESSAGE_LINES_COUNT = 3
    }
}

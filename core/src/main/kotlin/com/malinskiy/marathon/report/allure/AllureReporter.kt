package com.malinskiy.marathon.report.allure

import com.malinskiy.marathon.analytics.internal.sub.ExecutionReport
import com.malinskiy.marathon.device.DeviceInfo
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.TestOwnerProvider
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.extension.relativePathTo
import com.malinskiy.marathon.report.Reporter
import com.malinskiy.marathon.report.summary.TestSummary
import com.malinskiy.marathon.report.summary.TestSummaryFormatter
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.toSafeTestName
import io.qameta.allure.AllureLifecycle
import io.qameta.allure.Description
import io.qameta.allure.Epic
import io.qameta.allure.Feature
import io.qameta.allure.Features
import io.qameta.allure.FileSystemResultsWriter
import io.qameta.allure.Issue
import io.qameta.allure.Lead
import io.qameta.allure.Owner
import io.qameta.allure.Severity
import io.qameta.allure.SeverityLevel
import io.qameta.allure.Stories
import io.qameta.allure.Story
import io.qameta.allure.TmsLink
import io.qameta.allure.model.Attachment
import io.qameta.allure.model.Label
import io.qameta.allure.model.Status
import io.qameta.allure.model.StatusDetails
import io.qameta.allure.util.ResultsUtils
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.util.Properties
import java.util.UUID

class AllureReporter(
    val configuration: Configuration,
    private val outputDirectory: File,
    private val testSummaryFormatter: TestSummaryFormatter,
    private val testOwnerProvider: TestOwnerProvider?
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

        val params = configuration.toMap().toMutableMap()
        params.forEach {
            params[it.key] = it.value
        }
        params[PLATFORM] = ANDROID
        params.saveToEnvironmentProperties()
    }

    private fun Map<String, String>.saveToEnvironmentProperties() {
        val propertiesFile = Properties()
        this.forEach { propertiesFile.setProperty(it.key, it.value) }
        FileOutputStream(outputDirectory.absolutePath + File.separator + "environment.properties").use {
            propertiesFile.store(it, null)
        }
    }

    @Suppress("LongMethod")
    private fun createTestResult(
        uuid: String,
        device: DeviceInfo,
        testResult: TestResult,
        summary: TestSummary?
    ): io.qameta.allure.model.TestResult {
        val test = testResult.test
        val fullName = test.toSafeTestName()
        val testMethodName = test.method
        val suite = "${test.pkg}.${test.clazz}"

        val status: Status =
            when (testResult.status) {
                TestStatus.FAILURE -> Status.FAILED
                TestStatus.PASSED -> Status.PASSED
                TestStatus.INCOMPLETE -> Status.BROKEN
                TestStatus.ASSUMPTION_FAILURE -> Status.SKIPPED
                TestStatus.IGNORED -> Status.SKIPPED
            }

        testResult.attachments.forEach {
            val linkFile = outputDirectory.resolve(it.file.name).toPath()
            Files.deleteIfExists(linkFile)
            Files.createSymbolicLink(linkFile, it.file.toPath())
        }

        val testAttachments: MutableList<Attachment> = testResult
            .attachments
            .map {
                Attachment()
                    .setName(it.type.name.lowercase().replaceFirstChar(Char::titlecase))
                    .setSource(it.file.name)
                    .setType(it.type.toMimeType())
            }.toMutableList()

        attachSummary(summary, uuid, testResult, testAttachments)

        val allureTestResult = io.qameta.allure.model.TestResult()
            .setUuid(uuid)
            .setFullName(fullName)
            .setName(testMethodName)
            .setHistoryId(getHistoryId(test))
            .setTestCaseId(fullName)
            .setTestCaseName(testMethodName)
            .setStatus(status)
            .setStart(testResult.startTime)
            .setStop(testResult.endTime)
            .setAttachments(testAttachments)
            .setParameters(emptyList())
            .setLabels(
                mutableListOf(
                    ResultsUtils.createHostLabel().setValue(device.serialNumber),
                    ResultsUtils.createPackageLabel(test.pkg),
                    ResultsUtils.createTestClassLabel(suite),
                    ResultsUtils.createTestMethodLabel(test.method),
                    ResultsUtils.createSuiteLabel(suite)
                )
            )

        val shortStacktrace = testResult.stacktrace?.lines()?.take(MESSAGE_LINES_COUNT)?.joinToString(separator = "\n")
        val isFlaky = summary?.isFlaky ?: false

        allureTestResult.statusDetails = StatusDetails()
            .setMessage(shortStacktrace)
            .setFlaky(isFlaky)
            .setTrace(testResult.stacktrace)

        test.findValue<String>(Description::class.java.canonicalName)?.let { allureTestResult.setDescription(it) }
        test.findValue<String>(Issue::class.java.canonicalName)?.let { allureTestResult.links.add(ResultsUtils.createIssueLink(it)) }
        test.findValue<String>(TmsLink::class.java.canonicalName)?.let { allureTestResult.links.add(ResultsUtils.createTmsLink(it)) }
        allureTestResult.labels.add(ResultsUtils.createLabel(PLATFORM, ANDROID))
        allureTestResult.labels.addAll(ResultsUtils.getProvidedLabels())
        allureTestResult.labels.addAll(test.getOptionalLabels())

        return allureTestResult
    }

    private fun attachSummary(
        summary: TestSummary?,
        uuid: String,
        testResult: TestResult,
        testAttachments: MutableList<Attachment>
    ) {
        if (summary != null && summary.results.any { it.isFailedOrBroken }) {

            // We must add summary file to Allure only if we had something failed or broken
            // If everything has been passed or ignored summary won't give us anything

            val summaryFile = outputDirectory
                .resolve("$uuid-summary.log")
                .apply { writeText(testSummaryFormatter.formatTestResultSummary(testResult, summary)) }

            val summaryAttachment = Attachment()
                .setName("Summary")
                .setSource(summaryFile.relativePathTo(outputDirectory))
                .setType("text/plain")

            testAttachments += summaryAttachment
        }
    }

    private fun Test.isApplicationTest(): Boolean =
        configuration.appModuleRegexes.any { it.matches(componentInfo.name) }

    private fun getHistoryId(test: Test): String =
        ResultsUtils.generateMethodSignatureHash(test.clazz, test.method, emptyList())

    private fun Test.getOptionalLabels(): Collection<Label> {
        val list = mutableListOf<Label>()

        findValue<String>(Epic::class.java.canonicalName)?.let { list.add(ResultsUtils.createEpicLabel(it)) }
        findArrayOf<String>(Features::class.java.canonicalName).let { features ->
            features.forEach { list.add(ResultsUtils.createFeatureLabel(it)) }
        }
        findValue<String>(Feature::class.java.canonicalName)?.let { list.add(ResultsUtils.createFeatureLabel(it)) }
        findArrayOf<String>(Stories::class.java.canonicalName).let { stories ->
            stories.forEach { list.add(ResultsUtils.createStoryLabel(it)) }
        }
        findValue<String>(Story::class.java.canonicalName)?.let { list.add(ResultsUtils.createStoryLabel(it)) }
        findValue<SeverityLevel>(Severity::class.java.canonicalName)?.let { list.add(ResultsUtils.createSeverityLabel(it)) }
        findValue<String>(Owner::class.java.canonicalName)?.let { list.add(ResultsUtils.createOwnerLabel(it)) }
        findValue<String>(Lead::class.java.canonicalName)?.let { list.add(ResultsUtils.createLabel(ResultsUtils.LEAD_LABEL_NAME, it)) }
        findValue<String>("io.qameta.allure.junit4.Tag")?.let { list.add(ResultsUtils.createTagLabel(it)) }
        findValue<String>("io.qameta.allure.label.TestPlanEntry")?.let { list.add(ResultsUtils.createLabel(TEST_PLAN_ENTRY, it)) }
        findValue<String>("io.qameta.allure.label.Layer")
            ?.let { list.add(ResultsUtils.createLabel(LAYER, it)) }
            ?: list.add(
                ResultsUtils.createLabel(
                    LAYER, if (isApplicationTest()) CLIENT_APPLICATION else CLIENT_COMPONENT
                )
            )
        val annotatedTeam = findValue<String>("io.qameta.allure.label.Team")
        testOwnerProvider?.getTestOwner(this)?.let { testOwner ->
            list.addTeam(testOwner.team, annotatedTeam)
            list.add(ResultsUtils.createLabel(COMPONENT, testOwner.component))
        } ?: { list.addTeam(null, annotatedTeam) }
        return list
    }

    private fun MutableList<Label>.addTeam(technoMancerTeam: String?, annotatedTeam: String?) {
        (technoMancerTeam ?: annotatedTeam)?.let {
            add(ResultsUtils.createLabel(TEAM, it))
        }
    }

    private inline fun <reified T> Test.findArrayOf(name: String): Array<T> {
        metaProperties.find { it.name == name }?.let { property ->
            return property.values["value"] as? Array<T> ?: emptyArray()
        }

        return emptyArray()
    }

    private inline fun <reified T> Test.findValue(name: String): T? {
        metaProperties.find { it.name == name }?.let { property ->
            return property.values["value"] as? T
        }

        return null
    }

    private companion object {
        private const val MESSAGE_LINES_COUNT = 3
        private const val LAYER = "layer"
        private const val TEST_PLAN_ENTRY = "testPlanEntry"
        private const val TEAM = "team"
        private const val COMPONENT = "component"
        private const val PLATFORM = "platform"
        private const val ANDROID = "Android"
        private const val CLIENT_APPLICATION = "Application client"
        private const val CLIENT_COMPONENT = "Component client"
    }
}

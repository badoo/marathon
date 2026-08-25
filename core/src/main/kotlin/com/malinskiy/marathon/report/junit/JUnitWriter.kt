package com.malinskiy.marathon.report.junit

import com.malinskiy.marathon.device.DeviceInfo
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.TestResult
import com.malinskiy.marathon.execution.TestStatus
import com.malinskiy.marathon.io.FileManager
import com.malinskiy.marathon.io.FileType
import com.malinskiy.marathon.report.summary.TestSummary
import com.malinskiy.marathon.report.summary.TestSummaryFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.XMLStreamWriter

class JUnitWriter(
    private val fileManager: FileManager,
    private val fileType: FileType,
    private val testSummaryFormatter: TestSummaryFormatter,
) {
    fun testFinished(
        devicePoolId: DevicePoolId,
        device: DeviceInfo,
        testResult: TestResult,
        testSummary: TestSummary?,
    ) {
        val file = fileManager.createFile(fileType, devicePoolId, device, testResult.test)
        file.createNewFile()

        file.outputStream().buffered().use { output ->
            val writer = XMLOutputFactory.newFactory().createXMLStreamWriter(output, "UTF-8")
            try {
                generateXml(writer, testResult, testSummary)
                writer.flush()
            } finally {
                writer.close()
            }
        }
    }

    private fun generateXml(writer: XMLStreamWriter, testResult: TestResult, testSummary: TestSummary?) {
        fun Long.toJUnitSeconds(): String = (this / 1000.0).toString()

        val test = testResult.test

        val failures = if (testResult.status == TestStatus.FAILURE) 1 else 0
        val ignored = if (testResult.status == TestStatus.IGNORED || testResult.status == TestStatus.ASSUMPTION_FAILURE) 1 else 0

        val formattedTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date(testResult.endTime))

        writer.document {
            element("testsuite") {
                attribute("name", "${test.pkg}.${test.clazz}")
                attribute("tests", "1")
                attribute("failures", "$failures")
                attribute("errors", "0")
                attribute("skipped", "$ignored")
                attribute("time", testResult.durationMillis().toJUnitSeconds())
                attribute("timestamp", formattedTimestamp)
                element("properties") {}
                element("testcase") {
                    attribute("classname", "${test.pkg}.${test.clazz}")
                    attribute("name", test.method)
                    attribute("time", testResult.durationMillis().toJUnitSeconds())
                    element("system-out") {
                        writeCData(testSummaryFormatter.formatTestResultSummary(testResult, testSummary))
                    }
                    when (testResult.status) {
                        TestStatus.IGNORED, TestStatus.ASSUMPTION_FAILURE -> {
                            element("skipped") {
                                testResult.stacktrace?.let {
                                    writeCData(it)
                                }
                            }
                        }
                        TestStatus.INCOMPLETE, TestStatus.FAILURE -> {
                            element("failure") {
                                writeCData(testResult.stacktrace.orEmpty())
                            }
                        }
                        else -> {
                        }
                    }
                }
            }
        }
    }
}

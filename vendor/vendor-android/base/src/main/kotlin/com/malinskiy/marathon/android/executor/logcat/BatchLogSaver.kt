package com.malinskiy.marathon.android.executor.logcat

import com.malinskiy.marathon.android.executor.logcat.model.LogcatMessage
import com.malinskiy.marathon.report.logs.BatchLogs
import com.malinskiy.marathon.report.logs.Log
import com.malinskiy.marathon.report.logs.LogEvent
import com.malinskiy.marathon.report.logs.LogTest
import kotlinx.coroutines.CompletableDeferred
import java.io.File
import java.io.Writer
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

class BatchLogSaver {

    private val fullBatchLogSaver = LogSaver()
    private val testLogSavers: MutableMap<LogTest, LogSaver> = ConcurrentHashMap()

    fun save(entry: SaveEntry, test: LogTest?) {
        fullBatchLogSaver.saveEntry(entry)

        if (test != null) {
            val testSaver = testLogSavers.getOrPut(test) { LogSaver() }
            testSaver.saveEntry(entry)
        }
    }

    fun onTestFinished(test: LogTest) {
        testLogSavers[test]?.close()
    }

    fun onBatchFinished() {
        testLogSavers.values.forEach(LogSaver::close)
        fullBatchLogSaver.close()
    }

    suspend fun getBatchLogs(forceCreate: Boolean): BatchLogs {
        val tests = testLogSavers
            .mapValues { it.value.getLog(forceCreate) }

        return BatchLogs(
            tests = tests,
            log = fullBatchLogSaver.getLog(forceCreate)
        )
    }

    sealed class SaveEntry {
        class Message(val message: LogcatMessage) : SaveEntry()
        class Event(val event: LogEvent) : SaveEntry()
    }

    private class LogSaver {

        private val logFile: File = createTempFile()
            .also {
                // file will be copied to target directory before exit
                it.deleteOnExit()
            }

        private val fileWriter: Writer = logFile.bufferedWriter()
        private val events: MutableList<LogEvent> = arrayListOf()

        private val logFuture: CompletableDeferred<Log> = CompletableDeferred()

        fun saveEntry(entry: SaveEntry) {
            if (logFuture.isCompleted) return

            when (entry) {
                is SaveEntry.Message -> fileWriter.writeMessage(entry.message)
                is SaveEntry.Event -> events.add(entry.event)
            }
        }

        fun close() {
            try {
                fileWriter.close()
            } finally {
                logFuture.complete(Log(logFile, events))
            }
        }

        suspend fun getLog(forceCreate: Boolean): Log {
            if (forceCreate) {
                close()
            }

            return logFuture.await()
        }

        private fun Writer.writeMessage(logcatMessage: LogcatMessage) {
            val timeStamp = LOGCAT_TIMESTAMP_FORMATTER.format(logcatMessage.timestamp)
            write(
                "$timeStamp ${logcatMessage.processId}-${logcatMessage.threadId}/${logcatMessage.applicationName} " +
                    "${logcatMessage.logLevel.letter}/${logcatMessage.tag}: ${logcatMessage.body}\n"
            )
        }

        private companion object {
            private val LOGCAT_TIMESTAMP_FORMATTER = DateTimeFormatter
                .ofPattern("MM-dd HH:mm:ss.SSS")
                .withZone(ZoneId.systemDefault())
        }
    }
}

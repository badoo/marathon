package com.malinskiy.marathon.android.ddmlib.shell

import com.android.ddmlib.IDevice
import com.android.ddmlib.logcat.LogCatHeader
import com.android.ddmlib.logcat.LogCatLongEpochMessageParser
import com.android.ddmlib.logcat.LogCatMessage
import org.apache.commons.io.input.TailerListenerAdapter

/**
 * Listens for logcat lines from `adb logcat -v long -v epoch` and emits parsed LogCatMessage on receiver with message bodies grouped by the header
 */
class LogcatParserListener(
    private val device: IDevice,
    private val receiver: (List<LogCatMessage>) -> Unit
) : TailerListenerAdapter() {

    private val parser = LogCatLongEpochMessageParser()
    private val messageBuffer = StringBuilder()
    private var lastHeader: LogCatHeader? = null

    override fun handle(line: String?) {
        if (line != null) {
            val logcatMessage = parser.processLogLines(arrayOf(line), device).firstOrNull() ?: return

            if (lastHeader != null && logcatMessage.header != lastHeader) {
                flushBuffer()
            }

            lastHeader = logcatMessage.header
            messageBuffer.appendLine(logcatMessage.message)
        }
    }

    override fun endOfFileReached() {
        flushBuffer()
    }

    private fun flushBuffer() {
        if (lastHeader != null && messageBuffer.isNotEmpty()) {
            // remove extra new line
            messageBuffer.setLength(messageBuffer.length - 1)

            receiver.invoke(listOf(LogCatMessage(lastHeader, messageBuffer.toString())))
            messageBuffer.setLength(0)
        }
    }

    override fun handle(exception: Exception) {
        exception.printStackTrace()
    }
}

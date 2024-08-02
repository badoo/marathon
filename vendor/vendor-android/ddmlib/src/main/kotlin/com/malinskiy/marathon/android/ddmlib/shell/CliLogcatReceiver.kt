package com.malinskiy.marathon.android.ddmlib.shell

import com.android.ddmlib.IDevice
import com.android.ddmlib.logcat.LogCatMessage
import com.malinskiy.marathon.io.FileManager
import com.malinskiy.marathon.io.FileType
import org.apache.commons.io.input.Tailer
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Date

class CliLogcatReceiver(
    private val adbPath: File,
    private val fileManager: FileManager,
    private val device: IDevice,
    private val listener: (List<LogCatMessage>) -> Unit
) {

    private var tailer: Tailer? = null
    private var process: Process? = null

    fun start() {
        val logcatFile = createFile()
        val receiver = LogcatParserListener(device, listener)

        process = captureLogcat(logcatFile)
        tailer = Tailer.create(
            logcatFile,
            receiver,
            TAILER_FREQUENCY_MS,
            true
        )
    }

    fun dispose() {
        tailer?.stop()
        process?.destroyForcibly()
    }

    private fun captureLogcat(redirectOutputTo: File): Process =
        ProcessBuilder()
            .command(adbPath.absolutePath, "-s", device.serialNumber, "logcat", "-v", "long", "-v", "epoch")
            .redirectOutput(redirectOutputTo)
            .start()

    private fun createFile(): File {
        val dateFormat: DateFormat = SimpleDateFormat("yyyy-MM-dd_HH_mm_ss")
        val fileName = "log_" + dateFormat.format(Date())
        return fileManager.createFile(FileType.FULL_LOG, device.serialNumber, fileName)
    }

    private companion object {
        private const val TAILER_FREQUENCY_MS = 100L
    }
}

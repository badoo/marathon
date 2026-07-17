package com.malinskiy.marathon.android.ddmlib.shell

import com.android.ddmlib.IDevice
import com.android.ddmlib.logcat.LogCatMessage
import com.malinskiy.marathon.io.FileManager
import com.malinskiy.marathon.io.FileType
import org.apache.commons.io.input.Tailer
import java.io.File
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.time.Duration
import java.util.Date
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

internal class CliLogcatReceiver(
    private val adbPath: File,
    private val fileManager: FileManager,
    private val device: IDevice,
    private val listener: (List<LogCatMessage>) -> Unit
) : AutoCloseable {

    private var logcatProcess: Process? = null
    private var logcatTailer: Tailer? = null
    private var tailerExecutor: ExecutorService? = null

    fun start() {
        val logcatFile = createFile()
        val logcatParserListener = LogcatParserListener(device, listener)

        logcatProcess = captureLogcat(logcatFile)
        val executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "logcat-tailer-${device.serialNumber}").apply { isDaemon = true }
        }.also { tailerExecutor = it }
        logcatTailer = Tailer.builder()
            .setDelayDuration(TAILER_DELAY)
            .setExecutorService(executor)
            .setFile(logcatFile)
            .setTailerListener(logcatParserListener)
            .setTailFromEnd(true)
            .get()
    }

    override fun close() {
        logcatTailer?.close()
        logcatProcess?.destroyForcibly()
        tailerExecutor?.shutdown()
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
        private val TAILER_DELAY = Duration.ofMillis(100)
    }
}

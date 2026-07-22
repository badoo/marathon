package com.malinskiy.marathon.android

import com.android.sdklib.AndroidApiLevel
import com.android.sdklib.AndroidVersion
import com.malinskiy.marathon.android.executor.listeners.video.ScreenRecorderHandler
import com.malinskiy.marathon.android.executor.listeners.video.ScreenRecorderOptions
import com.malinskiy.marathon.device.Device
import com.malinskiy.marathon.device.StubDevice
import java.awt.image.BufferedImage
import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class StubAndroidDevice(
    delegate: Device = StubDevice(),
    override val apiLevel: Int = 30,
    override val version: AndroidVersion = AndroidVersion(AndroidApiLevel(apiLevel))
) : AndroidDevice, Device by delegate {

    var screenshotProvider: () -> BufferedImage = { BufferedImage(720, 1280, BufferedImage.TYPE_INT_ARGB) }

    val executedCommands = CopyOnWriteArrayList<String>()
    val executedShellCommands = CopyOnWriteArrayList<String>()
    val pulledFiles = CopyOnWriteArrayList<String>()
    val screenRecordings = CopyOnWriteArrayList<ScreenRecording>()

    override val fileManager: RemoteFileManager by lazy { RemoteFileManager(this) }

    override fun getExternalStorageMount(): String = "/sdcard"

    override fun executeCommand(command: String, errorMessage: String) {
        executedCommands += command
    }

    override fun pullFile(remoteFilePath: String, localFilePath: String) {
        pulledFiles += remoteFilePath
    }

    override fun safeUninstallPackage(appPackage: String): String? = null

    override fun safeInstallPackage(absolutePath: String, reinstall: Boolean, optionalParams: String): String? = null

    override fun safeExecuteShellCommand(command: String): String {
        executedShellCommands += command
        return ""
    }

    override fun getScreenshot(timeout: Long, units: TimeUnit): BufferedImage = screenshotProvider()

    /**
     * Blocks until the handler signals stop, mimicking ddmlib's `screenrecord` shell command.
     * An interrupt unblocks it the same way it unblocks ddmlib's poll loop.
     */
    override fun safeStartScreenRecorder(handler: ScreenRecorderHandler, remoteFilePath: String, options: ScreenRecorderOptions) {
        val recording = ScreenRecording(remoteFilePath)
        screenRecordings += recording
        handler.subscribeOnStop { recording.stopSignal.countDown() }
        try {
            recording.stopSignal.await()
        } finally {
            recording.finishedSignal.countDown()
        }
    }

    fun awaitScreenRecording(timeout: Duration = Duration.ofSeconds(5)): ScreenRecording {
        val deadline = System.nanoTime() + timeout.toNanos()
        while (screenRecordings.isEmpty()) {
            check(System.nanoTime() < deadline) { "No screen recording started within $timeout" }
            Thread.sleep(POLL_INTERVAL_MS)
        }
        return screenRecordings.last()
    }

    override fun close() = Unit

    class ScreenRecording(val remoteFilePath: String) {
        internal val stopSignal = CountDownLatch(1)
        internal val finishedSignal = CountDownLatch(1)

        val isFinished: Boolean
            get() = finishedSignal.count == 0L

        fun awaitFinished(timeout: Duration): Boolean = finishedSignal.await(timeout.toMillis(), TimeUnit.MILLISECONDS)
    }

    private companion object {
        private const val POLL_INTERVAL_MS = 10L
    }
}

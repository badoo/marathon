package com.malinskiy.marathon.android

import com.android.sdklib.AndroidApiLevel
import com.android.sdklib.AndroidVersion
import com.malinskiy.marathon.android.executor.listeners.video.ScreenRecorderHandler
import com.malinskiy.marathon.android.executor.listeners.video.ScreenRecorderOptions
import com.malinskiy.marathon.device.Device
import com.malinskiy.marathon.device.StubDevice
import java.awt.image.BufferedImage
import java.util.concurrent.TimeUnit

class StubAndroidDevice(
    delegate: Device = StubDevice(),
    override val apiLevel: Int = 30,
    override val version: AndroidVersion = AndroidVersion(AndroidApiLevel(apiLevel))
) : AndroidDevice, Device by delegate {

    var screenshotProvider: () -> BufferedImage = { BufferedImage(720, 1280, BufferedImage.TYPE_INT_ARGB) }

    override val fileManager: RemoteFileManager by lazy { RemoteFileManager(this) }

    override fun getExternalStorageMount(): String = "/sdcard"

    override fun executeCommand(command: String, errorMessage: String) = Unit

    override fun pullFile(remoteFilePath: String, localFilePath: String) = Unit

    override fun safeUninstallPackage(appPackage: String): String? = null

    override fun safeInstallPackage(absolutePath: String, reinstall: Boolean, optionalParams: String): String? = null

    override fun safeExecuteShellCommand(command: String): String = ""

    override fun getScreenshot(timeout: Long, units: TimeUnit): BufferedImage = screenshotProvider()

    override fun safeStartScreenRecorder(handler: ScreenRecorderHandler, remoteFilePath: String, options: ScreenRecorderOptions) = Unit

    override fun close() = Unit
}

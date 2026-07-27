package com.malinskiy.marathon.android.ddmlib

import com.android.ddmlib.IDevice
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.android.AndroidAppInstaller
import com.malinskiy.marathon.android.executor.logcat.LogcatListener
import com.malinskiy.marathon.android.serial.SerialStrategy
import com.malinskiy.marathon.execution.StrictRunChecker
import com.malinskiy.marathon.io.AttachmentManager
import com.malinskiy.marathon.io.FileManager
import com.malinskiy.marathon.time.SystemTimer
import com.malinskiy.marathon.time.Timer
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import org.mockito.kotlin.mock
import java.io.File
import java.time.Clock

fun TestScope.stubDdmlibAndroidDevice(
    ddmsDevice: IDevice,
    adbPath: File = File("adb"),
    track: Track = Track(),
    timer: Timer = SystemTimer(Clock.systemDefaultZone()),
    androidAppInstaller: AndroidAppInstaller = mock(),
    attachmentManager: AttachmentManager = mock(),
    reportsFileManager: FileManager = mock(),
    serialStrategy: SerialStrategy = SerialStrategy.AUTOMATIC,
    logcatListener: LogcatListener = mock(),
    strictRunChecker: StrictRunChecker = mock()
): DdmlibAndroidDevice = DdmlibAndroidDevice(
    ddmsDevice = ddmsDevice,
    adbPath = adbPath,
    track = track,
    timer = timer,
    androidAppInstaller = androidAppInstaller,
    attachmentManager = attachmentManager,
    reportsFileManager = reportsFileManager,
    serialStrategy = serialStrategy,
    logcatListener = logcatListener,
    strictRunChecker = strictRunChecker,
    ioDispatcher = StandardTestDispatcher(testScheduler)
)

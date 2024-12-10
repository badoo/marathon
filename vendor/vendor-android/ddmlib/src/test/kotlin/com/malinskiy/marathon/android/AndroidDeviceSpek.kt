package com.malinskiy.marathon.android

import com.android.ddmlib.IDevice
import com.android.sdklib.AndroidVersion
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.android.ddmlib.DdmlibAndroidDevice
import com.malinskiy.marathon.android.serial.SerialStrategy
import com.malinskiy.marathon.time.SystemTimer
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.junit.jupiter.api.Assertions.assertEquals
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.time.Clock

class AndroidDeviceSpek : Spek({
    describe("android device") {
        val iDevice = mock<IDevice>()
        whenever(iDevice.serialNumber).thenReturn("serial")
        val track = Track()
        val timer = SystemTimer(Clock.systemDefaultZone())
        val appInstaller = mock<AndroidAppInstaller>()

        it("model return Unknown if ddmDevice property ro.product.model") {
            whenever(iDevice.getProperty("ro.product.model")).thenReturn(null)
            val device = DdmlibAndroidDevice(
                ddmsDevice = iDevice,
                adbPath = File("adb"),
                track = track,
                timer = timer,
                androidAppInstaller = appInstaller,
                attachmentManager = mock(),
                reportsFileManager = mock(),
                serialStrategy = SerialStrategy.AUTOMATIC,
                logcatListener = mock(),
                strictRunChecker = mock()
            )
            assertEquals("Unknown", device.model)
        }
        it("manufacturer return Unknown if ddmlib property ") {
            whenever(iDevice.getProperty("ro.product.manufacturer")).thenReturn(null)
            val device = DdmlibAndroidDevice(
                ddmsDevice = iDevice,
                adbPath = File("adb"),
                track = track,
                timer = timer,
                androidAppInstaller = appInstaller,
                attachmentManager = mock(),
                reportsFileManager = mock(),
                serialStrategy = SerialStrategy.AUTOMATIC,
                logcatListener = mock(),
                strictRunChecker = mock()
            )
            assertEquals("Unknown", device.manufacturer)
        }
        it("should return ddmlib version instead of ro.build.version.sdk property value") {
            val default = AndroidVersion.DEFAULT
            whenever(iDevice.version).thenReturn(default)
            whenever(iDevice.getProperty("ro.build.version.sdk")).thenReturn("INVALID_VERSION")
            val device = DdmlibAndroidDevice(
                ddmsDevice = iDevice,
                adbPath = File("adb"),
                track = track,
                timer = timer,
                androidAppInstaller = appInstaller,
                attachmentManager = mock(),
                reportsFileManager = mock(),
                serialStrategy = SerialStrategy.AUTOMATIC,
                logcatListener = mock(),
                strictRunChecker = mock()
            )
            assertEquals(default.apiString, device.operatingSystem.version)
        }
    }
})

package com.malinskiy.marathon.android

import com.android.ddmlib.IDevice
import com.android.sdklib.AndroidVersion
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.android.ddmlib.DdmlibAndroidDevice
import com.malinskiy.marathon.android.serial.SerialStrategy
import com.malinskiy.marathon.time.SystemTimer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.File
import java.time.Clock

class AndroidDeviceTest {
    private val iDevice = mock<IDevice>()

    @BeforeEach
    fun setUp() {
        whenever(iDevice.serialNumber).thenReturn("serial")
    }

    @Test
    fun `model return Unknown if ddmDevice property ro-product-model is missing`() {
        whenever(iDevice.getProperty("ro.product.model")).thenReturn(null)
        val device = createDevice()

        assertThat(device.model).isEqualTo("Unknown")
    }

    @Test
    fun `manufacturer return Unknown if ddmlib property ro-product-manufacturer is missing`() {
        whenever(iDevice.getProperty("ro.product.manufacturer")).thenReturn(null)
        val device = createDevice()

        assertThat(device.manufacturer).isEqualTo("Unknown")
    }

    @Test
    fun `should return ddmlib version instead of ro-build-version-sdk property value`() {
        val default = AndroidVersion.DEFAULT
        whenever(iDevice.version).thenReturn(default)
        whenever(iDevice.getProperty("ro.build.version.sdk")).thenReturn("INVALID_VERSION")
        val device = createDevice()

        assertThat(device.operatingSystem.version).isEqualTo(default.apiString)
    }

    private fun createDevice() = DdmlibAndroidDevice(
        ddmsDevice = iDevice,
        adbPath = File("adb"),
        track = Track(),
        timer = SystemTimer(Clock.systemDefaultZone()),
        androidAppInstaller = mock<AndroidAppInstaller>(),
        attachmentManager = mock(),
        reportsFileManager = mock(),
        serialStrategy = SerialStrategy.AUTOMATIC,
        logcatListener = mock(),
        strictRunChecker = mock()
    )
}

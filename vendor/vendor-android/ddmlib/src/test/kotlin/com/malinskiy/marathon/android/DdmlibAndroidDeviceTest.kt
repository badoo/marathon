package com.malinskiy.marathon.android

import com.android.ddmlib.AdbCommandRejectedException
import com.android.ddmlib.IDevice
import com.android.ddmlib.SyncException
import com.android.ddmlib.TimeoutException
import com.android.sdklib.AndroidVersion
import com.malinskiy.marathon.android.ddmlib.stubDdmlibAndroidDevice
import com.malinskiy.marathon.android.exception.TransferException
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.test.TestBatch
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.IOException

class DdmlibAndroidDeviceTest {
    private val appInstaller = mock<AndroidAppInstaller>()
    private val iDevice = mock<IDevice> {
        on { serialNumber } doReturn "serial"
    }

    @Test
    fun `model return Unknown if ddmDevice property ro-product-model is missing`() = runTest {
        whenever(iDevice.getProperty("ro.product.model")).thenReturn(null)
        val device = createDevice()

        assertThat(device.model).isEqualTo("Unknown")
    }

    @Test
    fun `manufacturer return Unknown if ddmlib property ro-product-manufacturer is missing`() = runTest {
        whenever(iDevice.getProperty("ro.product.manufacturer")).thenReturn(null)
        val device = createDevice()

        assertThat(device.manufacturer).isEqualTo("Unknown")
    }

    @Test
    fun `should return ddmlib version instead of ro-build-version-sdk property value`() = runTest {
        val default = AndroidVersion.DEFAULT
        whenever(iDevice.version).thenReturn(default)
        whenever(iDevice.getProperty("ro.build.version.sdk")).thenReturn("INVALID_VERSION")
        val device = createDevice()

        assertThat(device.operatingSystem.version).isEqualTo(default.apiString)
    }

    @Test
    fun `GIVEN installer throws InterruptedException WHEN executing a batch THEN rethrows the interruption`() = runTest {
        whenever(appInstaller.ensureInstalled(any(), any())).thenAnswer { throw InterruptedException("Interrupted") }
        val device = createDevice()
        val componentInfo = stubAndroidComponentInfo(testApplicationId = "com.test")
        val batch = TestBatch(tests = emptyList(), componentInfo = componentInfo)

        assertThrows<InterruptedException> {
            device.execute(
                configuration = mock(),
                devicePoolId = DevicePoolId("test-pool"),
                testBatch = batch,
                deferred = CompletableDeferred(),
                progressReporter = mock()
            )
        }
    }

    @ParameterizedTest(name = "pullFile wraps {0} into TransferException")
    @MethodSource("pullFileExceptions")
    fun `pullFile wraps ddmlib exceptions into TransferException`(exception: Exception) = runTest {
        whenever(iDevice.pullFile(any(), any())).thenThrow(exception)
        val device = createDevice()

        assertThrows<TransferException> {
            device.pullFile("/sdcard/video.mp4", "video.mp4")
        }
    }

    private fun TestScope.createDevice() =
        stubDdmlibAndroidDevice(ddmsDevice = iDevice, androidAppInstaller = appInstaller)

    companion object {
        @JvmStatic
        fun pullFileExceptions(): List<Exception> = listOf(
            SyncException(SyncException.SyncError.TRANSFER_PROTOCOL_ERROR),
            TimeoutException("timeout"),
            AdbCommandRejectedException("rejected"),
            IOException("io error")
        )
    }
}

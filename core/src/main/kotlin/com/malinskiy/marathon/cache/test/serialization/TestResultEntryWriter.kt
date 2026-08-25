package com.malinskiy.marathon.cache.test.serialization

import com.malinskiy.marathon.cache.CacheEntryWriter
import com.malinskiy.marathon.device.DeviceInfo
import com.malinskiy.marathon.execution.Attachment
import com.malinskiy.marathon.execution.TestResult
import io.ktor.utils.io.core.buildPacket
import io.ktor.utils.io.core.remaining
import io.ktor.utils.io.core.writeText
import io.ktor.utils.io.streams.writePacket
import java.io.DataOutputStream
import java.io.File
import java.io.OutputStream

class TestResultEntryWriter(private val testResult: TestResult) : CacheEntryWriter {
    override fun writeTo(output: OutputStream) {
        DataOutputStream(output).run {
            writeDeviceInfo(testResult.device)
            writeInt(testResult.status.ordinal)
            writeLong(testResult.startTime)
            writeLong(testResult.endTime)
            writeString(testResult.batchId)
            writeString(testResult.stacktrace)

            writeInt(testResult.attachments.size)
            testResult.attachments.forEach { attachment ->
                writeAttachment(attachment)
            }
            flush()
        }
    }

    private fun DataOutputStream.writeAttachment(attachment: Attachment) {
        writeInt(attachment.type.ordinal)
        writeInt(attachment.fileType.ordinal)
        writeFile(attachment.file)
    }

    private fun DataOutputStream.writeFile(file: File) {
        writeLong(file.length())
        write(file.readBytes())
    }

    private fun DataOutputStream.writeDeviceInfo(deviceInfo: DeviceInfo) {
        writeString(deviceInfo.operatingSystem.version)
        writeString(deviceInfo.serialNumber)
        writeString(deviceInfo.model)
        writeString(deviceInfo.manufacturer)
        writeInt(deviceInfo.networkState.ordinal)
        writeEnumCollection(deviceInfo.deviceFeatures)
        writeBoolean(deviceInfo.healthy)
    }

    private fun DataOutputStream.writeEnumCollection(collection: Collection<Enum<*>>) {
        writeInt(collection.size)
        collection.forEach {
            writeInt(it.ordinal)
        }
    }

    private fun DataOutputStream.writeString(str: String?) {
        if (str == null) {
            writeBoolean(false)
            return
        }

        writeBoolean(true)

        val packet = buildPacket {
            writeText(str)
        }

        writeLong(packet.remaining)
        writePacket(packet)
    }
}

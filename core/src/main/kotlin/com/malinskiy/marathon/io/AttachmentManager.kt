package com.malinskiy.marathon.io

import com.malinskiy.marathon.device.DeviceInfo
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.Attachment
import com.malinskiy.marathon.execution.AttachmentType
import com.malinskiy.marathon.test.Test
import com.malinskiy.marathon.test.toTestName
import java.io.File
import java.nio.file.Files.createDirectories
import java.nio.file.Path
import java.nio.file.Paths.get

class AttachmentManager(private val outputDirectory: File) {

    fun createAttachment(fileType: FileType, attachmentType: AttachmentType): Attachment {
        val file = File.createTempFile(TEMP_FILE_PREFIX, TEMP_FILE_SUFFIX)
            .apply { deleteOnExit() }

        return Attachment(
            file = file,
            type = attachmentType,
            fileType = fileType
        )
    }

    fun writeToTarget(
        batchId: String,
        poolId: DevicePoolId,
        device: DeviceInfo,
        runId: String,
        test: Test,
        attachment: Attachment
    ): File {
        val directory = createDirectory(attachment.fileType, poolId, device)
        val filename = createFilename(test, runId, batchId, attachment.fileType)
        val targetFile = createFile(directory, filename)

        attachment.file.copyTo(targetFile)

        return targetFile
    }

    private fun createDirectory(fileType: FileType, pool: DevicePoolId, device: DeviceInfo): Path =
        createDirectories(getDirectory(fileType, pool, device))

    private fun getDirectory(fileType: FileType, pool: DevicePoolId, device: DeviceInfo): Path =
        getDirectory(fileType, pool, device.serialNumber)

    private fun getDirectory(fileType: FileType, pool: DevicePoolId, serial: String): Path =
        get(outputDirectory.absolutePath, fileType.dir, pool.name, serial)

    private fun createFile(directory: Path, filename: String): File = File(directory.toFile(), filename)

    private fun createFilename(test: Test, runId: String, batchId: String, fileType: FileType): String =
        "${test.toTestName().take(TEST_NAME_CHARACTERS_LIMIT)}-$runId-$batchId.${fileType.suffix}"

    private companion object {
        private const val TEMP_FILE_PREFIX = "test_run_attachment"
        private const val TEMP_FILE_SUFFIX = "tmp"

        // On some file systems file names are limited to 255 symbols
        private const val TEST_NAME_CHARACTERS_LIMIT = 150
    }
}

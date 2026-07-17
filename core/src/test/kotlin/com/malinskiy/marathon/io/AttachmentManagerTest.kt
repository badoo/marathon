package com.malinskiy.marathon.io

import com.malinskiy.marathon.createDeviceInfo
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.execution.AttachmentType
import com.malinskiy.marathon.test.TestComponentInfo
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import com.malinskiy.marathon.test.Test as MarathonTest

class AttachmentManagerTest {
    @TempDir
    private lateinit var outputDir: File

    private val tempFileFactory: TempFileFactory by lazy {
        DefaultTempFileFactory(File(outputDir, "tmp"))
    }
    private val attachmentManager by lazy {
        AttachmentManager(outputDir, tempFileFactory)
    }

    @Test
    fun `WHEN creating an attachment THEN its file is created under the tmp directory`() {
        val attachment = attachmentManager.createAttachment(FileType.VIDEO, AttachmentType.VIDEO)

        assertThat(attachment.file).exists()
        assertThat(attachment.file.parentFile).isEqualTo(File(outputDir, "tmp"))
        assertThat(attachment.type).isEqualTo(AttachmentType.VIDEO)
        assertThat(attachment.fileType).isEqualTo(FileType.VIDEO)
    }

    @Test
    fun `GIVEN an attachment with content WHEN writing to target THEN content is copied AND the temp file remains`() {
        val attachment = attachmentManager.createAttachment(FileType.LOG, AttachmentType.LOG)
        attachment.file.writeText("log content")

        val targetFile = attachmentManager.writeToTarget(
            batchId = "batch-1",
            poolId = DevicePoolId("pool"),
            device = createDeviceInfo(),
            runId = "run-1",
            test = createTest(),
            attachment = attachment
        )

        assertThat(targetFile).hasContent("log content")
        assertThat(targetFile.parentFile).isEqualTo(File(outputDir, "logs/pool/fake serial"))
        assertThat(attachment.file).exists()
    }

    private fun createTest(): MarathonTest =
        MarathonTest(
            pkg = "com.test",
            clazz = "Test",
            method = "test1",
            componentInfo = TestComponentInfo(someInfo = "someInfo", name = "component-name"),
            metaProperties = emptyList()
        )
}

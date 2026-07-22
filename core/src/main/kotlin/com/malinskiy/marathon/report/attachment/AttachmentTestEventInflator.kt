package com.malinskiy.marathon.report.attachment

import com.malinskiy.marathon.analytics.internal.sub.TestEvent
import com.malinskiy.marathon.analytics.internal.sub.TestEventInflator
import com.malinskiy.marathon.io.AttachmentManager
import java.util.UUID

class AttachmentTestEventInflator(
    private val attachmentManager: AttachmentManager
) : TestEventInflator {

    /**
     * Writes test attachments to the destination directory
     */
    override suspend fun inflate(event: TestEvent): TestEvent {
        val test = event.testResult
        val runId = UUID.randomUUID().toString()
        val newAttachments = event.testResult.attachments
            .map { attachment ->
                val targetFile = attachmentManager.writeToTarget(
                    test.batchId,
                    event.poolId,
                    test.device,
                    runId,
                    test.test,
                    attachment
                )

                attachment.copy(file = targetFile)
            }

        return event.copy(
            testResult = event.testResult.copy(
                attachments = newAttachments
            )
        )
    }
}

package com.malinskiy.marathon.report.attachment

import com.malinskiy.marathon.execution.Attachment
import com.malinskiy.marathon.test.Test

class StubAttachmentListener : AttachmentListener {
    val attachments = mutableMapOf<Test, MutableList<Attachment>>()

    override fun onAttachment(test: Test, attachment: Attachment) {
        attachments.getOrPut(test) { mutableListOf() }.add(attachment)
    }
}

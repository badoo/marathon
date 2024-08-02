package com.malinskiy.marathon.report.attachment

import com.malinskiy.marathon.execution.Attachment
import com.malinskiy.marathon.test.Test

interface AttachmentListener {
    fun onAttachment(test: Test, attachment: Attachment)
}

package com.malinskiy.marathon.report.attachment

interface AttachmentProvider {
    fun registerListener(listener: AttachmentListener)
}

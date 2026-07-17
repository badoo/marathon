package com.malinskiy.marathon.vendor

import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.StrictRunChecker
import com.malinskiy.marathon.io.AttachmentManager
import com.malinskiy.marathon.io.FileHasher
import com.malinskiy.marathon.io.FileManager
import com.malinskiy.marathon.io.TempFileFactory
import com.malinskiy.marathon.time.Timer

class VendorDependencies(
    val configuration: Configuration,
    val track: Track,
    val timer: Timer,
    val fileManager: FileManager,
    val attachmentManager: AttachmentManager,
    val tempFileFactory: TempFileFactory,
    val fileHasher: FileHasher,
    val strictRunChecker: StrictRunChecker
)

package com.malinskiy.marathon.vendor

import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.StrictRunChecker
import com.malinskiy.marathon.io.AttachmentManager
import com.malinskiy.marathon.io.FileHasher
import com.malinskiy.marathon.io.FileManager
import com.malinskiy.marathon.io.TempFileFactory
import com.malinskiy.marathon.time.Timer
import kotlinx.coroutines.CoroutineDispatcher

class VendorDependencies(
    val attachmentManager: AttachmentManager,
    val configuration: Configuration,
    val fileHasher: FileHasher,
    val fileManager: FileManager,
    val ioDispatcher: CoroutineDispatcher,
    val strictRunChecker: StrictRunChecker,
    val tempFileFactory: TempFileFactory,
    val timer: Timer,
    val track: Track
)

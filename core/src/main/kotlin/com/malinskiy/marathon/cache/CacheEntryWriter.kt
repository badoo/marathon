package com.malinskiy.marathon.cache

import io.ktor.utils.io.*

interface CacheEntryWriter {
    suspend fun writeTo(output: ByteWriteChannel)
}

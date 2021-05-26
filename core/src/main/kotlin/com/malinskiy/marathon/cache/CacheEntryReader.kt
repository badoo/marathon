package com.malinskiy.marathon.cache

import io.ktor.utils.io.*

interface CacheEntryReader {
    suspend fun readFrom(input: ByteReadChannel)
}

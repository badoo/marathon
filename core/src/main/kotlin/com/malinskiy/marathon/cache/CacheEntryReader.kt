package com.malinskiy.marathon.cache

import io.ktor.utils.io.ByteReadChannel

interface CacheEntryReader {
    suspend fun readFrom(input: ByteReadChannel)
}

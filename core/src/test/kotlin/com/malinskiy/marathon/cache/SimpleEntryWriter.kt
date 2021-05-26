package com.malinskiy.marathon.cache

import io.ktor.utils.io.*

class SimpleEntryWriter(data: String) : CacheEntryWriter {

    private val bytes = data.toByteArray()

    override suspend fun writeTo(output: ByteWriteChannel) {
        output.writeFully(bytes)
    }

}

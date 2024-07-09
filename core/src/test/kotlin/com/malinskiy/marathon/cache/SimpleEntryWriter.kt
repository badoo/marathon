package com.malinskiy.marathon.cache

import java.io.OutputStream

class SimpleEntryWriter(data: String) : CacheEntryWriter {

    private val bytes = data.toByteArray()

    override fun writeTo(output: OutputStream) {
       output.write(bytes)
    }

}

package com.malinskiy.marathon.io

import java.io.File

class DefaultTempFileFactory(private val tempDirectory: File) : TempFileFactory {
    override fun create(prefix: String, extension: String): File {
        tempDirectory.mkdirs()
        return File.createTempFile(prefix, extension, tempDirectory)
    }
}

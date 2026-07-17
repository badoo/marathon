package com.malinskiy.marathon.io

import java.io.File

fun interface TempFileFactory {
    fun create(prefix: String, extension: String): File
}

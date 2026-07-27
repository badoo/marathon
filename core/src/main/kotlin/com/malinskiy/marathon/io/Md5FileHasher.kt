package com.malinskiy.marathon.io

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import java.util.HexFormat

internal class Md5FileHasher(
    private val ioDispatcher: CoroutineDispatcher
) : FileHasher {

    override suspend fun getHash(file: File): String =
        withContext(ioDispatcher) {
            file.calculateHash()
        }

    private fun File.calculateHash(): String {
        val messageDigest = MessageDigest.getInstance("MD5")

        val digest = inputStream()
            .use {
                val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                var bytesRead: Int

                do {
                    bytesRead = it.read(buffer)
                    if (bytesRead > 0) {
                        messageDigest.update(buffer, 0, bytesRead)
                    }
                } while (bytesRead != -1)

                messageDigest.digest()
            }

        return HexFormat.of().formatHex(digest)
    }
}

package com.malinskiy.marathon.io

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class Md5FileHasherTest {
    private val hasher = Md5FileHasher()

    @TempDir
    private lateinit var tempDir: File

    @Test
    fun `hashes file content to its md5 hex string`() = runTest {
        val file = tempDir.resolve("file").apply { writeText("hello world") }

        val hash = hasher.getHash(file)

        assertThat(hash).isEqualTo("5eb63bbbe01eeed093cb22bb8f5acdc3")
    }

    @Test
    fun `hashes an empty file`() = runTest {
        val file = tempDir.resolve("file").apply { writeText("") }

        val hash = hasher.getHash(file)

        assertThat(hash).isEqualTo("d41d8cd98f00b204e9800998ecf8427e")
    }

    @Test
    fun `pads hash with leading zeros to 32 characters`() = runTest {
        val file = tempDir.resolve("file").apply { writeText("jk8ssl") }

        val hash = hasher.getHash(file)

        assertThat(hash).isEqualTo("0000000018e6137ac2caab16074784a6")
    }
}

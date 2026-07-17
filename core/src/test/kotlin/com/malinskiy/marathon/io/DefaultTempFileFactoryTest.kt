package com.malinskiy.marathon.io

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class DefaultTempFileFactoryTest {
    @TempDir
    private lateinit var rootDir: File

    @Test
    fun `WHEN creating a temp file THEN it is created in the temp directory with the given prefix and extension`() {
        val factory = DefaultTempFileFactory(File(rootDir, "tmp"))

        val file = factory.create(prefix = "logcat", extension = ".log")

        assertThat(file).exists()
        assertThat(file.parentFile).isEqualTo(File(rootDir, "tmp"))
        assertThat(file.name).startsWith("logcat").endsWith(".log")
    }

    @Test
    fun `GIVEN missing temp directory WHEN creating a temp file THEN the directory is created on demand`() {
        val tempDir = File(rootDir, "tmp")
        val factory = DefaultTempFileFactory(tempDir)

        val file = factory.create(prefix = "logcat", extension = ".log")

        assertThat(file).exists()
        assertThat(tempDir).isDirectory()
    }

    @Test
    fun `WHEN creating multiple temp files THEN each file is unique`() {
        val factory = DefaultTempFileFactory(File(rootDir, "tmp"))

        val first = factory.create(prefix = "logcat", extension = ".log")
        val second = factory.create(prefix = "logcat", extension = ".log")

        assertThat(first).isNotEqualTo(second)
    }
}

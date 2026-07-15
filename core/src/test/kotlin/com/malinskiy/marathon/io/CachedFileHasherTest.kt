package com.malinskiy.marathon.io

import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.io.File
import java.util.concurrent.ConcurrentHashMap

class CachedFileHasherTest {
    private val delegate = CountingFileHasher()

    @Test
    fun `returns hash from the delegate`() = runTest {
        val hasher = CachedFileHasher(delegate)

        val hash = hasher.getHash(File("a"))

        assertThat(hash).isEqualTo("hash-a")
    }

    @Test
    fun `caches hash of the same file`() = runTest {
        val hasher = CachedFileHasher(delegate)

        hasher.getHash(File("a"))
        val cachedHash = hasher.getHash(File("a"))

        assertThat(cachedHash).isEqualTo("hash-a")
        assertThat(delegate.hashCount(File("a"))).isEqualTo(1)
    }

    @Test
    fun `accessing an entry protects it from eviction`() = runTest {
        val hasher = CachedFileHasher(delegate, cacheCapacity = 2)

        hasher.getHash(File("a"))
        hasher.getHash(File("b"))
        hasher.getHash(File("a"))
        hasher.getHash(File("c"))
        hasher.getHash(File("a"))

        assertThat(delegate.hashCount(File("a"))).isEqualTo(1)
    }

    @Test
    fun `evicts the least recently used entry when capacity is exceeded`() = runTest {
        val hasher = CachedFileHasher(delegate, cacheCapacity = 2)

        hasher.getHash(File("a"))
        hasher.getHash(File("b"))
        hasher.getHash(File("a"))
        hasher.getHash(File("c"))
        hasher.getHash(File("a"))
        hasher.getHash(File("b"))

        assertThat(delegate.hashCount(File("a"))).isEqualTo(1)
        assertThat(delegate.hashCount(File("b"))).isEqualTo(2)
        assertThat(delegate.hashCount(File("c"))).isEqualTo(1)
    }

    private class CountingFileHasher : FileHasher {
        private val hashCounts = ConcurrentHashMap<File, Int>()

        override suspend fun getHash(file: File): String {
            hashCounts.merge(file, 1, Int::plus)
            return "hash-${file.name}"
        }

        fun hashCount(file: File): Int = hashCounts.getOrDefault(file, 0)
    }
}

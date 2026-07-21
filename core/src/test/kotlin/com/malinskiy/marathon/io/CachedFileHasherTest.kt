package com.malinskiy.marathon.io

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

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
    fun `does not cache a failed hash computation`() = runTest {
        val failingDelegate = CountingFileHasher(failures = 1)
        val hasher = CachedFileHasher(failingDelegate)

        assertThrows<IOException> { hasher.getHash(File("a")) }
        val hash = hasher.getHash(File("a"))

        assertThat(hash).isEqualTo("hash-a")
        assertThat(failingDelegate.hashCount(File("a"))).isEqualTo(2)
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
    fun `computes hash of the same file only once under concurrent access`() = runTest {
        val slowDelegate = CountingFileHasher(delay = 100.milliseconds)
        val hasher = CachedFileHasher(slowDelegate)

        val hashes = (1..10).map { async { hasher.getHash(File("a")) } }.awaitAll()

        assertThat(hashes).containsOnly("hash-a")
        assertThat(slowDelegate.hashCount(File("a"))).isEqualTo(1)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun `computes hashes of different files concurrently`() = runTest {
        val slowDelegate = CountingFileHasher(delay = 100.milliseconds)
        val hasher = CachedFileHasher(slowDelegate)

        val start = currentTime
        listOf(File("a"), File("b")).map { async { hasher.getHash(it) } }.awaitAll()

        assertThat(currentTime - start).isEqualTo(100)
    }

    @Test
    fun `does not recompute the hash when a waiting caller is cancelled`() = runTest {
        val slowDelegate = CountingFileHasher(delay = 100.milliseconds)
        val hasher = CachedFileHasher(slowDelegate)

        val winner = async { hasher.getHash(File("a")) }
        val waiter = launch { hasher.getHash(File("a")) }
        delay(50.milliseconds)
        waiter.cancelAndJoin()
        val latecomer = async { hasher.getHash(File("a")) }
        val hashes = listOf(winner, latecomer).awaitAll()

        assertThat(hashes).containsOnly("hash-a")
        assertThat(slowDelegate.hashCount(File("a"))).isEqualTo(1)
    }

    @Test
    fun `does not block concurrent callers when the first hash computation fails`() = runTest {
        val failingDelegate = CountingFileHasher(delay = 100.milliseconds, failures = 1)
        val hasher = CachedFileHasher(failingDelegate)

        val hashes = supervisorScope {
            val failing = async { hasher.getHash(File("a")) }
            val waiting = (1..9).map { async { hasher.getHash(File("a")) } }
            assertThrows<IOException> { failing.await() }
            waiting.awaitAll()
        }

        assertThat(hashes).containsOnly("hash-a")
        assertThat(failingDelegate.hashCount(File("a"))).isEqualTo(2)
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

    private class CountingFileHasher(
        private val delay: Duration = Duration.ZERO,
        private val failures: Int = 0
    ) : FileHasher {
        private val hashCounts = ConcurrentHashMap<File, Int>()

        override suspend fun getHash(file: File): String {
            val attempt = hashCounts.merge(file, 1, Int::plus)!!
            delay(delay)
            if (attempt <= failures) {
                throw IOException("Simulated hash failure for ${file.name}")
            }
            return "hash-${file.name}"
        }

        fun hashCount(file: File): Int = hashCounts.getOrDefault(file, 0)
    }
}

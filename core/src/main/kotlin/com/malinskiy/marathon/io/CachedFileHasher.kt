package com.malinskiy.marathon.io

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap

internal class CachedFileHasher(
    private val delegate: FileHasher,
    cacheCapacity: Int = DEFAULT_CACHE_CAPACITY
) : FileHasher {

    private val cache = Collections.synchronizedMap(LruMap<File, String>(cacheCapacity, LOAD_FACTOR))
    private val locks = ConcurrentHashMap<File, Mutex>()

    override suspend fun getHash(file: File): String {
        cache[file]?.let { return it }

        val mutex = locks.computeIfAbsent(file) { Mutex() }
        return mutex.withLock {
            try {
                cache.getOrPut(file) { delegate.getHash(file) }
            } finally {
                locks.remove(file, mutex)
            }
        }
    }

    private class LruMap<K, V>(
        private val maxSize: Int,
        loadFactor: Float
    ) : LinkedHashMap<K, V>(initialCapacity(maxSize, loadFactor), loadFactor, /* accessOrder = */ true) {

        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<K, V>): Boolean = size > maxSize

        private companion object {
            // Sized so the backing table never resizes before reaching maxSize entries.
            private fun initialCapacity(maxSize: Int, loadFactor: Float): Int = (maxSize / loadFactor).toInt() + 1
        }
    }

    private companion object {
        private const val DEFAULT_CACHE_CAPACITY = 512
        private const val LOAD_FACTOR = 0.75f
    }
}

package com.malinskiy.marathon.io

import java.io.File
import java.util.Collections

internal class CachedFileHasher(
    private val delegate: FileHasher,
    cacheCapacity: Int = DEFAULT_CACHE_CAPACITY
) : FileHasher {

    private val cache = Collections.synchronizedMap(LruMap<File, String>(cacheCapacity, LOAD_FACTOR))

    override suspend fun getHash(file: File): String =
        cache.getOrPut(file) { delegate.getHash(file) }

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

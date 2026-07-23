package com.malinskiy.marathon.cache

internal class NoOpCacheService : CacheService {
    override suspend fun load(key: CacheKey, reader: CacheEntryReader): Boolean = false

    override suspend fun store(key: CacheKey, writer: CacheEntryWriter) = Unit

    override fun close() = Unit
}

package com.malinskiy.marathon.cache

interface CacheService : AutoCloseable {
    suspend fun load(key: CacheKey, reader: CacheEntryReader): Boolean
    suspend fun store(key: CacheKey, writer: CacheEntryWriter)
}

package com.malinskiy.marathon.cache

import com.malinskiy.marathon.execution.CacheConfiguration

fun interface CacheServiceFactory {
    fun createCacheService(config: CacheConfiguration): CacheService
}

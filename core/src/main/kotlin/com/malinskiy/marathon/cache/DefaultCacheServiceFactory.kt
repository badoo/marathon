package com.malinskiy.marathon.cache

import com.malinskiy.marathon.cache.config.LocalCacheConfiguration
import com.malinskiy.marathon.cache.config.RemoteCacheConfiguration
import com.malinskiy.marathon.cache.gradle.GradleHttpCacheService
import com.malinskiy.marathon.execution.CacheConfiguration

internal class DefaultCacheServiceFactory : CacheServiceFactory {
    override fun createCacheService(config: CacheConfiguration): CacheService {
        require(config.local is LocalCacheConfiguration.Disabled) { "Local cache is not supported yet" }

        return when (config.remote) {
            is RemoteCacheConfiguration.Enabled -> GradleHttpCacheService(config.remote)
            is RemoteCacheConfiguration.Disabled -> NoOpCacheService()
        }
    }
}

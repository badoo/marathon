package com.malinskiy.marathon

import com.malinskiy.marathon.cache.config.Credentials
import com.malinskiy.marathon.cache.config.LocalCacheConfiguration
import com.malinskiy.marathon.cache.config.RemoteCacheConfiguration
import com.malinskiy.marathon.execution.CacheConfiguration
import org.gradle.api.Action
import java.io.File

open class CachePluginConfiguration {

    var localExtension: LocalCacheExtension? = null
    var remoteExtension: RemoteCacheExtension? = null

    fun local(action: Action<LocalCacheExtension>) {
        localExtension = (localExtension ?: LocalCacheExtension()).also { action.execute(it) }
    }

    fun remote(action: Action<RemoteCacheExtension>) {
        remoteExtension = (remoteExtension ?: RemoteCacheExtension()).also { action.execute(it) }
    }
}

private val DEFAULT_LOCAL_CACHE_DIRECTORY = File("~/cache/marathon")
private const val DEFAULT_LOCAL_UNUSED_ENTRIES_DELETE_AFTER_DAYS = 7

open class LocalCacheExtension {
    var directory: File = DEFAULT_LOCAL_CACHE_DIRECTORY
    var removeUnusedEntriesAfterDays: Int = DEFAULT_LOCAL_UNUSED_ENTRIES_DELETE_AFTER_DAYS
}

private fun LocalCacheExtension?.toConfig(): LocalCacheConfiguration =
    this?.let {
        LocalCacheConfiguration.Enabled(it.directory, it.removeUnusedEntriesAfterDays)
    } ?: LocalCacheConfiguration.Disabled

open class RemoteCacheExtension {
    var url: String? = null
    var credentials: Credentials? = null
}

private fun RemoteCacheExtension?.toConfig(): RemoteCacheConfiguration =
    this?.let {
        val url = it.url ?: throw IllegalArgumentException("Remote cache URL is required for remote cache configuration")
        RemoteCacheConfiguration.Enabled(url, it.credentials)
    } ?: RemoteCacheConfiguration.Disabled

fun CachePluginConfiguration.toCacheConfiguration(): CacheConfiguration {
    return CacheConfiguration(
        local = localExtension.toConfig(),
        remote = remoteExtension.toConfig()
    )
}

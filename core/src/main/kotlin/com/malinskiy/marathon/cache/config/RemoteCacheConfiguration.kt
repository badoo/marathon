package com.malinskiy.marathon.cache.config

sealed class RemoteCacheConfiguration {

    data class Enabled(
        val url: String,
        val credentials: Credentials? = null
    ) : RemoteCacheConfiguration()

    data object Disabled : RemoteCacheConfiguration()
}

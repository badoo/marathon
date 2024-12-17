package com.malinskiy.marathon.cache.config

import java.net.URI

sealed class RemoteCacheConfiguration {

    data class Enabled(
        val url: URI,
        val credentials: Credentials? = null,
        val push: Boolean = true
    ) : RemoteCacheConfiguration()

    data object Disabled : RemoteCacheConfiguration()
}

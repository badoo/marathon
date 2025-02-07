package com.malinskiy.marathon.cache.config

import java.net.URI

sealed class RemoteCacheConfiguration {

    data class Enabled(
        val url: URI,
        val push: Boolean = true,
        val accessKey: String? = null
    ) : RemoteCacheConfiguration()

    data object Disabled : RemoteCacheConfiguration()
}

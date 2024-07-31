package com.malinskiy.marathon.cache.config

import java.net.URI

sealed class RemoteCacheConfiguration {

    data class Enabled(
        val url: URI,
        val credentials: Credentials? = null
    ) : RemoteCacheConfiguration()

    data object Disabled : RemoteCacheConfiguration()
}

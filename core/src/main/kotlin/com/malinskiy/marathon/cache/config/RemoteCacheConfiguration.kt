package com.malinskiy.marathon.cache.config

import dev.zacsweers.redacted.annotations.Redacted
import java.net.URI

sealed class RemoteCacheConfiguration {

    data class Enabled(
        val url: URI,
        val push: Boolean = true,
        @Redacted
        val accessKey: String? = null
    ) : RemoteCacheConfiguration()

    data object Disabled : RemoteCacheConfiguration()
}

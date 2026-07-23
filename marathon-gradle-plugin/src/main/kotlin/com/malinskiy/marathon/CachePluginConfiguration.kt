package com.malinskiy.marathon

import org.gradle.api.Action
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Nested
import java.net.URI

interface CachePluginConfiguration {
    @get:Nested
    val local: LocalCacheExtension

    @get:Nested
    val remote: RemoteCacheExtension

    fun local(action: Action<LocalCacheExtension>) {
        action.execute(local)
    }

    fun remote(action: Action<RemoteCacheExtension>) {
        action.execute(remote)
    }
}

interface LocalCacheExtension {
    val directory: DirectoryProperty
    val enabled: Property<Boolean>
    val removeUnusedEntriesAfterDays: Property<Int>
}

interface RemoteCacheExtension {
    val url: Property<URI>
    val accessKey: Property<String>
    val enabled: Property<Boolean>
    val push: Property<Boolean>
}

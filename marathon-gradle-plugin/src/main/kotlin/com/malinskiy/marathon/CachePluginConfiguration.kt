package com.malinskiy.marathon

import com.malinskiy.marathon.cache.config.Credentials
import com.malinskiy.marathon.cache.config.LocalCacheConfiguration
import com.malinskiy.marathon.cache.config.RemoteCacheConfiguration
import com.malinskiy.marathon.execution.CacheConfiguration
import org.gradle.api.Action
import org.gradle.api.credentials.PasswordCredentials
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
    val credentials: Property<PasswordCredentials>
    val enabled: Property<Boolean>
    val push: Property<Boolean>
}

internal fun CachePluginConfiguration.initDefaults() {
    local.enabled.convention(true)
    local.removeUnusedEntriesAfterDays.convention(7)
    remote.enabled.convention(true)
    remote.push.convention(true)
}

internal fun CachePluginConfiguration.toCacheConfiguration(): CacheConfiguration =
    CacheConfiguration(
        local = local.toConfig(),
        remote = remote.toConfig()
    )

private fun LocalCacheExtension.toConfig(): LocalCacheConfiguration =
    if (directory.isPresent && enabled.get()) {
        LocalCacheConfiguration.Enabled(directory.get().asFile, removeUnusedEntriesAfterDays.get())
    } else {
        LocalCacheConfiguration.Disabled
    }

private fun RemoteCacheExtension.toConfig(): RemoteCacheConfiguration =
    if (url.isPresent && enabled.get()) {
        RemoteCacheConfiguration.Enabled(
            url = url.get(),
            credentials = credentials.orNull?.toCredentials(),
            push = push.get()
        )
    } else {
        RemoteCacheConfiguration.Disabled
    }

private fun PasswordCredentials.toCredentials(): Credentials =
    Credentials(
        userName = requireNotNull(username) { "Username is required" },
        password = requireNotNull(password) { "Password is required" }
    )

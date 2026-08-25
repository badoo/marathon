package com.malinskiy.marathon.cache.test.key

import com.malinskiy.marathon.cache.CacheKey
import com.malinskiy.marathon.device.DevicePoolId
import com.malinskiy.marathon.test.Test
import java.io.OutputStream
import java.security.DigestOutputStream
import java.security.MessageDigest
import java.util.HexFormat

class TestCacheKeyFactory(
    private val componentCacheKeyProvider: ComponentCacheKeyProvider,
    private val versionNameProvider: VersionNameProvider,
) {
    suspend fun getCacheKey(poolId: DevicePoolId, test: Test): CacheKey {
        val digestInputStream = createDigestOutputStream()
        val componentCachingKey = componentCacheKeyProvider.getCacheKey(test.componentInfo)

        digestInputStream
            .bufferedWriter()
            .use {
                it.write(versionNameProvider.versionName)
                it.write(CACHE_FORMAT_VERSION)
                it.write(componentCachingKey)
                it.write(poolId.name)
                it.write(test.pkg)
                it.write(test.clazz)
                it.write(test.method)
            }

        val key = digestInputStream
            .messageDigest
            .digest()
            .let { HexFormat.of().formatHex(it) }

        return TestCacheKey(key, test)
    }

    private fun createDigestOutputStream(): DigestOutputStream = DigestOutputStream(
        object : OutputStream() {
            override fun write(b: Int) {}
        },
        MessageDigest.getInstance("MD5"),
    )

    private companion object {
        private const val CACHE_FORMAT_VERSION = "version:1"
    }
}

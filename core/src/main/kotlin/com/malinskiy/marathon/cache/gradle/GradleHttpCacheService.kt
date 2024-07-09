package com.malinskiy.marathon.cache.gradle

import com.malinskiy.marathon.cache.CacheEntryReader
import com.malinskiy.marathon.cache.CacheEntryWriter
import com.malinskiy.marathon.cache.CacheKey
import com.malinskiy.marathon.cache.CacheService
import com.malinskiy.marathon.cache.config.RemoteCacheConfiguration
import com.malinskiy.marathon.log.MarathonLogging
import io.ktor.client.*
import io.ktor.client.engine.apache.*
import io.ktor.client.plugins.auth.*
import io.ktor.client.plugins.auth.providers.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.content.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.URI
import java.net.URL

class GradleHttpCacheService(private val configuration: RemoteCacheConfiguration.Enabled) : CacheService {

    private val httpClient = createClient()
    private val baseUri = URI.create(configuration.url)

    private val logger = MarathonLogging.logger("GradleHttpCacheService")

    override suspend fun load(key: CacheKey, reader: CacheEntryReader): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val response = httpClient.get(url = key.entryUrl())
                if (response.status != HttpStatusCode.OK) {
                    logger.warn("Got response status when loading cache entry for ${key.key} : ${response.status}")
                    false
                } else {
                    reader.readFrom(response.bodyAsChannel())
                    true
                }
            } catch (exception: IOException) {
                logger.warn("Error during loading cache entry for ${key.key}", exception)
                false
            }
        }

    override suspend fun store(key: CacheKey, writer: CacheEntryWriter) {
        withContext(Dispatchers.IO) {
            val stream = ByteArrayOutputStream()
            try {
                writer.writeTo(stream)
                val response = httpClient.put(url = key.entryUrl()) { setBody(ByteArrayContent(stream.toByteArray())) }
                if (!response.status.isSuccess()) {
                    logger.warn("Got response status when storing cache entry for ${key.key} with ${key.entryUrl()} : ${response.status}")
                }
            } catch (exception: IOException) {
                logger.warn("Error during storing cache entry for ${key.key}", exception)
            } finally {
                stream.close()
            }
        }
    }

    override fun close() {
        httpClient.close()
    }

    private fun CacheKey.entryUrl(): URL =
        baseUri.resolve(this.key).toURL()

    private fun createClient(): HttpClient = HttpClient(Apache) {
        engine {
            followRedirects = true
        }

        expectSuccess = false

        configuration.credentials?.let { credentials ->
            install(Auth) {
                basic {
                    credentials {
                        BasicAuthCredentials(username = credentials.userName, password = credentials.password)
                    }
                }
            }
        }
    }
}

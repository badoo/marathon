package com.malinskiy.marathon.cache.gradle

import com.malinskiy.marathon.cache.CacheEntryReader
import com.malinskiy.marathon.cache.CacheEntryWriter
import com.malinskiy.marathon.cache.CacheKey
import com.malinskiy.marathon.cache.CacheService
import com.malinskiy.marathon.cache.config.RemoteCacheConfiguration
import com.malinskiy.marathon.log.MarathonLogging
import io.ktor.client.HttpClient
import io.ktor.client.engine.apache.Apache
import io.ktor.client.plugins.auth.Auth
import io.ktor.client.plugins.auth.providers.BasicAuthCredentials
import io.ktor.client.plugins.auth.providers.basic
import io.ktor.client.plugins.defaultRequest
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsChannel
import io.ktor.content.ByteArrayContent
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import io.ktor.http.takeFrom
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException

class GradleHttpCacheService(private val configuration: RemoteCacheConfiguration.Enabled) : CacheService {

    private val httpClient = createClient()

    private val logger = MarathonLogging.logger("GradleHttpCacheService")

    override suspend fun load(key: CacheKey, reader: CacheEntryReader): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val response = httpClient.get(key.key)
                if (response.status != HttpStatusCode.OK) {
                    if (response.status != HttpStatusCode.NotFound) {
                        logger.warn("Got response status when loading cache entry for ${key.key} : ${response.status}")
                    }
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
                val response = httpClient.put(key.key) {
                    setBody(ByteArrayContent(stream.toByteArray()))
                }
                if (!response.status.isSuccess()) {
                    logger.warn("Got response status when storing cache entry for ${key.key} : ${response.status}")
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

    private fun createClient(): HttpClient = HttpClient(Apache) {
        defaultRequest {
            url.takeFrom(configuration.url)
        }

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

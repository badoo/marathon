package com.malinskiy.marathon.cache.gradle

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.MountableFile
import java.net.URI
import java.time.Duration

class GradleCacheContainer(image: String = DEFAULT_IMAGE) :
    GenericContainer<GradleCacheContainer>(image) {

    init {
        addExposedPorts(DEFAULT_PORT)
        withCommand("start", "--no-warn-anon-cache-write")
        withCopyFileToContainer(MountableFile.forClasspathResource("config.yaml"), "/data/conf/config.yaml")
        withLogConsumer {
            logger().info(it.utf8String)
        }
        waitStrategy = Wait
            .forLogMessage(".*Build cache node started(?s).*", 1)
            .withStartupTimeout(Duration.ofSeconds(DEFAULT_STARTUP_TIMEOUT_SECONDS))
    }

    val cacheUrl: URI
        get() = URI.create("http://$host:$httpPort/cache/")

    private val httpPort: Int
        get() = getMappedPort(DEFAULT_PORT)

    private companion object {
        private const val DEFAULT_IMAGE = "gradle/build-cache-node:21.2"
        private const val DEFAULT_PORT = 5071
        private const val DEFAULT_STARTUP_TIMEOUT_SECONDS = 60L
    }
}

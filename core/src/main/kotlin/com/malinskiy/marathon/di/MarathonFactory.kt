package com.malinskiy.marathon.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.malinskiy.marathon.Marathon
import com.malinskiy.marathon.analytics.TrackerFactory
import com.malinskiy.marathon.analytics.external.AnalyticsFactory
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.cache.CacheServiceFactory
import com.malinskiy.marathon.cache.test.CacheTestReporter
import com.malinskiy.marathon.cache.test.CacheTestResultsTracker
import com.malinskiy.marathon.cache.test.TestCacheLoader
import com.malinskiy.marathon.cache.test.TestCacheSaver
import com.malinskiy.marathon.cache.test.TestResultsCache
import com.malinskiy.marathon.cache.test.key.TestCacheKeyFactory
import com.malinskiy.marathon.cache.test.key.VersionNameProvider
import com.malinskiy.marathon.execution.Configuration
import com.malinskiy.marathon.execution.ConfigurationStrictRunChecker
import com.malinskiy.marathon.execution.Scheduler
import com.malinskiy.marathon.execution.progress.ProgressReporter
import com.malinskiy.marathon.io.AttachmentManager
import com.malinskiy.marathon.io.CachedFileHasher
import com.malinskiy.marathon.io.DefaultTempFileFactory
import com.malinskiy.marathon.io.FileManager
import com.malinskiy.marathon.io.Md5FileHasher
import com.malinskiy.marathon.json.FileSerializer
import com.malinskiy.marathon.time.SystemTimer
import com.malinskiy.marathon.time.Timer
import com.malinskiy.marathon.vendor.VendorDependencies
import java.io.File
import java.time.Clock

@Suppress("LongMethod")
fun createMarathon(configuration: Configuration, timer: Timer? = null): Marathon {
    val marathonTimer = timer ?: SystemTimer(Clock.systemDefaultZone())
    val track = Track()
    val fileManager = FileManager(configuration.outputDir)
    val tempFileFactory = DefaultTempFileFactory(configuration.tempDir)
    val attachmentManager = AttachmentManager(configuration.outputDir, tempFileFactory)
    val fileHasher = CachedFileHasher(Md5FileHasher())
    val progressReporter = ProgressReporter(configuration.strictMode)
    val strictRunChecker = ConfigurationStrictRunChecker(configuration)

    val vendorComponents = configuration.vendorConfiguration.createComponents(
        VendorDependencies(
            configuration = configuration,
            track = track,
            timer = marathonTimer,
            fileManager = fileManager,
            attachmentManager = attachmentManager,
            tempFileFactory = tempFileFactory,
            fileHasher = fileHasher,
            strictRunChecker = strictRunChecker
        )
    )

    val cacheService = CacheServiceFactory(configuration).createCacheService()
    val testCacheKeyFactory = TestCacheKeyFactory(vendorComponents.componentCacheKeyProvider, VersionNameProvider())
    val testResultsCache = TestResultsCache(cacheService, attachmentManager, track)
    val testCacheLoader = TestCacheLoader(testResultsCache, testCacheKeyFactory, configuration.strictRunConfiguration)
    val testCacheSaver = TestCacheSaver(testResultsCache, testCacheKeyFactory)

    val tracker = TrackerFactory(
        configuration = configuration,
        fileManager = fileManager,
        attachmentManager = attachmentManager,
        cacheTestResultsTracker = CacheTestResultsTracker(testCacheSaver),
        logsProvider = vendorComponents.logsProvider,
        gson = createGson(),
        timer = marathonTimer,
        track = track
    ).create()

    val analytics = AnalyticsFactory().create()

    val scheduler = Scheduler(
        deviceProvider = vendorComponents.deviceProvider,
        cacheService = cacheService,
        cacheLoader = testCacheLoader,
        cacheSaver = testCacheSaver,
        cachedTestsReporter = CacheTestReporter(progressReporter, track),
        analytics = analytics,
        configuration = configuration,
        progressReporter = progressReporter,
        strictRunChecker = strictRunChecker,
        logsProvider = vendorComponents.logsProvider,
        track = track,
        timer = marathonTimer
    )

    return Marathon(
        configuration = configuration,
        tracker = tracker,
        analytics = analytics,
        testParser = vendorComponents.testParser,
        progressReporter = progressReporter,
        scheduler = scheduler
    )
}

private fun createGson(): Gson = GsonBuilder()
    .registerTypeAdapter(File::class.java, FileSerializer())
    .create()

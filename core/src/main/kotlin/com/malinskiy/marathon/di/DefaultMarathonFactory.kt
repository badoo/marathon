package com.malinskiy.marathon.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.malinskiy.marathon.Marathon
import com.malinskiy.marathon.MarathonFactory
import com.malinskiy.marathon.analytics.TrackerFactory
import com.malinskiy.marathon.analytics.external.AnalyticsFactory
import com.malinskiy.marathon.analytics.internal.pub.Track
import com.malinskiy.marathon.cache.CacheServiceFactory
import com.malinskiy.marathon.cache.DefaultCacheServiceFactory
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
import kotlinx.coroutines.CoroutineDispatcher
import java.io.File
import java.time.Clock

class DefaultMarathonFactory(
    private val ioDispatcher: CoroutineDispatcher,
    private val cacheServiceFactory: CacheServiceFactory = DefaultCacheServiceFactory(ioDispatcher),
    private val timer: Timer = SystemTimer(Clock.systemDefaultZone())
) : MarathonFactory {

    @Suppress("LongMethod")
    override fun createMarathon(configuration: Configuration): Marathon {
        val analytics = AnalyticsFactory().create()
        val fileManager = FileManager(configuration.outputDir)
        val tempFileFactory = DefaultTempFileFactory(configuration.tempDir)
        val attachmentManager = AttachmentManager(configuration.outputDir, tempFileFactory)
        val fileHasher = CachedFileHasher(Md5FileHasher(ioDispatcher))
        val progressReporter = ProgressReporter(configuration.strictMode)
        val strictRunChecker = ConfigurationStrictRunChecker(configuration)
        val track = Track()
        val versionNameProvider = VersionNameProvider()

        val vendorComponents = configuration.vendorConfiguration.createComponents(
            VendorDependencies(
                attachmentManager = attachmentManager,
                configuration = configuration,
                fileHasher = fileHasher,
                fileManager = fileManager,
                ioDispatcher = ioDispatcher,
                strictRunChecker = strictRunChecker,
                tempFileFactory = tempFileFactory,
                timer = timer,
                track = track
            )
        )

        val cacheService = cacheServiceFactory.createCacheService(configuration.cache)
        val testCacheKeyFactory = TestCacheKeyFactory(vendorComponents.componentCacheKeyProvider, versionNameProvider)
        val testResultsCache = TestResultsCache(cacheService, attachmentManager, ioDispatcher, track)
        val testCacheLoader = TestCacheLoader(testResultsCache, testCacheKeyFactory, configuration.strictRunConfiguration)
        val testCacheSaver = TestCacheSaver(testResultsCache, testCacheKeyFactory, ioDispatcher)
        val cachedTestsReporter = CacheTestReporter(progressReporter, track)
        val cacheTestResultsTracker = CacheTestResultsTracker(testCacheSaver)

        val tracker = TrackerFactory(
            configuration = configuration,
            fileManager = fileManager,
            attachmentManager = attachmentManager,
            cacheTestResultsTracker = cacheTestResultsTracker,
            logsProvider = vendorComponents.logsProvider,
            gson = createGson(),
            timer = timer,
            track = track
        ).create()

        val scheduler = Scheduler(
            deviceProvider = vendorComponents.deviceProvider,
            cacheService = cacheService,
            cacheLoader = testCacheLoader,
            cacheSaver = testCacheSaver,
            cachedTestsReporter = cachedTestsReporter,
            analytics = analytics,
            configuration = configuration,
            progressReporter = progressReporter,
            strictRunChecker = strictRunChecker,
            logsProvider = vendorComponents.logsProvider,
            track = track,
            timer = timer
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
}

package com.malinskiy.marathon.android.di

import com.malinskiy.marathon.android.AndroidComponentCacheKeyProvider
import com.malinskiy.marathon.android.AndroidComponentInfoExtractor
import com.malinskiy.marathon.android.AndroidTestParser
import com.malinskiy.marathon.android.ApkFileHasher
import com.malinskiy.marathon.android.executor.logcat.LogcatCollector
import com.malinskiy.marathon.android.executor.logcat.LogcatListener
import com.malinskiy.marathon.android.executor.logcat.parse.LogcatEventsAdapter
import com.malinskiy.marathon.android.executor.logcat.parse.LogcatEventsListener
import com.malinskiy.marathon.cache.test.key.ComponentCacheKeyProvider
import com.malinskiy.marathon.execution.ComponentInfoExtractor
import com.malinskiy.marathon.execution.TestParser
import com.malinskiy.marathon.io.CachedFileHasher
import com.malinskiy.marathon.report.logs.LogsProvider
import org.koin.dsl.module

val androidModule = module {
    single<TestParser?> { AndroidTestParser() }
    single<ComponentInfoExtractor?> { AndroidComponentInfoExtractor() }
    single<ComponentCacheKeyProvider?> { AndroidComponentCacheKeyProvider(CachedFileHasher(ApkFileHasher())) }
    single<LogcatCollector?> { LogcatCollector() }
    single<LogcatEventsListener?> { get<LogcatCollector>() }
    single<LogsProvider?> { get<LogcatCollector>() }
    single<LogcatListener?> { LogcatEventsAdapter(get()) }
}

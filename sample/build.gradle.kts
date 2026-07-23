import com.malinskiy.marathon.SerialStrategyConfiguration

plugins {
    id("com.android.application") apply false
    id("com.android.library") apply false
    id("com.badoo.marathon")
}

marathon {
    ignoreFailures = true

    serialStrategy = SerialStrategyConfiguration.DDMS
    retryStrategy {
        fixedQuota {}
    }
}

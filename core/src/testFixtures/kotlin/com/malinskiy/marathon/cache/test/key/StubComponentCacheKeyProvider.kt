package com.malinskiy.marathon.cache.test.key

import com.malinskiy.marathon.execution.ComponentInfo

class StubComponentCacheKeyProvider : ComponentCacheKeyProvider {
    override suspend fun getCacheKey(componentInfo: ComponentInfo): String = componentInfo.name
}

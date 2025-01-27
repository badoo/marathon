package com.malinskiy.marathon

import com.malinskiy.marathon.analytics.internal.pub.Tracker
import com.malinskiy.marathon.execution.MarathonListener
import com.malinskiy.marathon.execution.TestOwnerProvider

object MarathonListenerHolder {
    var analyticsTracker: Tracker? = null
    var listener: MarathonListener? = null
    var testOwnerProvider: TestOwnerProvider? = null
}

package com.malinskiy.marathon.test

import com.malinskiy.marathon.log.MarathonLogConfigurator
import com.malinskiy.marathon.vendor.VendorConfiguration

class StubMarathonLogConfigurator : MarathonLogConfigurator {
    override fun configure(vendorConfiguration: VendorConfiguration) = Unit
}

package com.neerly.mobile.feature.account

import org.junit.Assert.assertEquals
import org.junit.Test

class DriverFacingNameTest {
    @Test fun firstNamePlusLastInitial() = assertEquals("Shiva S.", driverFacingName("Shiva Sai"))
    @Test fun usesTheLastWordForTheInitial() = assertEquals("Shiva D.", driverFacingName("  Shiva  sai donthula "))
    @Test fun singleNameStaysAsIs() = assertEquals("Ravi", driverFacingName("Ravi"))
    @Test fun blankGivesNothing() = assertEquals("", driverFacingName("   "))
}

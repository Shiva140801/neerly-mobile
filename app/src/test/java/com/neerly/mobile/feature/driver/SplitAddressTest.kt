package com.neerly.mobile.feature.driver

import org.junit.Assert.assertEquals
import org.junit.Test

class SplitAddressTest {
    @Test fun shortFlatNumber_takesTheBuildingWithIt() =
        assertEquals("204, Lakshmi Residency" to "Gachibowli 500032", splitAddress("204, Lakshmi Residency, Gachibowli 500032"))
    @Test fun longFirstPart_standsAlone() =
        assertEquals("H.No 8-2-293 Road No. 10" to "Jubilee Hills", splitAddress("H.No 8-2-293 Road No. 10, Jubilee Hills"))
    @Test fun noComma_wholeIsTitle() = assertEquals("Film Nagar" to null, splitAddress("Film Nagar"))
}

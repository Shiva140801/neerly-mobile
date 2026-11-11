package com.neerly.mobile.feature.vendor.catalog

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class CustomProductFormTest {

    @Test fun ice_defaultsToKgBag_andSuggestsTheName() {
        val f = CustomProductForm().withCategory(CustomCategory.ICE).copy(price = "40")
        assertEquals(CustomUnit.KG, f.unit)
        assertEquals(Packaging.BAG, f.packaging)
        assertEquals("Ice cubes · 1 kg bag", f.suggestedName)
        assertTrue(f.valid)
        val body = f.body()!!
        assertEquals("ICE", body.category); assertEquals("KG", body.unit); assertEquals("BAG", body.packaging)
        assertNull(body.depositAmount); assertFalse(body.allowEmptySwap)
    }

    @Test fun ice_isNeverAReturnableJar() {
        assertFalse(Packaging.RETURNABLE in CustomProductForm.packagings(CustomCategory.ICE, CustomUnit.KG))
    }

    @Test fun sealedCase_carriesThePackCount() {
        val f = CustomProductForm().withCategory(CustomCategory.WATER).withUnit(CustomUnit.ML)
            .copy(kind = "MINERAL", size = "500", packaging = Packaging.SEALED, packCount = 24, price = "240")
        assertEquals("Mineral water · 500 ml × 24", f.suggestedName)
        assertEquals(24, f.body()!!.packCount)
    }

    @Test fun sizeOutsideTheRange_isRefused() {
        val f = CustomProductForm().withCategory(CustomCategory.ICE).copy(size = "80", price = "40")
        assertFalse(f.sizeOk); assertFalse(f.valid)
    }

    @Test fun returnableJar_needsASwapOrADeposit() {
        val jar = CustomProductForm().withCategory(CustomCategory.WATER).copy(size = "20", packaging = Packaging.RETURNABLE, price = "90")
        assertTrue(jar.valid)
        assertFalse(jar.copy(swap = false).valid)
        assertTrue(jar.copy(swap = false, deposit = "250").valid)
        assertEquals(BigDecimal("250"), jar.copy(deposit = "250").body()!!.depositAmount)
    }

    @Test fun somethingElse_needsAName() {
        val f = CustomProductForm().withCategory(CustomCategory.OTHER).copy(size = "1", price = "300")
        assertFalse(f.valid)
        assertTrue(f.copy(kind = "dispenser stand").valid)
    }

    @Test fun editing_prefillsFromTheProduct() {
        val p = ShopProduct(id = "p1", template = ShopTemplate("t", "CUSTOM_ICE", "Ice cubes · 1 kg bag"), price = BigDecimal("40.00"),
            dailyCapacity = 30, status = "AVAILABLE",
            custom = ShopCustomInfo("ICE", "CUBES", "KG", BigDecimal("1.00"), "BAG", null, null))
        val f = CustomProductForm.from(p)!!
        assertEquals(CustomCategory.ICE, f.category); assertEquals("1", f.size); assertEquals("40", f.price)
        assertEquals("Ice cubes · 1 kg bag", p.name)
    }
}

package com.neerly.mobile.feature.vendor.catalog

import androidx.lifecycle.SavedStateHandle
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class VendorCatalogViewModelsTest {
    private val api: VendorCatalogApi = mockk()
    private val tpl = ShopTemplate("t1", "JAR_20L_NORMAL", "20L Jar (Normal)", allowsDeposit = true)
    private fun product(status: String = "AVAILABLE", keep: Boolean = true) = ShopProduct(
        id = "p1", template = tpl, price = BigDecimal("85.00"), dailyCapacity = 50, status = status,
        allowKeepContainer = keep, allowTransferAndReturn = true, depositAmount = BigDecimal("300.00"), retentionHours = 72)

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun switchOff_withBackOnDay_sendsThatDayAndShowsTheServerCopy() {
        coEvery { api.products() } returns listOf(product())
        coEvery { api.pause("p1", PauseBody(true, "2026-11-08")) } returns product("PAUSED").copy(pausedUntil = "2026-11-08")
        val vm = VendorCatalogViewModel(api, com.neerly.mobile.support.TestStrings)
        vm.switchOff(vm.state.value.products.single(), LocalDate.of(2026, 11, 8))
        assertEquals("PAUSED", vm.state.value.products.single().status)
        assertEquals("2026-11-08", vm.state.value.products.single().pausedUntil)
        assertNull(vm.state.value.saving)
    }

    @Test
    fun switchOn_failure_keepsTheRowAndSaysSo() {
        coEvery { api.products() } returns listOf(product("PAUSED"))
        coEvery { api.pause(any(), any()) } throws RuntimeException("boom")
        val vm = VendorCatalogViewModel(api, com.neerly.mobile.support.TestStrings)
        vm.switchOn(vm.state.value.products.single())
        assertEquals("PAUSED", vm.state.value.products.single().status)
        assertNotNull(vm.state.value.error)
    }

    @Test
    fun productEdit_sendsOnlyChangedFields() {
        coEvery { api.products() } returns listOf(product())
        coEvery { api.update(any(), any()) } returns product().copy(price = BigDecimal("90"))
        val vm = VendorProductEditViewModel(api, SavedStateHandle(mapOf("productId" to "p1")), com.neerly.mobile.support.TestStrings)
        assertNull("nothing changed yet", vm.patch())
        vm.setPrice("90")
        vm.save()
        coVerify { api.update("p1", ProductPatch(price = BigDecimal("90"))) }
        assertTrue(vm.state.value.saved)
    }

    @Test
    fun productEdit_blankPrice_isNotValid() {
        coEvery { api.products() } returns listOf(product())
        val vm = VendorProductEditViewModel(api, SavedStateHandle(mapOf("productId" to "p1")), com.neerly.mobile.support.TestStrings)
        vm.setPrice("")
        assertFalse(vm.state.value.valid)
    }

    @Test
    fun addProduct_keepNeedsADeposit_andAtLeastOneWay() {
        coEvery { api.templates() } returns listOf(tpl)
        coEvery { api.products() } returns emptyList()
        val vm = VendorAddProductViewModel(api, com.neerly.mobile.support.TestStrings)
        vm.pick(tpl)
        vm.update { copy(price = "60", keep = true, swap = false) }
        assertNull("deposit missing", vm.request())
        vm.update { copy(deposit = "250") }
        val req = vm.request()!!
        assertEquals("JAR_20L_NORMAL", req.templateCategoryCode)
        assertTrue(req.allowKeepContainer); assertFalse(req.allowTransferAndReturn)
        assertEquals(72, req.retentionHours)
        vm.update { copy(keep = false, swap = false) }
        assertNull("no way to take the jar", vm.request())
    }
}

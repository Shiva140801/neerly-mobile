package com.neerly.mobile.feature.vendor.onboarding

import com.neerly.mobile.data.dto.VendorResponse
import com.neerly.mobile.data.repo.VendorRepository
import com.neerly.mobile.feature.address.AddressLocator
import com.neerly.mobile.feature.address.FoundPlace
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class VendorOnboardingViewModelTest {
    private val repo: VendorRepository = mockk(relaxed = true)
    private val setup: VendorSetupApi = mockk(relaxed = true)

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun locator(fix: Pair<Double, Double>?) = object : AddressLocator {
        override suspend fun currentFix() = fix
        override suspend fun reverse(lat: Double, lng: Double) = FoundPlace("Film Nagar Main Rd", "500096", "Hyderabad", "Shop 3, Film Nagar")
    }

    private fun vendor(status: String = "PENDING_VERIFICATION") = VendorResponse(
        "v1", "u1", "Sri Balaji Water", "Venkatesh Reddy", "PROPRIETOR", listOf("WATER_SUPPLIER"), "TIER_1", status,
        "Hyderabad", "500096", null, null, null, 0, "2026-11-07T04:18:00Z", null, listOf("500096"))

    private fun http(code: Int) = HttpException(Response.error<Any>(code, "{}".toResponseBody()))

    private fun noVendorVm(fix: Pair<Double, Double>? = 17.41 to 78.41): VendorOnboardingViewModel {
        coEvery { repo.me() } throws http(404)
        return VendorOnboardingViewModel(repo, setup, locator(fix), io.mockk.mockk(relaxed = true), com.neerly.mobile.support.TestStrings)
    }

    private fun VendorOnboardingViewModel.fillShop() {
        useMyLocation()
        edit { copy(name = "Sri Balaji Water", owner = "Venkatesh Reddy") }
    }

    @Test
    fun applicationAlreadyOnFile_skipsToDocuments() {
        coEvery { repo.me() } returns vendor()
        val vm = VendorOnboardingViewModel(repo, setup, locator(null), io.mockk.mockk(relaxed = true), com.neerly.mobile.support.TestStrings)
        assertEquals(SetupStep.DOCS, vm.state.value.step)
    }

    @Test
    fun shopStep_needsARealPinBeforeMovingOn() {
        val vm = noVendorVm(fix = null)
        vm.go(SetupStep.SHOP)
        vm.edit { copy(name = "Sri Balaji Water", owner = "Venkatesh Reddy", street = "Film Nagar", pincode = "500096") }
        vm.useMyLocation()
        vm.nextFromShop()
        assertEquals(SetupStep.SHOP, vm.state.value.step)
        assertNotNull(vm.state.value.locationError)
        assertTrue(vm.state.value.error!!.contains("location"))
    }

    @Test
    fun gpsFix_fillsStreetAndPincode() {
        val vm = noVendorVm()
        vm.fillShop()
        val f = vm.state.value.form
        assertEquals("500096", f.pincode)
        assertEquals("Film Nagar Main Rd", f.street)
        assertNull(f.shopProblem())
    }

    @Test
    fun pinOutsideIndia_isRefused() {
        val vm = noVendorVm(fix = 37.42 to -122.08)
        vm.useMyLocation()
        assertNull(vm.state.value.form.lat)
        assertNotNull(vm.state.value.locationError)
    }

    @Test
    fun submit_sendsTheShopAndSavesHours_thenDocuments() {
        val vm = noVendorVm()
        vm.fillShop()
        coEvery { repo.submitOnboarding(any()) } returns vendor()
        vm.submit()
        assertEquals(SetupStep.DOCS, vm.state.value.step)
        coVerify { repo.submitOnboarding(match { it.businessLatitude != null && it.serviceablePincodes == setOf("500096") && it.isTier2 }) }
        coVerify { repo.replaceHours(match { rows -> rows.size == 7 && rows.last().closeTime == "13:00:00" }) }
    }

    @Test
    fun submitAsCustomer_explainsSellerSignIn() {
        val vm = noVendorVm()
        vm.fillShop()
        coEvery { repo.submitOnboarding(any()) } throws http(403)
        vm.submit()
        assertTrue(vm.state.value.needsSellerSignIn)
        assertNull(vm.state.value.vendor)
        assertTrue(vm.state.value.error!!.contains("seller"))
    }

    @Test
    fun fssaiNumberWithoutExpiry_blocksSubmit() {
        val vm = noVendorVm()
        vm.fillShop()
        vm.edit { copy(fssaiNumber = "10019011000256") }
        vm.submit()
        coVerify(exactly = 0) { repo.submitOnboarding(any()) }
        assertTrue(vm.state.value.error!!.contains("valid till"))
    }

    @Test
    fun products_needAPrice_andUseTheJarRules() {
        coEvery { repo.me() } returns vendor()
        coEvery { setup.templates() } returns listOf(
            ProductTemplate("t1", "JAR_20L_NORMAL", "20L Jar (Normal)", allowsDeposit = true),
            ProductTemplate("t2", "BOTTLE_1L", "1L Bottle"),
            ProductTemplate("t3", "TANKER_5000L", "5000L Tanker"))
        coEvery { setup.products() } returns emptyList()
        val vm = VendorOnboardingViewModel(repo, setup, locator(null), io.mockk.mockk(relaxed = true), com.neerly.mobile.support.TestStrings)
        vm.go(SetupStep.PRODUCTS)
        assertEquals(2, vm.state.value.templates.size)          // tankers aren't a jar shop's first product
        vm.pick("JAR_20L_NORMAL") { copy(on = true) }
        vm.saveProducts()
        assertTrue(vm.state.value.error!!.contains("price"))
        vm.pick("JAR_20L_NORMAL") { copy(price = "60") }
        vm.setDeposit("150")
        vm.saveProducts()
        coVerify { setup.createProduct(match { it.templateCategoryCode == "JAR_20L_NORMAL" && it.allowTransferAndReturn &&
            it.depositAmount?.toInt() == 150 && it.price.toInt() == 60 }) }
        assertEquals(SetupStep.REVIEW, vm.state.value.step)
    }

    @Test
    fun hoursRows_followSundayChoice() {
        val f = ShopForm(opens = "06:00", closes = "21:00")
        assertEquals(6, f.copy(sunday = SundayMode.CLOSED).hoursRows().size)
        assertEquals("21:00:00", f.copy(sunday = SundayMode.FULL).hoursRows().last().closeTime)
        assertNotNull(f.copy(opens = "22:00").areaProblem())
        assertFalse(ShopForm(pincode = "500096").pincodes.isEmpty())
    }
}

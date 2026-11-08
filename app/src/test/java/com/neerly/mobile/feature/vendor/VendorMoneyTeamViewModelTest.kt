package com.neerly.mobile.feature.vendor

import com.neerly.mobile.data.dto.AddVendorBankAccountRequest
import com.neerly.mobile.data.dto.EarningsBucket
import com.neerly.mobile.data.dto.EarningsSummary
import com.neerly.mobile.data.dto.VendorBankAccountResponse
import com.neerly.mobile.data.repo.VendorRepository
import com.neerly.mobile.feature.vendor.bank.VendorBankViewModel
import com.neerly.mobile.feature.vendor.earnings.EarningsPeriod
import com.neerly.mobile.feature.vendor.earnings.VendorEarningsViewModel
import com.neerly.mobile.feature.vendor.money.EarningLine
import com.neerly.mobile.feature.vendor.money.EarningPage
import com.neerly.mobile.feature.vendor.money.VendorMoneyApi
import com.neerly.mobile.feature.vendor.money.VendorPayout
import com.neerly.mobile.feature.vendor.money.periodLabel
import com.neerly.mobile.feature.vendor.team.CashConfirmation
import com.neerly.mobile.feature.vendor.team.CashToday
import com.neerly.mobile.feature.vendor.team.DriverLookup
import com.neerly.mobile.feature.vendor.team.TeamDriver
import com.neerly.mobile.feature.vendor.team.VendorTeamApi
import com.neerly.mobile.feature.vendor.team.VendorTeamViewModel
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

@OptIn(ExperimentalCoroutinesApi::class)
class VendorMoneyTeamViewModelTest {
    private val repo: VendorRepository = mockk(relaxed = true)
    private val money: VendorMoneyApi = mockk()
    private val team: VendorTeamApi = mockk()

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun b(net: String) = EarningsBucket(BigDecimal("100"), BigDecimal("10"), BigDecimal("1.8"), BigDecimal(net), 2)
    private fun payout(id: String, status: String) = VendorPayout(id, "2026-10-26", "2026-11-01", BigDecimal("100"), BigDecimal("10"),
        BigDecimal("1.8"), BigDecimal("88.2"), status, failureReason = if (status == "FAILED") "Account frozen" else null, createdAt = "2026-11-02T04:32:00Z")
    private fun line(id: String, type: String, net: String) = EarningLine(id, "o$id", BigDecimal(net), BigDecimal.ZERO, BigDecimal(net), "2026-11-07", entryType = type)

    @Test
    fun earnings_splitsTripPayAndLeadsWithStuckPayouts() {
        coEvery { repo.earnings() } returns EarningsSummary(b("88.2"), b("500"), b("900"), "2026-11-09T04:30:00Z", BigDecimal("900"))
        coEvery { money.unpaid() } returns com.neerly.mobile.feature.vendor.money.UnpaidSummary(3, BigDecimal("1000"), BigDecimal("900"), "2026-11-02", "2026-11-08")
        coEvery { money.payouts(any(), any()) } returns listOf(payout("p1", "FAILED"), payout("p2", "COMPLETED"))
        coEvery { money.lines(any(), any()) } returns EarningPage(listOf(line("a", "LATE_CANCEL_TRIP", "25"), line("b", "ORDER", "97.02")))
        val vm = VendorEarningsViewModel(repo, money, com.neerly.mobile.support.TestStrings)
        val s = vm.state.value
        assertEquals(listOf("p1"), s.stuck.map { it.id })
        assertEquals(BigDecimal("25"), s.tripPayTotal)
        vm.select(EarningsPeriod.WEEK)
        assertEquals(BigDecimal("500"), vm.state.value.bucket?.net)
    }

    @Test
    fun periodLabel_sameAndAcrossMonths() {
        assertEquals("2–8 Nov", periodLabel("2026-11-02", "2026-11-08"))
        assertEquals("26 Oct – 1 Nov", periodLabel("2026-10-26", "2026-11-01"))
    }

    @Test
    fun bank_validationNeedsMatchingNumbersAndIfsc() {
        val ok = AddVendorBankAccountRequest("Venkatesh Reddy", "123456789012", "SBIN0020281")
        assertNull(VendorBankViewModel.validate(ok, "123456789012"))
        assertNotNull(VendorBankViewModel.validate(ok, "123456789013"))
        assertNotNull(VendorBankViewModel.validate(ok.copy(ifsc = "SBIN20281"), "123456789012"))
    }

    @Test
    fun bank_autoMarkedManualAccountIsNotCalledChecked() {
        val a = VendorBankAccountResponse("1", "R K", "•••• 1234", "HDFC0001234", "HDFC", null, "SAVINGS", true,
            "2026-08-23T12:41:34Z", "MANUAL", "2026-08-23T12:41:34Z", null)
        assertFalse(VendorBankViewModel.checked(a))
        assertTrue(VendorBankViewModel.checked(a.copy(verificationMethod = "PENNY_DROP")))
    }

    @Test
    fun invite_looksUpBeforeAddingAndRefusesUnknownNumbers() {
        coEvery { team.team() } returns emptyList()
        coEvery { team.cashToday() } returns emptyList()
        coEvery { team.lookup("9949000218") } returns DriverLookup(exists = false)
        val vm = VendorTeamViewModel(repo, team, com.neerly.mobile.support.TestStrings)
        vm.startInvite(); vm.typePhone("+91 99490 00218"); vm.findDriver()
        assertEquals(false, vm.state.value.invite?.found?.exists)
        vm.addFound(null)
        coVerify(exactly = 0) { repo.addDriver(any(), any()) }
    }

    @Test
    fun invite_addsAFoundDriverWithCountryCode() {
        coEvery { team.team() } returns emptyList()
        coEvery { team.cashToday() } returns emptyList()
        coEvery { team.lookup("9949000218") } returns DriverLookup(exists = true, nameMasked = "Naveen K.")
        val vm = VendorTeamViewModel(repo, team, com.neerly.mobile.support.TestStrings)
        vm.startInvite(); vm.typePhone("9949000218"); vm.findDriver(); vm.addFound("mornings")
        coVerify { repo.addDriver("+919949000218", "mornings") }
        assertTrue(vm.state.value.invite?.added == true)
    }

    @Test
    fun cashCount_showsLiveShortfallAndSavesTheCount() {
        val row = CashToday("d1", "Ramesh Kumar", "2026-11-07", 4, BigDecimal("1240"), 3)
        coEvery { team.team() } returns listOf(TeamDriver("m1", "d1", "Ramesh Kumar"))
        coEvery { team.cashToday() } returns listOf(row)
        coEvery { team.confirmCash("d1", any()) } returns CashConfirmation("c1", "2026-11-07", BigDecimal("1240"), BigDecimal("1200"),
            BigDecimal("40"), 3, 2, 1, true, null, "2026-11-07T14:10:00Z")
        val vm = VendorTeamViewModel(repo, team, com.neerly.mobile.support.TestStrings)
        vm.startCount("d1")
        assertEquals(3, vm.state.value.count?.empties)
        vm.typeCash("1200"); vm.empties(-1)
        assertEquals(BigDecimal("40"), vm.state.value.count?.cashShort)
        assertEquals(1, vm.state.value.count?.emptiesShort)
        vm.confirmCount()
        coVerify { team.confirmCash("d1", match { it.cashReceived == BigDecimal("1200") && it.emptiesReceived == 2 }) }
        assertTrue(vm.state.value.count?.result?.shortfall == true)
    }

    @Test
    fun cashCount_refusesABlankAmount() {
        coEvery { team.team() } returns emptyList()
        coEvery { team.cashToday() } returns listOf(CashToday("d1", "R", "2026-11-07", 1, BigDecimal("80"), 0))
        val vm = VendorTeamViewModel(repo, team, com.neerly.mobile.support.TestStrings)
        vm.startCount("d1"); vm.confirmCount()
        assertNotNull(vm.state.value.count?.problem)
        coVerify(exactly = 0) { team.confirmCash(any(), any()) }
    }
}

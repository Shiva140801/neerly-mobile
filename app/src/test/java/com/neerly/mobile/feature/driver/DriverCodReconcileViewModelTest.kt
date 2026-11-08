package com.neerly.mobile.feature.driver

import com.neerly.mobile.data.repo.DriverRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class DriverCodReconcileViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val repo: DriverRepository = mockk()
    private val ops: DriverOpsApi = mockk()
    private val platform: DriverPlatformApi = mockk()

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { Dispatchers.resetMain() }

    @Test
    fun collectedComesFromTheServer_driverOnlyEntersWhatTheyGave() = runTest(dispatcher) {
        coEvery { ops.today() } returns DriverToday(BigDecimal("140"), 4, BigDecimal("780"))
        coEvery { repo.assignments() } returns emptyList()
        coEvery { platform.reconcile(any()) } returns retrofit2.Response.success(Unit)
        val vm = DriverCodReconcileViewModel(repo, ops, platform, com.neerly.mobile.support.TestStrings); advanceUntilIdle()
        assertEquals(BigDecimal("780"), vm.state.value.collected)
        vm.submit(BigDecimal("760"), "₹20 change given", mapOf("o7" to BigDecimal("180"))); advanceUntilIdle()
        coVerify { platform.reconcile(match { it.collected == BigDecimal("780") && it.handedOver == BigDecimal("760") &&
            it.notes == "₹20 change given" && it.lines?.single()?.orderId == "o7" }) }
        assertTrue(vm.state.value.saved)
    }
}

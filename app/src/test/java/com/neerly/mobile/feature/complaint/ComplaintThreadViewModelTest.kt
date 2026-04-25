package com.neerly.mobile.feature.complaint

import androidx.lifecycle.SavedStateHandle
import com.neerly.mobile.data.dto.ComplaintMessageDto
import com.neerly.mobile.data.dto.ComplaintResponse
import com.neerly.mobile.data.repo.CustomerRepository
import io.mockk.coEvery
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
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ComplaintThreadViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo: CustomerRepository = mockk()

    private val sample = ComplaintResponse(
        id = "c1", customerId = "u1", vendorId = "v1", orderId = "o1",
        category = "LATE_DELIVERY", subject = "Late by 2h", description = "Slot 10-11am",
        evidencePhotos = emptyList(), status = "TRIAGED", priority = "HIGH",
        slaDeadline = "2026-04-26T10:00:00Z", slaBreached = false,
        filedAt = "2026-04-25T10:00:00Z",
        messages = listOf(
            ComplaintMessageDto("m1", "u1", "CUSTOMER", "Hello", emptyList(), false, "2026-04-25T10:01:00Z")
        )
    )

    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After  fun tearDown() { Dispatchers.resetMain() }

    private fun vm() = ComplaintThreadViewModel(repo, SavedStateHandle(mapOf("complaintId" to "c1")))

    @Test
    fun load_populatesComplaint() = runTest(dispatcher) {
        coEvery { repo.complaint("c1") } returns sample
        val v = vm()
        advanceUntilIdle()
        assertEquals("Late by 2h", v.state.value.complaint?.subject)
    }

    @Test
    fun appendMessage_appendsLocallyAfterServerEcho() = runTest(dispatcher) {
        coEvery { repo.complaint("c1") } returns sample
        coEvery { repo.appendComplaintMessage("c1", "Any update?") } returns
            ComplaintMessageDto("m2", "u1", "CUSTOMER", "Any update?",
                emptyList(), false, "2026-04-25T10:05:00Z")

        val v = vm()
        advanceUntilIdle()
        v.appendMessage("Any update?")
        advanceUntilIdle()

        assertEquals(2, v.state.value.complaint?.messages?.size)
        assertEquals("Any update?", v.state.value.complaint?.messages?.last()?.message)
    }

    @Test
    fun appendMessage_blank_isNoOp() = runTest(dispatcher) {
        coEvery { repo.complaint("c1") } returns sample
        val v = vm()
        advanceUntilIdle()
        v.appendMessage("")
        advanceUntilIdle()
        assertEquals(1, v.state.value.complaint?.messages?.size)
    }
}

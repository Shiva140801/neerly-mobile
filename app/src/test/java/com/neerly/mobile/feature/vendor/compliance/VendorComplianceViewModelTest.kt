package com.neerly.mobile.feature.vendor.compliance

import android.net.Uri
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VendorComplianceViewModelTest {
    private val api: VendorDocsApi = mockk(relaxed = true)
    private val uploader: DocUploader = mockk()
    private val file: Uri = mockk()

    @Before fun setUp() { Dispatchers.setMain(UnconfinedTestDispatcher()) }
    @After fun tearDown() { Dispatchers.resetMain() }

    private fun row(type: String, status: String, at: String, days: Int? = null, renew: Boolean = false, reason: String? = null) =
        DocRow(id = "$type-$at", docType = type, status = status, uploadedAt = at, expiresInDays = days, renewSoon = renew, rejectionReason = reason)

    @Test
    fun latestCopyPerType_decidesWhatToFixRenewOrAdd() {
        coEvery { api.docs() } returns listOf(
            row("BIS_CERTIFICATE", "REJECTED", "2026-11-01T00:00:00Z", reason = "Blurry"),
            row("FSSAI_LICENSE", "REJECTED", "2026-10-01T00:00:00Z"),
            row("FSSAI_LICENSE", "APPROVED", "2026-11-02T00:00:00Z", days = 41, renew = true))
        val s = VendorComplianceViewModel(api, uploader, com.neerly.mobile.support.TestStrings).state.value
        assertEquals(listOf("BIS_CERTIFICATE"), s.toFix.map { it.docType })          // the old FSSAI rejection is superseded
        assertEquals(listOf("FSSAI_LICENSE"), s.toRenew.map { it.docType })
        assertEquals(setOf("PAN", "WATER_TEST"), s.missing.map { it.type }.toSet())    // GSTIN is optional
    }

    @Test
    fun draft_checksNumbersAndDates() {
        val fssai = SHOP_DOCS.first { it.type == "FSSAI_LICENSE" }
        assertNotNull(DocDraft(fssai, number = "123", expires = "2031-12-18", file = file).problem)
        assertNotNull(DocDraft(fssai, number = "10019011000256", file = file).problem)       // expiry missing
        assertNull(DocDraft(fssai, number = "10019011000256", expires = "2031-12-18", file = file).problem)
        val pan = SHOP_DOCS.first { it.type == "PAN" }
        assertNull(DocDraft(pan, number = "abcde1234f", file = file).problem)
        assertNotNull(DocDraft(SHOP_DOCS.first { it.type == "WATER_TEST" }).problem)          // no file
    }

    @Test
    fun send_uploadsThenSubmitsTheKey() {
        coEvery { api.docs() } returns emptyList()
        coEvery { uploader.upload(file, "WATER_TEST") } returns "vendor-docs/u1/water_test/abc.pdf"
        coEvery { api.submit(any()) } returns row("WATER_TEST", "SUBMITTED", "2026-11-07T10:00:00Z")
        val vm = VendorComplianceViewModel(api, uploader, com.neerly.mobile.support.TestStrings)
        vm.edit("WATER_TEST")
        vm.update { copy(file = this@VendorComplianceViewModelTest.file) }
        vm.send()
        coVerify { api.submit(match { it.docType == "WATER_TEST" && it.s3Key == "vendor-docs/u1/water_test/abc.pdf" }) }
        assertNull(vm.state.value.draft)
        assertTrue(vm.state.value.sent!!.contains("Water test report"))
        assertEquals(1, vm.state.value.docs.size)
    }

    @Test
    fun storageNotSetUp_saysSoAndKeepsTheDraft() {
        coEvery { api.docs() } returns emptyList()
        coEvery { uploader.upload(any(), any()) } throws DocUploader.UploadUnavailable(null)
        val vm = VendorComplianceViewModel(api, uploader, com.neerly.mobile.support.TestStrings)
        vm.edit("WATER_TEST")
        vm.update { copy(file = this@VendorComplianceViewModelTest.file) }
        vm.send()
        assertNotNull(vm.state.value.draft)
        assertTrue(vm.state.value.sendError!!.contains("isn't available"))
        coVerify(exactly = 0) { api.submit(any()) }
    }
}

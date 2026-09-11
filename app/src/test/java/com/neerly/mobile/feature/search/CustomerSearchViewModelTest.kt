package com.neerly.mobile.feature.search

import com.neerly.mobile.core.util.ConnectivityObserver
import com.neerly.mobile.data.dto.GroupedSearchResponse
import com.neerly.mobile.data.dto.SearchProductGroup
import com.neerly.mobile.data.dto.SearchVendorHit
import com.neerly.mobile.data.local.RecentSearchStore
import com.neerly.mobile.data.repo.CustomerRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal

@OptIn(ExperimentalCoroutinesApi::class)
class CustomerSearchViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val repo: CustomerRepository = mockk()
    private val recents: RecentSearchStore = mockk<RecentSearchStore>(relaxed = true).also {
        every { it.recents } returns flowOf(emptyList())
    }
    private val connectivity: ConnectivityObserver = mockk<ConnectivityObserver>(relaxed = true).also {
        every { it.isOnline } returns MutableStateFlow(true)
    }

    @Before fun setUp() {
        Dispatchers.setMain(dispatcher)
        coEvery { repo.myOrders(any(), any()) } returns emptyList()
    }

    @After fun tearDown() { Dispatchers.resetMain() }

    private fun product(name: String, vendorCount: Int = 3) = SearchProductGroup(
        categoryCode = "JAR_20L_NORMAL", displayName = name, sizeLabel = "20L",
        vendorCount = vendorCount, fromPrice = BigDecimal("85")
    )

    private fun vendorHit(name: String) =
        SearchVendorHit(id = "v-$name", businessName = name, status = "ACTIVE")

    private fun hits(
        products: List<SearchProductGroup> = emptyList(),
        vendors: List<SearchVendorHit> = emptyList()
    ) = GroupedSearchResponse(products, vendors)

    private fun vm() = CustomerSearchViewModel(repo, recents, connectivity)

    @Test
    fun oneCharacter_neverHitsTheNetwork() = runTest(dispatcher) {
        val search = vm()
        search.onQueryChange("j")
        advanceUntilIdle()

        assertTrue(search.state.value.results is SearchResults.Idle)
        coVerify(exactly = 0) { repo.searchGrouped(any()) }
    }

    @Test
    fun twoCharacters_searchesAndReturnsResults() = runTest(dispatcher) {
        coEvery { repo.searchGrouped("ja") } returns hits(products = listOf(product("20L Jar (Normal)")))
        val search = vm()
        search.onQueryChange("ja")
        advanceUntilIdle()

        val results = search.state.value.results
        assertTrue(results is SearchResults.Success)
        assertEquals(1, (results as SearchResults.Success).products.size)
    }

    @Test
    fun typingFast_debouncesToASingleRequest() = runTest(dispatcher) {
        coEvery { repo.searchGrouped(any()) } returns hits()
        val search = vm()
        search.onQueryChange("ja")
        search.onQueryChange("jar")
        search.onQueryChange("jars")
        advanceUntilIdle()

        coVerify(exactly = 1) { repo.searchGrouped("jars") }
    }

    @Test
    fun noResults_offersAShorterWordFromAMultiWordQuery() = runTest(dispatcher) {
        coEvery { repo.searchGrouped("tankar wtr") } returns hits()
        val search = vm()
        search.submit("tankar wtr")
        advanceUntilIdle()

        val results = search.state.value.results
        assertTrue(results is SearchResults.Empty)
        assertEquals("tankar", (results as SearchResults.Empty).shorterTerm)
    }

    @Test
    fun noResults_singleWord_makesNoSuggestionRatherThanGuessingASpelling() = runTest(dispatcher) {
        coEvery { repo.searchGrouped("zzz") } returns hits()
        val search = vm()
        search.submit("zzz")
        advanceUntilIdle()

        assertNull((search.state.value.results as SearchResults.Empty).shorterTerm)
    }

    @Test
    fun successfulSearch_isRecorded_asARecentTerm() = runTest(dispatcher) {
        coEvery { repo.searchGrouped("jar") } returns hits(products = listOf(product("20L Jar (Normal)")))
        val search = vm()
        search.submit("jar")
        advanceUntilIdle()

        coVerify { recents.record("jar") }
    }

    @Test
    fun vendorsWhoseNameMatched_areTheirOwnGroup_notMixedIntoProducts() = runTest(dispatcher) {
        coEvery { repo.searchGrouped("ganesh") } returns hits(
            products = listOf(product("20L Jar (Normal)")),
            vendors = listOf(vendorHit("Sri Ganesh Water Supply"))
        )
        val search = vm()
        search.onQueryChange("ganesh")
        advanceUntilIdle()

        val results = search.state.value.results as SearchResults.Success
        assertEquals(1, results.products.size)
        assertEquals(1, results.vendors.size)
        assertEquals("Sri Ganesh Water Supply", results.vendors.first().businessName)
    }

    @Test
    fun aVendorOnlyMatch_isNotTreatedAsNoResults() = runTest(dispatcher) {
        // Searching a shop's name finds the shop even when nothing it sells is called
        // that; showing "no results" there would be wrong.
        coEvery { repo.searchGrouped("balaji") } returns hits(vendors = listOf(vendorHit("Sri Balaji Water")))
        val search = vm()
        search.onQueryChange("balaji")
        advanceUntilIdle()

        val results = search.state.value.results
        assertTrue(results is SearchResults.Success)
        assertEquals(1, (results as SearchResults.Success).vendors.size)
    }

    @Test
    fun failure_showsCustomerCopy_neverTheThrowableMessage() = runTest(dispatcher) {
        coEvery { repo.searchGrouped(any()) } throws RuntimeException("Unable to create converter for List<Foo>")
        val search = vm()
        search.submit("jar")
        advanceUntilIdle()

        val results = search.state.value.results
        assertTrue(results is SearchResults.Error)
        val message = (results as SearchResults.Error).message
        assertFalse(message.contains("converter"))
        assertEquals("Couldn't search right now. Please try again.", message)
    }

    @Test
    fun clearQuery_returnsToTheRecentsState() = runTest(dispatcher) {
        coEvery { repo.searchGrouped(any()) } returns hits(products = listOf(product("20L Jar (Normal)")))
        val search = vm()
        search.submit("jar")
        advanceUntilIdle()
        search.clearQuery()

        assertEquals("", search.state.value.query)
        assertTrue(search.state.value.results is SearchResults.Idle)
    }

    @Test
    fun reorderChips_comeFromRealPastOrderItems_notAPopularityGuess() = runTest(dispatcher) {
        coEvery { repo.myOrders(any(), any()) } returns emptyList()
        val search = vm()
        advanceUntilIdle()

        // No orders, no chips — the screen shows nothing rather than filler.
        assertTrue(search.state.value.reorderSuggestions.isEmpty())
    }
}

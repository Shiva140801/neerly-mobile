package com.neerly.mobile.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.neerly.mobile.core.util.ConnectivityObserver
import com.neerly.mobile.core.util.userMessage
import com.neerly.mobile.data.dto.ProductResponse
import com.neerly.mobile.data.dto.SearchProductGroup
import com.neerly.mobile.data.dto.SearchVendorHit
import com.neerly.mobile.data.local.RecentSearchStore
import com.neerly.mobile.data.repo.CustomerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * `S-CUST-SEA-01` — search.
 *
 * `GET /customer/search?q=` needs two characters before it returns anything
 * useful, so a shorter query never leaves the device: the screen shows recents
 * and reorder chips instead of firing a request per keystroke. Typing is
 * debounced on top of that.
 *
 * Results come from `/customer/search/grouped`, which returns product templates
 * with a vendor count and a "from" price alongside the vendors whose own name
 * matched — the two groups the screen counts separately.
 */
@HiltViewModel
class CustomerSearchViewModel @Inject constructor(
    private val repo: CustomerRepository,
    private val recentStore: RecentSearchStore,
    connectivity: ConnectivityObserver
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    val isOnline: StateFlow<Boolean> = connectivity.isOnline

    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            recentStore.recents.collect { terms ->
                _state.value = _state.value.copy(recents = terms)
            }
        }
        loadReorderSuggestions()
    }

    fun onQueryChange(raw: String) {
        val query = raw
        _state.value = _state.value.copy(query = query)
        searchJob?.cancel()

        if (query.trim().length < MIN_QUERY_LENGTH) {
            _state.value = _state.value.copy(results = SearchResults.Idle)
            return
        }

        searchJob = viewModelScope.launch {
            delay(DEBOUNCE_MS)
            runSearch(query.trim())
        }
    }

    /** Explicit submit (keyboard "search", or tapping a recent/suggestion chip). */
    fun submit(term: String) {
        searchJob?.cancel()
        val cleaned = term.trim()
        _state.value = _state.value.copy(query = cleaned)
        if (cleaned.length < MIN_QUERY_LENGTH) {
            _state.value = _state.value.copy(results = SearchResults.Idle)
            return
        }
        searchJob = viewModelScope.launch { runSearch(cleaned) }
    }

    fun retry() = submit(_state.value.query)

    fun clearQuery() {
        searchJob?.cancel()
        _state.value = _state.value.copy(query = "", results = SearchResults.Idle)
    }

    fun removeRecent(term: String) {
        viewModelScope.launch { recentStore.remove(term) }
    }

    private suspend fun runSearch(query: String) {
        _state.value = _state.value.copy(results = SearchResults.Loading)
        runCatching { repo.searchGrouped(query) }
            .onSuccess { grouped ->
                recentStore.record(query)
                _state.value = _state.value.copy(
                    results = if (grouped.products.isEmpty() && grouped.vendors.isEmpty()) {
                        SearchResults.Empty(query = query, shorterTerm = shorterTermFor(query))
                    } else {
                        SearchResults.Success(products = grouped.products, vendors = grouped.vendors)
                    }
                )
            }
            .onFailure {
                _state.value = _state.value.copy(
                    results = SearchResults.Error(
                        it.userMessage(
                            context = "customer search",
                            fallback = "Couldn't search right now. Please try again."
                        )
                    )
                )
            }
    }

    /**
     * Reorder chips come from what this customer has actually bought — the
     * canvas labels the section "From your orders" and there is no popularity
     * endpoint to stand in for it.
     */
    private fun loadReorderSuggestions() {
        viewModelScope.launch {
            runCatching { repo.myOrders(page = 0, size = 10) }
                .onSuccess { orders ->
                    val names = orders
                        .flatMap { it.items }
                        .map { it.productName }
                        .filter { it.isNotBlank() }
                        .distinct()
                        .take(MAX_REORDER_CHIPS)
                    _state.value = _state.value.copy(reorderSuggestions = names)
                }
        }
    }

    /**
     * The canvas suggests "try a shorter word like 'tanker'" on a no-hit query.
     * We only make that offer when there genuinely is a shorter search to run —
     * the longest single word of a multi-word query. Inventing a spelling
     * correction would mean guessing at a catalogue we can't see.
     */
    private fun shorterTermFor(query: String): String? {
        val words = query.split(' ', '\t').map { it.trim() }.filter { it.length >= MIN_QUERY_LENGTH }
        if (words.size < 2) return null
        return words.maxByOrNull { it.length }
    }

    companion object {
        const val MIN_QUERY_LENGTH = 2
        const val DEBOUNCE_MS = 300L
        private const val MAX_REORDER_CHIPS = 3
    }
}

data class SearchUiState(
    val query: String = "",
    val results: SearchResults = SearchResults.Idle,
    val recents: List<String> = emptyList(),
    val reorderSuggestions: List<String> = emptyList()
)

sealed interface SearchResults {
    /** Fewer than two characters typed — the screen shows recents instead. */
    data object Idle : SearchResults
    data object Loading : SearchResults
    data class Success(
        val products: List<SearchProductGroup>,
        val vendors: List<SearchVendorHit> = emptyList()
    ) : SearchResults
    data class Empty(val query: String, val shorterTerm: String?) : SearchResults
    data class Error(val message: String) : SearchResults
}

package com.neerly.mobile.feature.search

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing
import com.neerly.mobile.core.design.OfflineBanner
import com.neerly.mobile.core.design.SkeletonBlock
import com.neerly.mobile.core.design.loadingSemantics
import com.neerly.mobile.core.util.asRupees
import com.neerly.mobile.data.dto.SearchProductGroup
import com.neerly.mobile.data.dto.SearchVendorHit

/**
 * `S-CUST-SEA-01` — search, reached from the Home header.
 *
 * Four states, all designed: an empty query showing recents and reorder chips,
 * a loading state, grouped results, and a no-results state that offers the
 * customer somewhere to go next instead of a dead end.
 *
 * Results carry a **Products** group only. `/customer/search` doesn't return
 * vendors or a price aggregate yet (gap doc: **EXTEND**), so there is no
 * Vendors header and no "3 vendors from ₹90" line — an empty section would
 * read as "no vendors match", which isn't what the API said.
 */
@Composable
fun CustomerSearchScreen(
    onBack: () -> Unit,
    onOpenVendor: (vendorId: String) -> Unit,
    onBrowseVendors: () -> Unit,
    vm: CustomerSearchViewModel = hiltViewModel()
) {
    val state by vm.state.collectAsState()
    val online by vm.isOnline.collectAsState()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Column(
        Modifier
            .fillMaxSize()
            .background(NeerlyColors.Paper)
            .imePadding()
    ) {
        if (!online) OfflineBanner(onRetry = vm::retry)

        SearchBar(
            query = state.query,
            focusRequester = focusRequester,
            onQueryChange = vm::onQueryChange,
            onSubmit = {
                keyboard?.hide()
                vm.submit(state.query)
            },
            onClear = vm::clearQuery,
            onBack = onBack
        )

        when (val results = state.results) {
            is SearchResults.Idle -> EmptyQueryState(
                recents = state.recents,
                reorderSuggestions = state.reorderSuggestions,
                onPick = { keyboard?.hide(); vm.submit(it) },
                onRemoveRecent = vm::removeRecent
            )

            is SearchResults.Loading -> SearchSkeleton()

            is SearchResults.Success -> ResultsList(
                products = results.products,
                vendors = results.vendors,
                query = state.query,
                onOpenVendor = onOpenVendor
            )

            is SearchResults.Empty -> NoResultsState(
                query = results.query,
                shorterTerm = results.shorterTerm,
                onTryShorter = { keyboard?.hide(); vm.submit(it) },
                onBrowseVendors = onBrowseVendors
            )

            is SearchResults.Error -> SearchErrorState(
                message = results.message,
                onRetry = vm::retry
            )
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    focusRequester: FocusRequester,
    onQueryChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit
) {
    Column {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = NeerlySpacing.x3, vertical = NeerlySpacing.x2),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(NeerlySpacing.x2)
        ) {
            Box(
                Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = NeerlyColors.Ink800
                )
            }
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(NeerlyRadius.pill))
                    .background(NeerlyColors.Paper)
                    .border(1.5.dp, NeerlyColors.CustomerPrimary, RoundedCornerShape(NeerlyRadius.pill))
                    .padding(horizontal = NeerlySpacing.x4, vertical = NeerlySpacing.x3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(NeerlySpacing.x2)
            ) {
                Icon(
                    Icons.Filled.Search,
                    contentDescription = null,
                    tint = NeerlyColors.CustomerPrimary,
                    modifier = Modifier.size(18.dp)
                )
                Box(Modifier.weight(1f)) {
                    if (query.isEmpty()) {
                        Text("Search water", fontSize = 14.sp, color = NeerlyColors.Ink400)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeerlyColors.Ink900
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                    )
                }
                if (query.isNotEmpty()) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Clear search",
                        tint = NeerlyColors.Ink400,
                        modifier = Modifier
                            .size(18.dp)
                            .clickable(onClick = onClear)
                    )
                }
            }
        }
        HorizontalDivider(color = NeerlyColors.Ink100)
    }
}

/** `S-CUST-SEA-01 · Empty query` — recents and reorder chips. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EmptyQueryState(
    recents: List<String>,
    reorderSuggestions: List<String>,
    onPick: (String) -> Unit,
    onRemoveRecent: (String) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = NeerlySpacing.x5, vertical = NeerlySpacing.x4)
    ) {
        if (recents.isNotEmpty()) {
            SectionLabel("Recent")
            Spacer(Modifier.height(NeerlySpacing.x1))
            recents.forEach { term ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable { onPick(term) }
                        .padding(vertical = NeerlySpacing.x3),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)
                ) {
                    Icon(
                        Icons.Filled.History,
                        contentDescription = null,
                        tint = NeerlyColors.Ink400,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        term,
                        Modifier.weight(1f),
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeerlyColors.Ink900
                    )
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Remove $term from recent searches",
                        tint = NeerlyColors.Ink300,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { onRemoveRecent(term) }
                    )
                }
                HorizontalDivider(color = NeerlyColors.Ink50)
            }
            Spacer(Modifier.height(NeerlySpacing.x6))
        }

        if (reorderSuggestions.isNotEmpty()) {
            SectionLabel("From your orders")
            Spacer(Modifier.height(NeerlySpacing.x3))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(NeerlySpacing.x2),
                verticalArrangement = Arrangement.spacedBy(NeerlySpacing.x2)
            ) {
                reorderSuggestions.forEach { term -> SuggestionChip(term) { onPick(term) } }
            }
            Spacer(Modifier.height(NeerlySpacing.x6))
        }

        Surface(color = NeerlyColors.Ink50, shape = RoundedCornerShape(NeerlyRadius.md)) {
            Text(
                "Type at least ${CustomerSearchViewModel.MIN_QUERY_LENGTH} letters to search.",
                modifier = Modifier.padding(horizontal = NeerlySpacing.x4, vertical = NeerlySpacing.x3),
                fontSize = 12.sp,
                color = NeerlyColors.Ink500,
                lineHeight = 18.sp
            )
        }
    }
}

@Composable
private fun ResultsList(
    products: List<SearchProductGroup>,
    vendors: List<SearchVendorHit>,
    query: String,
    onOpenVendor: (String) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = NeerlySpacing.x5,
            vertical = NeerlySpacing.x4
        ),
        verticalArrangement = Arrangement.spacedBy(NeerlySpacing.x2)
    ) {
        // Each group is skipped entirely when it is empty rather than shown as
        // "Products · 0", which reads as a failure rather than as "look below".
        if (products.isNotEmpty()) {
            item {
                SectionLabel("Products · ${products.size}")
                Spacer(Modifier.height(NeerlySpacing.x2))
            }
            items(products, key = { it.categoryCode }) { group ->
                ProductGroupRow(group, query)
            }
        }
        if (vendors.isNotEmpty()) {
            item {
                Spacer(Modifier.height(NeerlySpacing.x3))
                SectionLabel("Vendors · ${vendors.size}")
                Spacer(Modifier.height(NeerlySpacing.x2))
            }
            items(vendors, key = { it.id }) { vendor ->
                VendorResultRow(vendor, query, onClick = { onOpenVendor(vendor.id) })
            }
        }
    }
}

/**
 * One row per product template, not per listing: the same 20L jar sold by three
 * vendors is one result that says how many stock it and what the cheapest asks.
 */
@Composable
private fun ProductGroupRow(group: SearchProductGroup, query: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NeerlyRadius.md))
            .border(1.dp, NeerlyColors.Ink200, RoundedCornerShape(NeerlyRadius.md))
            .padding(NeerlySpacing.x3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(NeerlyRadius.sm))
                .background(NeerlyColors.Water50),
            contentAlignment = Alignment.Center
        ) { Text("💧", fontSize = 20.sp) }
        Column(Modifier.weight(1f)) {
            Text(
                highlightMatch(group.displayName, query),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NeerlyColors.Ink900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            val detail = listOfNotNull(
                group.sizeLabel?.takeIf { it.isNotBlank() },
                "from ${group.fromPrice.asRupees()}",
                if (group.vendorCount == 1) "1 vendor" else "${group.vendorCount} vendors"
            ).joinToString(" · ")
            Text(detail, fontSize = 12.sp, color = NeerlyColors.Ink500)
        }
    }
}

@Composable
private fun VendorResultRow(vendor: SearchVendorHit, query: String, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(NeerlyRadius.md))
            .border(1.dp, NeerlyColors.Ink200, RoundedCornerShape(NeerlyRadius.md))
            .clickable(onClick = onClick)
            .padding(NeerlySpacing.x3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(NeerlySpacing.x3)
    ) {
        Box(
            Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(NeerlyRadius.sm))
                .background(NeerlyColors.Water50),
            contentAlignment = Alignment.Center
        ) { Text("🏪", fontSize = 20.sp) }
        Column(Modifier.weight(1f)) {
            Text(
                highlightMatch(vendor.businessName, query),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = NeerlyColors.Ink900,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text("Tap to see what they stock", fontSize = 12.sp, color = NeerlyColors.Ink500)
        }
        Icon(
            Icons.Filled.Search,
            contentDescription = null,
            tint = NeerlyColors.Ink300,
            modifier = Modifier.size(16.dp)
        )
    }
}

/** Tints the matched span, the way the canvas marks the hit inside a result. */
private fun highlightMatch(text: String, query: String): AnnotatedString {
    val needle = query.trim()
    if (needle.isEmpty()) return AnnotatedString(text)
    val idx = text.indexOf(needle, ignoreCase = true)
    if (idx < 0) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text.substring(0, idx))
        withStyle(SpanStyle(background = NeerlyColors.CustomerSoft)) {
            append(text.substring(idx, idx + needle.length))
        }
        append(text.substring(idx + needle.length))
    }
}

/** `S-CUST-SEA-01 · No results` — always offers a next action. */
@Composable
private fun NoResultsState(
    query: String,
    shorterTerm: String?,
    onTryShorter: (String) -> Unit,
    onBrowseVendors: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = NeerlySpacing.x10, vertical = NeerlySpacing.x6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(130.dp).clip(CircleShape).background(NeerlyColors.Ink50),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Search,
                contentDescription = null,
                tint = NeerlyColors.Ink300,
                modifier = Modifier.size(52.dp)
            )
        }
        Spacer(Modifier.height(NeerlySpacing.x6))
        Text(
            "No matches for \"$query\"",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = NeerlyColors.Ink900
        )
        Spacer(Modifier.height(NeerlySpacing.x2))
        Text(
            if (shorterTerm != null) {
                "Check the spelling, or try a shorter word like \"$shorterTerm\"."
            } else {
                "Check the spelling, or browse the vendors delivering near you."
            },
            fontSize = 13.5.sp,
            color = NeerlyColors.Ink500,
            lineHeight = 21.sp
        )
        Spacer(Modifier.height(NeerlySpacing.x5))
        if (shorterTerm != null) {
            Button(
                onClick = { onTryShorter(shorterTerm) },
                shape = RoundedCornerShape(NeerlyRadius.pill),
                colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
            ) { Text("Search \"$shorterTerm\"", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
            Spacer(Modifier.height(NeerlySpacing.x2))
        }
        OutlinedButton(
            onClick = onBrowseVendors,
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeerlyColors.CustomerPrimary)
        ) { Text("Browse all vendors", fontWeight = FontWeight.Bold, fontSize = 13.sp) }
    }
}

@Composable
private fun SearchErrorState(message: String, onRetry: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = NeerlySpacing.x8, vertical = NeerlySpacing.x6),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Search isn't available",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = NeerlyColors.Ink900
        )
        Spacer(Modifier.height(NeerlySpacing.x2))
        Text(
            message,
            fontSize = 13.sp,
            color = NeerlyColors.Ink500,
            lineHeight = 19.sp
        )
        Spacer(Modifier.height(NeerlySpacing.x5))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
        ) { Text("Try again", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun SearchSkeleton() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = NeerlySpacing.x5, vertical = NeerlySpacing.x4)
            .loadingSemantics("Searching"),
        verticalArrangement = Arrangement.spacedBy(NeerlySpacing.x2)
    ) {
        SkeletonBlock(height = 12.dp, width = 90.dp)
        Spacer(Modifier.height(NeerlySpacing.x1))
        repeat(4) {
            SkeletonBlock(
                height = 68.dp,
                modifier = Modifier.fillMaxWidth(),
                radius = NeerlyRadius.md
            )
        }
    }
}

@Composable
private fun SuggestionChip(text: String, onClick: () -> Unit) {
    Surface(
        color = NeerlyColors.CustomerSofter,
        shape = RoundedCornerShape(NeerlyRadius.pill),
        modifier = Modifier
            .clip(RoundedCornerShape(NeerlyRadius.pill))
            .clickable(onClick = onClick)
            .border(1.dp, NeerlyColors.CustomerSoft, RoundedCornerShape(NeerlyRadius.pill))
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = NeerlySpacing.x4, vertical = NeerlySpacing.x3),
            fontSize = 13.5.sp,
            fontWeight = FontWeight.Bold,
            color = NeerlyColors.CustomerDark
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        fontSize = 11.sp,
        letterSpacing = 0.9.sp,
        fontWeight = FontWeight.SemiBold,
        color = NeerlyColors.Ink500
    )
}

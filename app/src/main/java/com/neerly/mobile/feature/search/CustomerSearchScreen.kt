package com.neerly.mobile.feature.search

import com.neerly.mobile.R
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.IconButton
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.wrapContentHeight
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
            .background(NeerlyColors.Canvas)
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
                onRemoveRecent = vm::removeRecent,
                onClearRecents = vm::clearRecents
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
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                    contentDescription = stringResource(R.string.cust_back),
                    tint = NeerlyColors.Ink800
                )
            }
            Row(
                Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(NeerlyRadius.pill))
                    .background(NeerlyColors.Paper)
                    .border(1.5.dp, NeerlyColors.CustomerPrimary, RoundedCornerShape(NeerlyRadius.pill))
                    .heightIn(min = 52.dp)
                    .padding(start = NeerlySpacing.x4, end = NeerlySpacing.x1),
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
                        Text(stringResource(R.string.cust_home_search), fontSize = 16.sp, color = NeerlyColors.Ink600)
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        singleLine = true,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = NeerlyColors.Ink900
                        ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                        modifier = Modifier.fillMaxWidth().focusRequester(focusRequester)
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cust_search_clear), tint = NeerlyColors.Ink600)
                    }
                } else Spacer(Modifier.size(44.dp))
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
    onRemoveRecent: (String) -> Unit,
    onClearRecents: () -> Unit = {}
) {
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = NeerlySpacing.x5, vertical = NeerlySpacing.x4)
    ) {
        Text(stringResource(R.string.cust_search_title), fontFamily = androidx.compose.ui.text.font.FontFamily.Serif, fontSize = 30.sp, color = NeerlyColors.Ink900)
        Spacer(Modifier.height(NeerlySpacing.x4))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.weight(1f)) { SectionLabel(stringResource(R.string.cust_search_recent)) }
            if (recents.isNotEmpty()) TextButton(onClick = onClearRecents, modifier = Modifier.heightIn(min = 44.dp)) {
                Text(stringResource(R.string.cust_clear), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
        }
        if (recents.isEmpty()) {
            Text(stringResource(R.string.cust_search_recent_empty), fontSize = 14.sp, color = NeerlyColors.Ink600,
                modifier = Modifier.padding(vertical = NeerlySpacing.x2))
            Spacer(Modifier.height(NeerlySpacing.x4))
        }
        if (recents.isNotEmpty()) {
            Spacer(Modifier.height(NeerlySpacing.x1))
            recents.forEach { term ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clickable { onPick(term) },
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
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeerlyColors.Ink900
                    )
                    IconButton(onClick = { onRemoveRecent(term) }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cust_search_remove_recent, term),
                            tint = NeerlyColors.Ink600, modifier = Modifier.size(18.dp))
                    }
                }
                HorizontalDivider(color = NeerlyColors.Ink50)
            }
            Spacer(Modifier.height(NeerlySpacing.x6))
        }

        if (reorderSuggestions.isNotEmpty()) {
            SectionLabel(stringResource(R.string.cust_search_from_orders))
            Spacer(Modifier.height(NeerlySpacing.x3))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(NeerlySpacing.x2),
                verticalArrangement = Arrangement.spacedBy(NeerlySpacing.x2)
            ) {
                reorderSuggestions.forEach { term -> SuggestionChip(term) { onPick(term) } }
            }
            Spacer(Modifier.height(NeerlySpacing.x6))
        }

        // Board 38 "Browse" — each is a search the catalogue answers, not a fixed list.
        SectionLabel(stringResource(R.string.cust_search_browse))
        Spacer(Modifier.height(NeerlySpacing.x3))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(NeerlySpacing.x2),
            verticalArrangement = Arrangement.spacedBy(NeerlySpacing.x2)
        ) {
            BROWSE_TERMS.forEach { (label, term) -> SuggestionChip(stringResource(label)) { onPick(term) } }
        }
        Spacer(Modifier.height(NeerlySpacing.x5))
        Text(
            stringResource(R.string.cust_search_min_letters, CustomerSearchViewModel.MIN_QUERY_LENGTH),
            fontSize = 13.sp,
            color = NeerlyColors.Ink600,
            lineHeight = 19.sp
        )
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
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Each group is skipped entirely when it is empty rather than shown as
        // "Products · 0", which reads as a failure rather than as "look below".
        if (products.isNotEmpty()) {
            item {
                SectionLabel(stringResource(R.string.cust_search_products, products.size))
                Spacer(Modifier.height(NeerlySpacing.x2))
            }
            items(products, key = { it.categoryCode }) { group ->
                ProductGroupRow(group, query, onOpenVendor)
            }
        }
        if (vendors.isNotEmpty()) {
            item {
                Spacer(Modifier.height(NeerlySpacing.x3))
                SectionLabel(stringResource(R.string.cust_search_suppliers, vendors.size))
                Spacer(Modifier.height(NeerlySpacing.x2))
            }
            items(vendors, key = { it.id }) { vendor ->
                VendorResultRow(vendor, query, onClick = { onOpenVendor(vendor.id) })
            }
        }
    }
}

/**
 * One row per product template, not per listing (board Search): glyph tile, the name with
 * the match marked, every shop's price on one line, and a chevron to the cheapest shop.
 */
@Composable
private fun ProductGroupRow(group: SearchProductGroup, query: String, onOpenVendor: (String) -> Unit = {}) {
    val first = group.offers.firstOrNull()
    com.neerly.mobile.core.design.ListRow(onClick = first?.let { o -> { onOpenVendor(o.vendorId) } }) {
        Box(Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(NeerlyColors.CustomerSoft), contentAlignment = Alignment.Center) {
            com.neerly.mobile.core.design.JarGlyph(outline = NeerlyColors.Ink900, body = NeerlyColors.Paper)
        }
        Column(Modifier.weight(1f)) {
            Text(highlightMatch(listOfNotNull(group.displayName, group.sizeLabel?.takeIf { it.isNotBlank() && !group.displayName.contains(it, true) }).joinToString(" · "), query),
                fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900, maxLines = 2, overflow = TextOverflow.Ellipsis)
            val line = if (group.offers.isNotEmpty()) group.offers.joinToString(" · ") { "${it.vendorName} ${it.price.asRupees()}" }
                else stringResource(R.string.cust_search_from_price, group.fromPrice.asRupees())
            Text(line, fontSize = 15.sp, color = NeerlyColors.Ink600, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = NeerlyColors.Ink600)
    }
}

/** "4.6 ★ · 1.2 km" — only the parts the server knows. */
internal fun offerDetail(o: com.neerly.mobile.data.dto.SearchOffer, km: (String) -> String = { "$it km" }): String = listOfNotNull(
    o.rating?.let { "${it.setScale(1, java.math.RoundingMode.HALF_UP)} ★" },
    o.distanceKm?.let { km(it.setScale(1, java.math.RoundingMode.HALF_UP).toPlainString()) }
).joinToString(" · ")

/** Board Search supplier row: serif initials, the name, "4.7 ★ · 2.1 km". */
@Composable
private fun VendorResultRow(vendor: SearchVendorHit, query: String, onClick: () -> Unit) {
    com.neerly.mobile.core.design.ListRow(onClick = onClick) {
        com.neerly.mobile.core.design.InitialsTile(vendor.businessName.split(' ').take(2).joinToString("") { it.take(1) }.uppercase())
        Column(Modifier.weight(1f)) {
            Text(highlightMatch(vendor.businessName, query), fontSize = 17.sp, fontWeight = FontWeight.Bold,
                color = NeerlyColors.Ink900, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val kmFmt = stringResource(R.string.cust_km, "%s")
            val bits = listOfNotNull(
                vendor.rating?.let { "${it.setScale(1, java.math.RoundingMode.HALF_UP)} ★" },
                vendor.distanceKm?.let { kmFmt.replace("%s", it.setScale(1, java.math.RoundingMode.HALF_UP).toPlainString()) }
            ).joinToString(" · ")
            Text(bits.ifBlank { stringResource(R.string.cust_search_tap_stock) }, fontSize = 15.sp, color = NeerlyColors.Ink600)
        }
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
        withStyle(SpanStyle(background = NeerlyColors.WarnSoft)) {
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
            stringResource(R.string.cust_search_nothing_for, query),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = NeerlyColors.Ink900
        )
        Spacer(Modifier.height(NeerlySpacing.x2))
        Text(
            if (shorterTerm != null) {
                stringResource(R.string.cust_search_try_shorter, shorterTerm)
            } else {
                stringResource(R.string.cust_search_try_browse)
            },
            fontSize = 15.sp,
            color = NeerlyColors.Ink600,
            lineHeight = 21.sp
        )
        Spacer(Modifier.height(NeerlySpacing.x5))
        if (shorterTerm != null) {
            Button(
                onClick = { onTryShorter(shorterTerm) },
                shape = RoundedCornerShape(NeerlyRadius.pill),
                colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
                , modifier = Modifier.heightIn(min = 48.dp)
            ) { Text(stringResource(R.string.cust_search_search_term, shorterTerm), fontWeight = FontWeight.Bold, fontSize = 15.sp) }
            Spacer(Modifier.height(NeerlySpacing.x2))
        }
        OutlinedButton(
            onClick = onBrowseVendors,
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = NeerlyColors.CustomerPrimary)
            , modifier = Modifier.heightIn(min = 48.dp)
        ) { Text(stringResource(R.string.cust_search_browse_all), fontWeight = FontWeight.Bold, fontSize = 15.sp) }
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
            stringResource(R.string.cust_search_unavailable),
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = NeerlyColors.Ink900
        )
        Spacer(Modifier.height(NeerlySpacing.x2))
        Text(
            message,
            fontSize = 15.sp,
            color = NeerlyColors.Ink600,
            lineHeight = 19.sp
        )
        Spacer(Modifier.height(NeerlySpacing.x5))
        Button(
            onClick = onRetry,
            shape = RoundedCornerShape(NeerlyRadius.pill),
            colors = ButtonDefaults.buttonColors(containerColor = NeerlyColors.CustomerPrimary)
        ) { Text(stringResource(R.string.cust_try_again), fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun SearchSkeleton() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = NeerlySpacing.x5, vertical = NeerlySpacing.x4)
            .loadingSemantics(stringResource(R.string.cust_search_searching)),
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
            modifier = Modifier.heightIn(min = 44.dp).wrapContentHeight(Alignment.CenterVertically)
                .padding(horizontal = NeerlySpacing.x4, vertical = NeerlySpacing.x2),
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = NeerlyColors.CustomerDark
        )
    }
}

@Composable
private fun SectionLabel(text: String) = com.neerly.mobile.core.design.SectionLabel(text)

/** Browse shortcuts (board 38): label shown → term searched. */
internal val BROWSE_TERMS = listOf(R.string.cust_search_b_jars to "20L", R.string.cust_search_b_chilled to "chilled", R.string.cust_search_b_bottles to "bottle", R.string.cust_search_b_events to "tanker")

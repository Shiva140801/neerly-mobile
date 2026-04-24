package com.neerly.mobile.feature.notification

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.neerly.mobile.core.design.NeerlyColors
import com.neerly.mobile.core.design.NeerlyRadius
import com.neerly.mobile.core.design.NeerlySpacing

/**
 * In-app feed, powered by GET /api/v1/notifications (see NotificationController).
 * Each row calls POST /api/v1/notifications/{id}/read when tapped.
 */
data class FeedItem(
    val id: String,
    val category: String,
    val subject: String?,
    val body: String,
    val isRead: Boolean,
    val queuedAt: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationFeedScreen(
    items: List<FeedItem>,
    loading: Boolean,
    onOpenItem: (String) -> Unit,
    onBack: () -> Unit
) {
    Scaffold(
        containerColor = NeerlyColors.Canvas,
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    TextButton(onClick = onBack) { Text("Back", color = NeerlyColors.CustomerPrimary) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = NeerlyColors.Paper)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding)
        ) {
            when {
                loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text("Loading…", color = NeerlyColors.Ink500)
                }
                items.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("All caught up", fontSize = 18.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink700)
                        Spacer(Modifier.height(6.dp))
                        Text("We'll drop you a line when something new happens.",
                            fontSize = 13.sp, color = NeerlyColors.Ink500)
                    }
                }
                else -> LazyColumn(
                    contentPadding = PaddingValues(NeerlySpacing.x4),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(items) { item -> FeedRow(item, onOpenItem) }
                }
            }
        }
    }
}

@Composable
private fun FeedRow(item: FeedItem, onOpen: (String) -> Unit) {
    Surface(
        color = if (item.isRead) NeerlyColors.Paper else NeerlyColors.CustomerSofter,
        shape = RoundedCornerShape(NeerlyRadius.md),
        shadowElevation = 1.dp,
        modifier = Modifier.fillMaxWidth().clickable { onOpen(item.id) }
    ) {
        Row(
            Modifier.fillMaxWidth().padding(NeerlySpacing.x4),
            verticalAlignment = Alignment.Top
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(
                        if (item.isRead) NeerlyColors.Ink300 else NeerlyColors.CustomerPrimary
                    )
                    .align(Alignment.Top)
            )
            Spacer(Modifier.width(NeerlySpacing.x3))
            Column(Modifier.weight(1f)) {
                if (!item.subject.isNullOrBlank()) {
                    Text(
                        item.subject,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = NeerlyColors.Ink900
                    )
                    Spacer(Modifier.height(4.dp))
                }
                Text(item.body, fontSize = 14.sp, color = NeerlyColors.Ink700, maxLines = 3)
                Spacer(Modifier.height(8.dp))
                Text("${item.category} · ${item.queuedAt}",
                    fontSize = 11.sp, color = NeerlyColors.Ink500)
            }
        }
    }
}

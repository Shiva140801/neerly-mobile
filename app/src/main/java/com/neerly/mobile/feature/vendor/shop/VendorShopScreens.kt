package com.neerly.mobile.feature.vendor.shop

import com.neerly.mobile.R
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.neerly.mobile.core.design.*
import java.math.RoundingMode

private fun stars(n: Int) = "★".repeat(n.coerceIn(0, 5)) + "☆".repeat((5 - n).coerceIn(0, 5))

private fun timeOf(iso: String?): String = iso?.let {
    runCatching {
        java.time.OffsetDateTime.parse(it).atZoneSameInstant(java.time.ZoneId.of("Asia/Kolkata"))
            .format(java.time.format.DateTimeFormatter.ofPattern("h:mm a"))
    }.getOrNull()
} ?: ""

// ============================================================ V38 · Shop tab

/** Board V38 — how customers see the shop, plus everything waiting on the owner. */
@Composable
fun VendorShopTabScreen(
    onBack: (() -> Unit)? = null,     // null as the Shop tab root
    onReviews: () -> Unit = {},
    onComplaints: () -> Unit = {},
    onEvents: () -> Unit = {},
    onLicences: () -> Unit = {},
    onCatalog: () -> Unit = {},
    onHolidays: () -> Unit = {},
    onTeam: () -> Unit = {},
    onSettings: () -> Unit = {},
    vm: ShopTabViewModel = hiltViewModel()
) {
    val s by vm.state.collectAsState()
    OnReturn(vm::load)
    val yourShop = stringResource(R.string.vend_your_shop)
    BoondhPage(yourShop, onBack = onBack, subtitle = stringResource(R.string.vend_how_customers_see)) {
        if (s.dashboard == null) { item { LoadState(s.loading, s.error, vm::load) }; return@BoondhPage }
        val d = s.dashboard!!
        val name = d.businessName ?: yourShop
        item {
            BoondhCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(48.dp).background(NeerlyColors.CustomerSoft, RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Text(name.split(' ').take(2).joinToString("") { it.take(1) }.uppercase(),
                            fontWeight = FontWeight.Bold, color = NeerlyColors.CustomerPrimary)
                    }
                    Column(Modifier.weight(1f)) {
                        Text(name, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                        val r = s.rating
                        Text(if (r != null && r.totalReviews > 0) pluralStringResource(R.plurals.vend_rating_reviews, r.totalReviews.toInt(),
                                r.averageRating.setScale(1, RoundingMode.HALF_UP).toPlainString(), r.totalReviews.toInt())
                            else stringResource(R.string.vend_no_ratings), fontSize = 14.sp, color = NeerlyColors.Ink600)
                    }
                    if (d.status != null && d.status != "ACTIVE") Pill(d.status.lowercase().replace('_', ' '), Tone.Warn)
                }
            }
        }
        item { SectionLabel(stringResource(R.string.vend_needs_you)) }
        item {
            BoondhCard {
                InfoRow(stringResource(R.string.vend_reviews_to_reply), stringResource(if (s.reviewsToReply == 0) R.string.vend_all_answered else R.string.vend_customers_read_replies),
                    trailing = s.reviewsToReply.toString(), onClick = onReviews)
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.vend_open_complaints), stringResource(R.string.vend_reply_2h),
                    trailing = s.openComplaints.toString(),
                    trailingColor = if (s.openComplaints > 0) NeerlyColors.Err else NeerlyColors.Ink900, onClick = onComplaints)
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.vend_event_bookings), stringResource(if (d.pendingEvents == 0) R.string.vend_nothing_waiting else R.string.vend_waiting_yes_no),
                    trailing = d.pendingEvents.toString(), onClick = onEvents)
                HorizontalDivider(color = NeerlyColors.Ink200)
                val days = s.renewInDays
                InfoRow(stringResource(R.string.vend_licences),
                    when {
                        s.renewDoc == null -> stringResource(R.string.vend_all_up_to_date)
                        days != null && days < 0 -> pluralStringResource(R.plurals.vend_doc_lapsed, -days, docLabel(s.renewDoc!!.docType), -days)
                        else -> pluralStringResource(R.plurals.vend_doc_renews, days ?: 0, docLabel(s.renewDoc!!.docType), days ?: 0)
                    },
                    trailing = if (d.licencesToRenew > 0) d.licencesToRenew.toString() else null,
                    trailingColor = NeerlyColors.Warn, onClick = onLicences)
            }
        }
        item { SectionLabel(yourShop) }
        item {
            BoondhCard {
                InfoRow(stringResource(R.string.vend_what_you_sell), pluralStringResource(R.plurals.vend_n_products, s.products, s.products), onClick = onCatalog)
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.vend_holidays),
                    s.nextHoliday?.let { stringResource(R.string.vend_next_holiday, listOfNotNull(shortDay(it.date), it.label).joinToString(" · ")) } ?: stringResource(R.string.vend_no_closures),
                    onClick = onHolidays)
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.vend_drivers), stringResource(R.string.vend_n_on_team, s.drivers), onClick = onTeam)
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.vend_settings), stringResource(R.string.vend_settings_sub), onClick = onSettings)
            }
        }
    }
}

@Composable
internal fun docLabel(type: String): String = when (type) {
    "FSSAI_LICENSE" -> stringResource(R.string.vend_doc_fssai)
    "BIS_CERTIFICATE" -> stringResource(R.string.vend_doc_bis)
    "WATER_TEST" -> stringResource(R.string.vend_doc_water)
    "PAN" -> stringResource(R.string.vend_doc_pan)
    "GSTIN" -> stringResource(R.string.vend_doc_gstin)
    "SHOP_ESTABLISHMENT" -> stringResource(R.string.vend_doc_shop)
    "TRADE_LICENSE" -> stringResource(R.string.vend_doc_trade)
    "CANCELLED_CHEQUE" -> stringResource(R.string.vend_doc_cheque)
    else -> type.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }
}

// ============================================================ V23 · Reviews

@Composable
fun VendorReviewsScreen(onBack: () -> Unit, vm: VendorReviewsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    var replyingTo by remember { mutableStateOf<String?>(null) }
    var draft by remember { mutableStateOf("") }
    val sum = s.summary
    BoondhPage(stringResource(R.string.vend_reviews), onBack = onBack,
        subtitle = if (sum != null && sum.totalReviews > 0) pluralStringResource(R.plurals.vend_rating_reviews, sum.totalReviews.toInt(),
            sum.averageRating.setScale(1, RoundingMode.HALF_UP).toPlainString(), sum.totalReviews.toInt()) else null) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ReviewFilter.values().forEach { f ->
                    FilterChip(selected = s.filter == f, onClick = { vm.setFilter(f) }, label = { Text(stringResource(f.label), fontSize = 14.sp) },
                        modifier = Modifier.heightIn(min = 44.dp))
                }
            }
        }
        if (s.reviews.isEmpty()) { item { LoadState(s.loading, s.error, vm::load, stringResource(R.string.vend_no_reviews),
            stringResource(R.string.vend_no_reviews_sub)) }; return@BoondhPage }
        if (s.shown.isEmpty()) item { Notice(stringResource(if (s.filter == ReviewFilter.ToReply) R.string.vend_replied_all else R.string.vend_nothing_here), Tone.Ok) }
        s.shown.forEach { r -> item(key = r.id) {
            BoondhCard {
                val starsDesc = stringResource(R.string.vend_out_of_5, r.rating)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stars(r.rating), fontSize = 17.sp, color = Boondh.Amber,
                        modifier = Modifier.weight(1f).semantics { contentDescription = starsDesc })
                    Text(listOfNotNull(r.customerName, r.orderNumber, shortDay(r.createdAt)).joinToString(" · "),
                        fontSize = 13.sp, color = NeerlyColors.Ink600)
                }
                Text(r.text?.ifBlank { null } ?: stringResource(R.string.vend_no_comment), fontSize = 15.sp, lineHeight = 22.sp,
                    color = if (r.text.isNullOrBlank()) NeerlyColors.Ink600 else NeerlyColors.Ink900)
                val reply = r.vendorResponse
                when {
                    reply != null -> Column(Modifier.fillMaxWidth().background(NeerlyColors.Canvas, RoundedCornerShape(10.dp)).padding(12.dp)) {
                        Text(stringResource(R.string.vend_you_replied), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink600)
                        Text("“${reply.text}”", fontSize = 14.sp, color = NeerlyColors.Ink900)
                    }
                    replyingTo == r.id -> {
                        OutlinedTextField(value = draft, onValueChange = { draft = it.take(2000) }, modifier = Modifier.fillMaxWidth(),
                            label = { Text(stringResource(R.string.vend_your_reply)) }, minLines = 2)
                        s.sendError?.let { Notice(it, Tone.Bad) }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SecondaryButton(stringResource(R.string.vend_cancel), { replyingTo = null; draft = "" }, modifier = Modifier.weight(1f))
                            PrimaryButton(stringResource(if (s.sending == r.id) R.string.vend_posting else R.string.vend_post_reply),
                                { vm.reply(r.id, draft) { replyingTo = null; draft = "" } },
                                enabled = draft.isNotBlank() && s.sending == null, modifier = Modifier.weight(1f))
                        }
                    }
                    else -> TextButton(onClick = { replyingTo = r.id; draft = "" }, modifier = Modifier.heightIn(min = 44.dp)) {
                        Text(stringResource(R.string.vend_reply), color = NeerlyColors.CustomerPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } }
    }
}

// ============================================================ V24/V25 · Complaints

private fun complaintTitle(c: ShopComplaint) = c.subject.ifBlank { c.category.lowercase().replace('_', ' ') }

@Composable
private fun complaintPill(c: ShopComplaint): Pair<String, Tone> {
    val last = c.messages.lastOrNull()
    return when {
        !c.open -> stringResource(R.string.vend_solved) to Tone.Ok
        c.slaBreached -> stringResource(R.string.vend_support_stepped_in) to Tone.Bad
        last?.authorRole == "VENDOR" || last?.authorRole == "VENDOR_OWNER" ->
            (timeOf(last.createdAt).takeIf { it.isNotBlank() }?.let { stringResource(R.string.vend_you_replied_at, it) } ?: stringResource(R.string.vend_you_replied)) to Tone.Info
        else -> stringResource(R.string.vend_waiting_for_you) to Tone.Warn
    }
}

@Composable
fun VendorComplaintsScreen(onBack: () -> Unit, vm: VendorComplaintsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    val open = s.open
    if (open != null) { ComplaintThread(open, s, vm); return }
    OnReturn(vm::load)
    BoondhPage(stringResource(R.string.vend_customer_complaints), onBack = onBack,
        subtitle = stringResource(R.string.vend_complaints_sub)) {
        if (s.complaints.isEmpty()) { item { LoadState(s.loading, s.error, vm::load, stringResource(R.string.vend_no_complaints),
            stringResource(R.string.vend_no_complaints_sub)) }; return@BoondhPage }
        val (live, solved) = s.complaints.partition { it.open }
        item { SectionLabel(stringResource(R.string.vend_open_n, live.size)) }
        if (live.isEmpty()) item { Notice(stringResource(R.string.vend_nothing_open), Tone.Ok) }
        live.forEach { c -> item(key = c.id) { ComplaintRow(c) { vm.openThread(c.id) } } }
        if (solved.isNotEmpty()) {
            item { SectionLabel(stringResource(R.string.vend_solved_n, solved.size)) }
            solved.take(10).forEach { c -> item(key = c.id) { ComplaintRow(c) { vm.openThread(c.id) } } }
        }
    }
}

@Composable
private fun ComplaintRow(c: ShopComplaint, onOpen: () -> Unit) {
    BoondhCard(onClick = onOpen) {
        Text(complaintTitle(c), fontSize = 16.sp, fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
        val (label, tone) = complaintPill(c)
        Pill(label, tone)
        Text(listOfNotNull(c.customerName, shortDay(c.filedAt), if (c.evidencePhotos.isNotEmpty()) stringResource(R.string.vend_photo) else null).joinToString(" · "),
            fontSize = 13.sp, color = NeerlyColors.Ink600)
        if (!c.open) c.resolutionNotes?.let { Text(it, fontSize = 13.sp, color = NeerlyColors.Ink700) }
    }
}

@Composable
private fun ComplaintThread(c: ShopComplaint, s: ComplaintsUiState, vm: VendorComplaintsViewModel) {
    BackHandler(onBack = vm::closeThread)
    var draft by remember { mutableStateOf("") }
    BoondhPage(
        title = listOfNotNull(c.customerName?.substringBefore(' '), complaintTitle(c)).joinToString(" · "),
        onBack = vm::closeThread, backLabel = stringResource(R.string.vend_complaints),
        subtitle = shortDay(c.filedAt),
        footer = if (!c.open) null else ({
            s.sendError?.let { Notice(it, Tone.Bad) }
            OutlinedTextField(value = draft, onValueChange = { draft = it.take(2000) }, modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.vend_message)) }, minLines = 1, maxLines = 4)
            PrimaryButton(stringResource(if (s.sending) R.string.vend_sending else R.string.vend_send), { vm.send(draft) { draft = "" } }, enabled = draft.isNotBlank() && !s.sending)
        })
    ) {
        item {
            BoondhCard {
                Text(c.description, fontSize = 15.sp, lineHeight = 22.sp, color = NeerlyColors.Ink900)
                if (c.evidencePhotos.isNotEmpty()) Text(pluralStringResource(R.plurals.vend_photos_attached, c.evidencePhotos.size, c.evidencePhotos.size),
                    fontSize = 13.sp, color = NeerlyColors.Ink600)
            }
        }
        if (s.threadLoading && c.messages.isEmpty()) item { LoadState(true, null, {}) }
        c.messages.filter { !it.message.isBlank() }.forEach { m -> item(key = m.id) {
            val mine = m.authorRole == "VENDOR" || m.authorRole == "VENDOR_OWNER"
            val who = when { mine -> stringResource(R.string.vend_you); m.authorRole.startsWith("ADMIN") -> stringResource(R.string.vend_boondh_support)
                else -> c.customerName?.substringBefore(' ') ?: stringResource(R.string.vend_customer) }
            Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
                Column(Modifier.fillMaxWidth(0.85f)
                    .background(if (mine) NeerlyColors.CustomerSoft else NeerlyColors.Paper, RoundedCornerShape(14.dp)).padding(12.dp)) {
                    Text(m.message, fontSize = 15.sp, color = NeerlyColors.Ink900)
                    Text("$who · ${timeOf(m.createdAt)}", fontSize = 12.sp, color = NeerlyColors.Ink600)
                }
            }
        } }
        if (c.open) item {
            Notice(stringResource(R.string.vend_refunds_decided), Tone.Info)
        } else item {
            Notice(listOfNotNull(stringResource(R.string.vend_solved), c.resolutionNotes).joinToString(" · "), Tone.Ok)
        }
    }
}

// ============================================================ V22/V45 · Event bookings

@Composable
private fun whenLine(e: ShopEvent): String {
    val fmt = java.time.format.DateTimeFormatter.ofPattern("EEE d MMM · h:mm a", androidx.compose.ui.platform.LocalConfiguration.current.locales[0])
    val start = runCatching { java.time.OffsetDateTime.parse(e.eventStart).atZoneSameInstant(java.time.ZoneId.of("Asia/Kolkata")).format(fmt) }
        .getOrDefault(shortDay(e.eventStart))
    return start + if ((e.chillingLeadHours ?: 0) > 0) stringResource(R.string.vend_chilled) else ""
}

/** The pill for an event's status: a string id (null for a status this app doesn't know) and its tone. */
internal fun eventPillRes(status: String): Pair<Int?, Tone> = when (status) {
    "PENDING_CONFIRMATION" -> R.string.vend_ev_needs_answer to Tone.Warn
    "EXPIRED" -> R.string.vend_ev_expired to Tone.Neutral
    "VENDOR_CONFIRMED", "ADVANCE_PAID" -> R.string.vend_ev_confirmed to Tone.Ok
    "IN_PROGRESS", "DELIVERED" -> R.string.vend_ev_under_way to Tone.Info
    "PICKED_UP", "COMPLETED" -> R.string.vend_ev_done to Tone.Ok
    "VENDOR_REJECTED" -> R.string.vend_ev_declined to Tone.Neutral
    else -> null to Tone.Neutral
}

@Composable
internal fun eventPill(status: String): Pair<String, Tone> {
    val (res, tone) = eventPillRes(status)
    return (res?.let { stringResource(it) } ?: status.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() }) to tone
}

@Composable
fun VendorEventsScreen(onBack: () -> Unit, vm: VendorEventsViewModel = hiltViewModel()) {
    val s by vm.state.collectAsState()
    s.open?.let { e -> if (s.declining) EventDecline(e, s, vm) else EventDetail(e, s, vm); return }
    OnReturn(vm::load)
    BoondhPage(stringResource(R.string.vend_event_bookings), onBack = onBack, subtitle = stringResource(R.string.vend_events_sub)) {
        if (s.events.isEmpty()) { item { LoadState(s.loading, s.error, vm::load, stringResource(R.string.vend_no_event_requests),
            stringResource(R.string.vend_no_event_requests_sub)) }; return@BoondhPage }
        // Expired requests can't be answered any more — list them last, after everything live.
        s.events.sortedBy { if (it.status == "EXPIRED") 1 else 0 }.forEach { e -> item(key = e.id) {
            BoondhCard(onClick = { vm.open(e.id) }) {
                val (label, tone) = eventPill(e.status)
                Pill(label, tone)
                Text(e.expectedGuests?.let { pluralStringResource(R.plurals.vend_n_guests, it.toInt(), e.eventName, it.toInt()) } ?: e.eventName, fontSize = 16.sp,
                    fontWeight = FontWeight.Bold, color = NeerlyColors.Ink900)
                InfoRow(whenLine(e), e.items.sumOf { it.quantity }.let { n -> pluralStringResource(R.plurals.vend_n_items, n, n) }, trailing = e.subtotal.plus(e.deliveryFee).rupees())
            }
        } }
    }
}

@Composable
private fun EventDetail(e: ShopEvent, s: EventsUiState, vm: VendorEventsViewModel) {
    BackHandler(onBack = vm::close)
    var note by remember(e.id) { mutableStateOf("") }
    val value = e.subtotal + e.deliveryFee
    BoondhPage(
        title = e.expectedGuests?.let { pluralStringResource(R.plurals.vend_n_guests, it.toInt(), e.eventName, it.toInt()) } ?: e.eventName,
        onBack = vm::close, backLabel = stringResource(R.string.vend_events),
        subtitle = stringResource(R.string.vend_event_request),
        footer = if (!e.awaitingShop || s.done != null) null else ({
            s.actionError?.let { Notice(it, Tone.Bad) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(stringResource(R.string.vend_cant_do_it), vm::startDecline, enabled = !s.working, modifier = Modifier.weight(1f), color = NeerlyColors.Err)
                PrimaryButton(stringResource(if (s.working) R.string.vend_confirming else R.string.vend_confirm_booking), { vm.confirm(note) }, enabled = !s.working, modifier = Modifier.weight(1f))
            }
        })
    ) {
        s.done?.let { item { Notice(it, Tone.Ok) } }
        item { val (l, t) = eventPill(e.status); Pill(l, t) }
        item {
            BoondhCard {
                InfoRow(stringResource(R.string.vend_when), trailing = whenLine(e))
                e.pickup?.let { InfoRow(stringResource(R.string.vend_jar_pickup), trailing = shortDay(it.scheduledFor)) }
            }
        }
        item { SectionLabel(stringResource(R.string.vend_what_they_want)) }
        item {
            BoondhCard {
                e.items.forEach { i ->
                    // The booking API returns the product id as its name for now; show the quantity
                    // and price honestly rather than a UUID.
                    val label = i.productName?.takeIf { it != i.productId && it.length < 60 } ?: stringResource(R.string.vend_product)
                    InfoRow(stringResource(R.string.vend_qty_x, i.quantity, label), stringResource(R.string.vend_each, i.unitPrice.rupees()), trailing = i.subtotal.rupees())
                }
                if (e.deliveryFee.signum() > 0) InfoRow(stringResource(R.string.vend_delivery), trailing = e.deliveryFee.rupees())
                HorizontalDivider(color = NeerlyColors.Ink200)
                InfoRow(stringResource(R.string.vend_booking_value), trailing = value.rupees())
                if (e.advancePaidAmount.signum() > 0) InfoRow(stringResource(R.string.vend_advance_paid), trailing = e.advancePaidAmount.rupees())
            }
        }
        if (e.depositTotal.signum() > 0) item {
            Text(stringResource(R.string.vend_event_deposits, e.depositTotal.rupees()),
                fontSize = 13.sp, lineHeight = 19.sp, color = NeerlyColors.Ink600)
        }
        e.customerNotes?.takeIf { it.isNotBlank() }?.let { item { Notice(stringResource(R.string.vend_customer_says, it), Tone.Info) } }
        e.rejectionReason?.let { item { Notice(stringResource(R.string.vend_you_said, it), Tone.Neutral) } }
        if (e.awaitingShop && s.done == null) item {
            OutlinedTextField(value = note, onValueChange = { note = it.take(500) }, modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(R.string.vend_note_customer_opt)) }, minLines = 2)
        }
    }
}

@Composable
private fun EventDecline(e: ShopEvent, s: EventsUiState, vm: VendorEventsViewModel) {
    BackHandler(onBack = vm::cancelDecline)
    var picked by remember(e.id) { mutableStateOf<String?>(null) }
    var message by remember(e.id) { mutableStateOf("") }
    val other = picked == EVENT_DECLINE_REASONS.last().serverText
    val reason = when { picked == null -> ""; other -> message; message.isBlank() -> picked!!; else -> "$picked — $message" }
    BoondhPage(
        title = stringResource(R.string.vend_why_cant_event), onBack = vm::cancelDecline, backLabel = stringResource(R.string.vend_booking),
        subtitle = if (e.advancePaidAmount.signum() > 0) stringResource(R.string.vend_decline_event_sub_adv, e.advancePaidAmount.rupees())
            else stringResource(R.string.vend_decline_event_sub),
        footer = {
            s.actionError?.let { Notice(it, Tone.Bad) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SecondaryButton(stringResource(R.string.vend_keep_it), vm::cancelDecline, modifier = Modifier.weight(1f))
                PrimaryButton(stringResource(if (s.working) R.string.vend_declining else R.string.vend_decline_booking), { vm.reject(reason) },
                    enabled = !s.working && reason.isNotBlank(), modifier = Modifier.weight(1f), color = NeerlyColors.Err)
            }
        }
    ) {
        item { Text("${e.eventName} · ${whenLine(e)}", fontSize = 14.sp, color = NeerlyColors.Ink600) }
        EVENT_DECLINE_REASONS.forEach { r -> item(key = r.serverText) {
            val on = picked == r.serverText
            BoondhCard(onClick = { picked = r.serverText }) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = on, onClick = null)
                    Spacer(Modifier.width(8.dp))
                    Column { Text(stringResource(r.label), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = NeerlyColors.Ink900)
                        Text(stringResource(r.detail), fontSize = 13.sp, color = NeerlyColors.Ink600) }
                }
            }
        } }
        item {
            OutlinedTextField(value = message, onValueChange = { message = it.take(400) }, modifier = Modifier.fillMaxWidth(),
                label = { Text(stringResource(if (other) R.string.vend_msg_customer else R.string.vend_msg_customer_opt)) }, minLines = 2)
        }
    }
}

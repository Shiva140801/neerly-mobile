package com.neerly.mobile.data.api

import com.neerly.mobile.data.dto.DepositResponse
import com.neerly.mobile.data.dto.OrderQuoteResponse
import com.neerly.mobile.data.dto.OrderResponse
import com.neerly.mobile.data.dto.PageResponse
import com.neerly.mobile.data.dto.WalletResponse
import com.neerly.mobile.data.dto.WalletTransaction
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Types
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Type
import java.math.BigDecimal

/**
 * Parses payloads captured from the running backend (`src/test/resources/contract/`)
 * through the app's real Moshi. The path-only drift check passed 102/102 while the
 * Deposits and Wallet screens silently rendered empty: a DTO naming a field the server
 * never sends, non-null, makes Moshi reject the whole page. Re-capture a fixture when
 * the backend shape changes on purpose.
 */
class ContractShapeTest {

    private val moshi = NetworkModule.provideMoshi()

    private fun fixture(name: String): String =
        requireNotNull(javaClass.classLoader?.getResource("contract/$name")) { "missing fixture $name" }.readText()

    private fun <T> parse(name: String, type: Type): T {
        val adapter: JsonAdapter<T> = moshi.adapter(type)
        return requireNotNull(adapter.fromJson(fixture(name))) { "$name parsed to null" }
    }

    private fun page(of: Class<*>): Type = Types.newParameterizedType(PageResponse::class.java, of)

    @Test
    fun deposits_page() {
        val page: PageResponse<DepositResponse> = parse("deposits-page.json", page(DepositResponse::class.java))
        assertTrue("fixture should hold deposits", page.content.isNotEmpty())
        page.content.forEach {
            assertTrue(it.amount > BigDecimal.ZERO)
            assertTrue(it.status in setOf("HELD", "LATE", "RETURNED", "FORFEIT", "WAIVED"))
        }
        assertTrue("dates the screen shows", page.content.any { it.returnDeadline != null })
    }

    @Test
    fun wallet_and_transactions() {
        val wallet: WalletResponse = parse("wallet.json", WalletResponse::class.java)
        val txns: PageResponse<WalletTransaction> =
            parse("wallet-transactions.json", page(WalletTransaction::class.java))
        assertTrue(txns.content.isNotEmpty())
        // The newest row's running balance is the wallet balance.
        assertEquals(0, txns.content.first().runningBalance.compareTo(wallet.balance))
    }

    @Test
    fun order_and_order_list() {
        val order: OrderResponse = parse("order.json", OrderResponse::class.java)
        assertTrue(order.orderNumber.startsWith("NEE-"))
        val list: List<OrderResponse> =
            parse("orders-page.json", Types.newParameterizedType(List::class.java, OrderResponse::class.java))
        assertTrue(list.isNotEmpty())
    }

    @Test
    fun switch_role() {
        val r: com.neerly.mobile.data.dto.SwitchRoleResponse =
            parse("switch-role.json", com.neerly.mobile.data.dto.SwitchRoleResponse::class.java)
        assertEquals("VENDOR_OWNER", r.activeRole)
    }

    @Test
    fun order_quote() {
        val q: OrderQuoteResponse = parse("order-quote.json", OrderQuoteResponse::class.java)
        val sum = q.subtotal + q.deliveryFee + q.surgeSurcharge + q.taxes + q.depositTotal - q.discount
        assertEquals(0, sum.compareTo(q.totalAmount))
    }

    @Test
    fun subscription_create_list_and_skip() {
        val sub: com.neerly.mobile.data.dto.SubscriptionResponse =
            parse("subscription.json", com.neerly.mobile.data.dto.SubscriptionResponse::class.java)
        assertEquals(2, sub.quantity)
        assertEquals("6 AM–9 AM", sub.deliverySlot)
        assertTrue("card shows a vendor and product name", sub.vendorName != null && sub.productName != null)
        val list: List<com.neerly.mobile.data.dto.SubscriptionResponse> = parse("subscriptions.json",
            Types.newParameterizedType(List::class.java, com.neerly.mobile.data.dto.SubscriptionResponse::class.java))
        assertTrue(list.isNotEmpty())
        // Skip answers with the skipped delivery, not the subscription.
        val skipped: com.neerly.mobile.data.dto.SubscriptionInstanceResponse =
            parse("subscription-instance.json", com.neerly.mobile.data.dto.SubscriptionInstanceResponse::class.java)
        assertEquals("SKIPPED", skipped.status)
    }

    @Test
    fun vendor_catalog_names_come_from_the_template() {
        val rows: List<com.neerly.mobile.data.dto.VendorProductRow> = parse("vendor-products.json",
            Types.newParameterizedType(List::class.java, com.neerly.mobile.data.dto.VendorProductRow::class.java))
        assertTrue(rows.isNotEmpty())
        rows.forEach { assertTrue("name '${it.name}' should include the template", it.name.contains(it.template.displayName)) }
    }

    @Test
    fun vendor_earnings_overview() {
        val e: com.neerly.mobile.data.dto.EarningsSummary =
            parse("vendor-earnings-overview.json", com.neerly.mobile.data.dto.EarningsSummary::class.java)
        // net = gross − Boondh fee − GST on the fee, for every window.
        listOf(e.today, e.week, e.month).forEach {
            assertEquals(0, (it.gross - it.commission - it.tax).compareTo(it.net))
        }
        assertTrue(e.nextPayoutAt != null)
    }

    @Test
    fun return_request_response() {
        val r: com.neerly.mobile.data.dto.ReturnResponse =
            parse("return.json", com.neerly.mobile.data.dto.ReturnResponse::class.java)
        assertEquals("SCHEDULED_PICKUP", r.returnMode)
        assertEquals(1, r.containerCount)
    }
}

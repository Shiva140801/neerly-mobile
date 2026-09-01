package com.neerly.mobile.data.api

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import java.math.BigDecimal

/**
 * Moshi has no built-in adapter for [BigDecimal], and every money field in the
 * backend contract is one. Without this, `Moshi.adapter(...)` throws
 * "Unable to create converter for …" at *adapter build* time — which Retrofit
 * raises before any HTTP request is attempted, taking down every screen whose
 * DTO graph touches an amount (vendors, wallet, orders, subscriptions, …).
 *
 * Precision is preserved in both directions:
 *  - read: [JsonReader.nextString] hands back the raw JSON literal, so we never
 *    round-trip through [Double].
 *  - write: [JsonWriter.value] with a [Number] emits `BigDecimal.toString()`
 *    verbatim as a JSON number, which parses back to an equal [BigDecimal].
 *
 * Registered `.nullSafe()` in [NetworkModule.provideMoshi] so nullable money
 * fields (`distanceKm`, `avgRating`, …) keep working.
 */
object BigDecimalAdapter : JsonAdapter<BigDecimal>() {

    override fun fromJson(reader: JsonReader): BigDecimal = BigDecimal(reader.nextString())

    override fun toJson(writer: JsonWriter, value: BigDecimal?) {
        writer.value(value)
    }
}

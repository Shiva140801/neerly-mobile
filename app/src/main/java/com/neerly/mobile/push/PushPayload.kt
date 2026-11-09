package com.neerly.mobile.push

import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

/**
 * The FCM data message (plan §c). Every field is a string in FCM; `data` is a JSON string of
 * type-specific fields. Parsing never throws — a malformed field is just absent.
 */
data class PushPayload(
    val v: Int,
    val type: String?,
    val role: String?,
    val nid: String? = null,
    val thread: String? = null,
    val op: Op = Op.POST,
    val title: String? = null,
    val body: String? = null,
    val publicTitle: String? = null,
    val publicBody: String? = null,
    val deepLink: String? = null,
    val actions: String? = null,
    val expiresAt: String? = null,
    val data: Map<String, String> = emptyMap()
) {
    enum class Op { POST, UPDATE, CANCEL }

    operator fun get(key: String): String? = data[key]?.takeIf { it.isNotBlank() }

    /** Epoch ms of [expiresAt], or null when absent/unparseable. */
    val expiresAtMs: Long? get() = expiresAt?.let { runCatching { java.time.Instant.parse(it).toEpochMilli() }.getOrNull() }

    /** Back to the FCM wire shape (debug injection and round-trip tests). */
    fun toMap(): Map<String, String> = buildMap {
        put("v", v.toString())
        type?.let { put("type", it) }
        role?.let { put("role", it) }
        nid?.let { put("nid", it) }
        thread?.let { put("thread", it) }
        put("op", op.name.lowercase())
        title?.let { put("title", it) }
        body?.let { put("body", it) }
        publicTitle?.let { put("publicTitle", it) }
        publicBody?.let { put("publicBody", it) }
        deepLink?.let { put("deepLink", it) }
        actions?.let { put("actions", it) }
        expiresAt?.let { put("expiresAt", it) }
        if (data.isNotEmpty()) put("data", mapAdapter.toJson(data))
    }

    companion object {
        private val moshi = Moshi.Builder().build()
        private val mapAdapter = moshi.adapter<Map<String, String>>(Types.newParameterizedType(Map::class.java, String::class.java, String::class.java))
        private val anyAdapter = moshi.adapter<Map<String, Any?>>(Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java))

        /** The payload version this app understands. */
        const val SUPPORTED_VERSION = 1

        fun parse(m: Map<String, String>): PushPayload = PushPayload(
            v = m["v"]?.trim()?.toIntOrNull() ?: 1,
            type = m["type"]?.trim()?.ifBlank { null },
            role = m["role"]?.trim()?.ifBlank { null },
            nid = m["nid"],
            thread = m["thread"]?.ifBlank { null },
            op = when (m["op"]?.trim()?.lowercase()) { "update" -> Op.UPDATE; "cancel" -> Op.CANCEL; else -> Op.POST },
            title = m["title"]?.ifBlank { null },
            body = m["body"]?.ifBlank { null },
            publicTitle = m["publicTitle"]?.ifBlank { null },
            publicBody = m["publicBody"]?.ifBlank { null },
            deepLink = m["deepLink"]?.ifBlank { null },
            actions = m["actions"]?.ifBlank { null },
            expiresAt = m["expiresAt"]?.ifBlank { null },
            data = parseData(m["data"])
        )

        internal fun parseData(json: String?): Map<String, String> {
            if (json.isNullOrBlank()) return emptyMap()
            return runCatching {
                anyAdapter.fromJson(json).orEmpty().mapNotNull { (k, v) ->
                    when (v) {
                        null -> null
                        // Moshi reads every JSON number as Double; "460" must stay "460".
                        is Double -> k to (if (v % 1.0 == 0.0 && kotlin.math.abs(v) < 1e15) v.toLong().toString() else v.toString())
                        else -> k to v.toString()
                    }
                }.toMap()
            }.getOrDefault(emptyMap())
        }
    }
}

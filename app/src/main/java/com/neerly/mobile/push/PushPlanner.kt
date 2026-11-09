package com.neerly.mobile.push

/**
 * Decides what a push does on this phone, as plain data — no Android types, so every rule
 * from the brief is unit-tested. [PushRouter] carries the decision out.
 */
object PushPlanner {

    /** Over this the phone is "moving" and heads-up buttons are hidden (NPDHeadsUp: 10 km/h). */
    const val MOVING_MPS = 10f / 3.6f

    data class Env(
        /** Active role on this phone (TokenStore), null when signed out. */
        val activeRole: PushRole?,
        val appForeground: Boolean,
        val nowMs: Long,
        /** Last known speed of the phone (driver location ticker), null when unknown. */
        val speedMps: Float? = null,
        /** Driver is on an open shift (rings only on duty, rule 2). Null = unknown. */
        val onDuty: Boolean? = null
    )

    sealed interface Decision {
        data class Ignore(val reason: String) : Decision
        data class Cancel(val tag: String, val id: Int) : Decision
        /** App already open in the right role: show the in-app screen instead of ringing (rule 1). */
        data class InApp(val route: String, val type: PushType) : Decision
        data class Show(val n: Planned) : Decision
    }

    /** Everything needed to build the Android notification. */
    data class Planned(
        val type: PushType?,
        val role: PushRole,
        val channel: PushChannel,
        val tag: String,
        val id: Int,
        val title: String,
        val body: String,
        val publicTitle: String?,
        val publicBody: String?,
        val visibility: Int?,
        val actions: List<PushAction>,
        val deepLink: String,
        val ring: Boolean,
        val ongoing: Boolean,
        val timeoutMs: Long?,
        val speak: Boolean,
        val onlyAlertOnce: Boolean,
        /** Posted into another role's group; tapping switches role first. */
        val otherRole: Boolean,
        val expiresAtMs: Long?
    )

    fun decide(p: PushPayload, env: Env): Decision {
        val type = PushType.of(p.type)
        val target = PushRole.of(p.role) ?: type?.role ?: env.activeRole ?: return Decision.Ignore("no role")
        val tag = tagFor(p, type)
        val id = idFor(tag)
        if (env.activeRole == null) return Decision.Ignore("signed out")
        if (p.op == PushPayload.Op.CANCEL) return Decision.Cancel(tag, id)

        val expires = p.expiresAtMs
        if (expires != null && expires <= env.nowMs) return Decision.Ignore("expired")

        // A newer payload version or a type this build doesn't know: plain card on the role's
        // default channel, title/body as sent, no buttons (plan §c versioning).
        if (p.v > PushPayload.SUPPORTED_VERSION || type == null) {
            val title = p.title ?: return Decision.Ignore("unknown type without text")
            return Decision.Show(Planned(type = null, role = target, channel = PushChannel.defaultFor(target), tag = tag, id = id,
                title = title, body = p.body.orEmpty(), publicTitle = null, publicBody = null, visibility = null,
                actions = emptyList(), deepLink = p.deepLink ?: "${DeepLinks.SCHEME}notifications", ring = false, ongoing = false,
                timeoutMs = null, speak = false, onlyAlertOnce = p.op == PushPayload.Op.UPDATE,
                otherRole = target != env.activeRole, expiresAtMs = expires))
        }

        val otherRole = target != env.activeRole
        if (type.ring) {
            // Rings only make sense in the role that can answer them.
            if (otherRole) return Decision.Ignore("ring for another role")
            if (type.role == PushRole.DRIVER && env.onDuty == false) return Decision.Ignore("off duty")
            if (env.appForeground) {
                val route = DeepLinks.route(p.deepLink ?: DeepLinks.default(type, p)) ?: return Decision.Ignore("no route")
                return Decision.InApp(route, type)
            }
        }

        val moving = env.speedMps != null && env.speedMps > MOVING_MPS
        val actions = (PushAction.parse(p.actions) ?: type.defaultActions)
            // Moving: voice only, no buttons. A dial button without a number in the payload
            // opens the screen that has the call button (the number is fetched there).
            .filterNot { type.speedGated && moving }

        return Decision.Show(Planned(
            type = type, role = target, channel = type.channel, tag = tag, id = id,
            title = p.title ?: "", body = p.body.orEmpty(),
            publicTitle = p.publicTitle, publicBody = p.publicBody,
            visibility = type.visibility,
            actions = actions,
            deepLink = p.deepLink ?: DeepLinks.default(type, p),
            ring = type.ring, ongoing = type.ongoing, timeoutMs = type.timeoutMs,
            speak = type.speak,
            onlyAlertOnce = p.op == PushPayload.Op.UPDATE || type == PushType.CUST_OUT_FOR_DELIVERY,
            otherRole = otherRole,
            expiresAtMs = expires
        ))
    }

    /** One thread per order/complaint/offer (rule 9). */
    fun tagFor(p: PushPayload, type: PushType?): String {
        p.thread?.let { return it }
        type ?: return "nid:${p.nid ?: p.type ?: "push"}"
        fun key(prefix: String, field: String) = p[field]?.let { "$prefix:$it" } ?: "type:${type.name}"
        return when (type.thread) {
            PushType.Thread.ORDER -> key("order", "orderId")
            PushType.Thread.OFFER -> key("offer", "orderId")
            PushType.Thread.COMPLAINT -> key("complaint", "complaintId")
            PushType.Thread.SUBSCRIPTION -> key("sub", "subscriptionId")
            PushType.Thread.REFUND -> key("refund", "refundId")
            PushType.Thread.PAYOUT -> key("payout", "payoutId")
            PushType.Thread.SOS -> key("sos", "sosId")
            PushType.Thread.DRIVER -> key("driver", "driverId")
            PushType.Thread.VENDOR -> key("vendor", "vendorId")
            PushType.Thread.TYPE -> "type:${type.name}"
        }
    }

    /** Stable positive 31-bit id from the tag (String.hashCode is specified, so it's stable). */
    fun idFor(tag: String): Int = tag.hashCode() and 0x7fffffff

    /** The number a CALL button dials, when the payload carries one. */
    fun phoneOf(p: PushPayload): String? = p["driverPhone"] ?: p["customerPhone"] ?: p["phone"]
}

package com.neerly.mobile.push

import androidx.annotation.StringRes
import com.neerly.mobile.R

/**
 * Action buttons, by the id the server sends in `actions` (plan "Changes during implementation").
 * The app owns the label and what the tap does.
 */
enum class PushAction(@StringRes val label: Int, val kind: Kind) {
    TRACK(R.string.push_a_track, Kind.OPEN),
    CALL(R.string.push_a_call, Kind.DIAL),
    SHOW_CODE(R.string.push_a_show_code, Kind.OPEN),
    RATE(R.string.push_a_rate, Kind.OPEN),
    REPORT(R.string.push_a_report, Kind.OPEN),
    NEW_TIME(R.string.push_a_new_time, Kind.OPEN),
    I_WAS_HOME(R.string.push_a_i_was_home, Kind.OPEN),
    OTHER_SHOPS(R.string.push_a_other_shops, Kind.OPEN),
    RETRY_PAY(R.string.push_a_retry_pay, Kind.OPEN),
    PAY_CASH(R.string.push_a_pay_cash, Kind.OPEN),
    SKIP_TOMORROW(R.string.push_a_skip_tomorrow, Kind.BACKGROUND),
    CHANGE_PLAN(R.string.push_a_change_plan, Kind.OPEN),
    ADD_MONEY(R.string.push_a_add_money, Kind.OPEN),
    PLAN_RETURN(R.string.push_a_plan_return, Kind.OPEN),
    VIEW(R.string.push_a_view, Kind.OPEN),
    REPLY(R.string.push_a_reply, Kind.REPLY),
    OPEN(R.string.push_a_open, Kind.OPEN),
    ORDER(R.string.push_a_order, Kind.OPEN),
    NOT_ME(R.string.push_a_not_me, Kind.OPEN),
    ACCEPT(R.string.push_a_accept, Kind.FOREGROUND),
    DECLINE(R.string.push_a_decline, Kind.OPEN),
    RING_SETTINGS(R.string.push_a_ring_settings, Kind.OPEN),
    ASSIGN(R.string.push_a_assign, Kind.OPEN),
    FRESH_JAR(R.string.push_a_fresh_jar, Kind.OPEN),
    COUNT_NOW(R.string.push_a_count_now, Kind.OPEN),
    SEE_MAP(R.string.push_a_see_map, Kind.MAP),
    ADD_STOCK(R.string.push_a_add_stock, Kind.OPEN),
    PLAN_PICKUPS(R.string.push_a_plan_pickups, Kind.OPEN),
    FIX_BANK(R.string.push_a_fix_bank, Kind.OPEN),
    OPEN_EARLY(R.string.push_a_open_early, Kind.OPEN),
    UPLOAD(R.string.push_a_upload, Kind.OPEN),
    TAKE(R.string.push_a_take, Kind.FOREGROUND),
    PASS(R.string.push_a_pass, Kind.BACKGROUND),
    CLOSE_DAY(R.string.push_a_close_day, Kind.OPEN),
    BACK_NOW(R.string.push_a_back_now, Kind.BACKGROUND),
    /** "Still on duty?" — keeps an idle shift open (POST /driver/shifts/stay-on). */
    STAY_ON(R.string.push_a_stay_on, Kind.BACKGROUND),
    /** On-duty card only (NPDLock). */
    TAKE_BREAK(R.string.push_duty_break, Kind.BACKGROUND),
    SAFE(R.string.push_a_safe, Kind.BACKGROUND);

    enum class Kind {
        /** Opens the app at a screen. */
        OPEN,
        /** Opens the dialer with the number in the payload (no CALL_PHONE permission). */
        DIAL,
        /** Opens a map at lat/lng. */
        MAP,
        /** Calls the API from a receiver, no screen. */
        BACKGROUND,
        /** Calls the API then opens the result screen (Accept / Take it). */
        FOREGROUND,
        /** Inline reply (RemoteInput). */
        REPLY
    }

    companion object {
        fun parse(csv: String?): List<PushAction>? = csv?.split(',')?.mapNotNull { id ->
            entries.firstOrNull { it.name == id.trim() }
        }
    }
}

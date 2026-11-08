package com.neerly.mobile.core.util

import com.neerly.mobile.R
import retrofit2.HttpException
import timber.log.Timber
import java.io.IOException

/**
 * Turns a failure into copy a customer can act on, and keeps the technical
 * detail in Timber where it belongs.
 *
 * `Throwable.message` must never reach the UI: Retrofit/Moshi raise text like
 * "Unable to create converter for java.util.List<VendorCardResponse>", which is
 * meaningless to a customer and leaks internal type names.
 *
 * @param context short description of what was being loaded, for the log line.
 * @param fallback surface-specific copy for failures we can't classify.
 */
fun Throwable.userMessage(
    context: String,
    fallback: String = copy(R.string.core_err_generic, "Something went wrong. Please try again.")
): String {
    Timber.e(this, "Request failed: %s", context)
    return when {
        this is IOException ->
            copy(R.string.core_err_offline, "Can't reach Boondh right now. Check your connection and try again.")
        this is HttpException -> when (code()) {
            401, 403 -> copy(R.string.core_err_session, "Your session expired. Please sign in again.")
            in 500..599 -> copy(R.string.core_err_server, "Boondh is having trouble right now. Please try again in a moment.")
            // A 4xx carries copy written for people ("Promo not applicable", "Wrong OTP")
            // in the error envelope — show that rather than a generic line.
            else -> serverMessage() ?: fallback
        }
        else -> fallback
    }
}

/** Same, with the surface's fallback as a string resource (in the app's language). */
fun Throwable.userMessage(context: String, @androidx.annotation.StringRes fallback: Int, strings: Strings): String =
    userMessage(context, strings.get(fallback))

/**
 * Built-in copy in the app's language. The English text is only used where there is no
 * Android context at all (plain JVM unit tests); on a phone it always comes from res/values*.
 */
private fun copy(@androidx.annotation.StringRes id: Int, english: String): String =
    AppLocale.appContext?.let { runCatching { AppLocale.wrapAlways(it).getString(id) }.getOrNull() } ?: english

/** `error.message` from the backend's `{"error":{"code":…,"message":…}}` envelope, if any. */
private fun HttpException.serverMessage(): String? = runCatching {
    val body = response()?.errorBody()?.string() ?: return null
    Regex(""""message"\s*:\s*"((?:[^"\\]|\\.)*)"""").find(body)?.groupValues?.get(1)
        ?.replace("\\\"", "\"")
        ?.takeIf { it.isNotBlank() }
}.getOrNull()

/**
 * The backend error `code` (e.g. SUBSCRIPTION_ENDING), for screens that word one case
 * themselves. Reads the error body, so call it instead of [userMessage], not before it.
 */
fun Throwable.serverErrorCode(): String? = (this as? HttpException)?.let { e ->
    runCatching {
        val body = e.response()?.errorBody()?.string() ?: return null
        Regex(""""code"\s*:\s*"([A-Z0-9_]+)"""").find(body)?.groupValues?.get(1)
    }.getOrNull()
}

package com.neerly.mobile.core.util

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
    fallback: String = "Something went wrong. Please try again."
): String {
    Timber.e(this, "Request failed: %s", context)
    return when {
        this is IOException ->
            "Can't reach Neerly right now. Check your connection and try again."
        this is HttpException -> when (code()) {
            401, 403 -> "Your session expired. Please sign in again."
            in 500..599 -> "Neerly is having trouble right now. Please try again in a moment."
            else -> fallback
        }
        else -> fallback
    }
}

package com.neerly.mobile.feature.address

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** What the reverse geocode found, to pre-fill the form (board 35). */
data class FoundPlace(val street: String?, val pincode: String?, val city: String?, val line: String?)

/** GPS fix + reverse geocode for the address form. Interface so view-model tests can fake it. */
interface AddressLocator {
    suspend fun currentFix(): Pair<Double, Double>?
    suspend fun reverse(lat: Double, lng: Double): FoundPlace?
}

@Singleton
class FusedAddressLocator @Inject constructor(@ApplicationContext private val context: Context) : AddressLocator {

    // The screen asks for the permission before calling; a SecurityException means it was refused.
    @SuppressLint("MissingPermission")
    override suspend fun currentFix(): Pair<Double, Double>? = withTimeoutOrNull(15_000) {
        runCatching {
            val client = LocationServices.getFusedLocationProviderClient(context)
            val cts = CancellationTokenSource()
            suspendCancellableCoroutine<Pair<Double, Double>?> { cont ->
                client.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.token)
                    .addOnSuccessListener { loc -> cont.resume(loc?.let { it.latitude to it.longitude }) }
                    .addOnFailureListener { cont.resume(null) }
                cont.invokeOnCancellation { cts.cancel() }
            }
        }.getOrNull()
    }

    @Suppress("DEPRECATION")
    override suspend fun reverse(lat: Double, lng: Double): FoundPlace? = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent()) return@withContext null
        runCatching {
            Geocoder(context, Locale("en", "IN")).getFromLocation(lat, lng, 1)?.firstOrNull()?.let { a ->
                FoundPlace(
                    street = listOfNotNull(a.thoroughfare, a.subLocality).joinToString(", ").ifBlank { null },
                    pincode = a.postalCode,
                    city = a.locality,
                    line = a.getAddressLine(0)
                )
            }
        }.getOrNull()
    }
}

@dagger.Module
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
abstract class AddressLocatorModule {
    @dagger.Binds abstract fun bind(impl: FusedAddressLocator): AddressLocator
}

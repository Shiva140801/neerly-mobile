package com.neerly.mobile.feature.address

import android.content.Context
import android.location.Geocoder
import com.squareup.moshi.JsonClass
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** One search result (board AddressAdd "Matches"): where it is and what the form can take from it. */
data class PlaceMatch(
    val title: String,
    val subtitle: String,
    val lat: Double,
    val lng: Double,
    val street: String?,
    val pincode: String?,
    val city: String?
)

/**
 * Address search for the pin picker. Today it is Android's [Geocoder], which needs no
 * extra Google API. A Places Autocomplete implementation can be bound instead once the
 * Places API is enabled on the key — flip [PLACES_AUTOCOMPLETE_ENABLED] and add the
 * binding; nothing else changes.
 */
interface PlaceSearch {
    suspend fun search(query: String): List<PlaceMatch>
}

/** Off until the Places API is enabled on the Maps key (and billed); Geocoder is used meanwhile. */
const val PLACES_AUTOCOMPLETE_ENABLED = false

@Singleton
class GeocoderPlaceSearch @Inject constructor(@ApplicationContext private val context: Context) : PlaceSearch {
    @Suppress("DEPRECATION")
    override suspend fun search(query: String): List<PlaceMatch> = withContext(Dispatchers.IO) {
        if (!Geocoder.isPresent() || query.isBlank()) return@withContext emptyList()
        runCatching {
            // Bias to India; the server refuses pins outside it anyway.
            Geocoder(context, Locale("en", "IN"))
                .getFromLocationName(query, MAX_MATCHES, 6.0, 68.0, 37.5, 97.5)
                .orEmpty()
                .map { a ->
                    val title = listOfNotNull(a.featureName?.takeIf { it != a.postalCode && it != a.subThoroughfare }, a.thoroughfare)
                        .distinct().joinToString(", ").ifBlank { a.getAddressLine(0) ?: query }
                    PlaceMatch(
                        title = title,
                        subtitle = listOfNotNull(a.subLocality ?: a.locality, a.postalCode).joinToString(" "),
                        lat = a.latitude, lng = a.longitude,
                        street = listOfNotNull(a.thoroughfare, a.subLocality).joinToString(", ").ifBlank { null },
                        pincode = a.postalCode, city = a.locality
                    )
                }
        }.getOrDefault(emptyList())
    }

    private companion object { const val MAX_MATCHES = 5 }
}

@JsonClass(generateAdapter = true)
data class ServiceableShop(val id: String, val businessName: String)

/** GET /customer/vendors/serviceability — "N suppliers deliver here". */
@JsonClass(generateAdapter = true)
data class Serviceability(val pincode: String, val shopCount: Long, val shops: List<ServiceableShop> = emptyList())

interface AddressMapApi {
    @GET("api/v1/customer/vendors/serviceability")
    suspend fun serviceability(@Query("pincode") pincode: String): Serviceability
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PlaceSearchModule {
    @Binds abstract fun bind(impl: GeocoderPlaceSearch): PlaceSearch

    companion object {
        @Provides @Singleton
        fun api(retrofit: Retrofit): AddressMapApi = retrofit.create(AddressMapApi::class.java)
    }
}

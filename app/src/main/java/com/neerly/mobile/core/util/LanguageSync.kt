package com.neerly.mobile.core.util

import android.content.Context
import com.neerly.mobile.data.auth.TokenStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.PUT
import javax.inject.Inject
import javax.inject.Singleton

interface MeLanguageApi {
    @PUT("api/v1/me/language") suspend fun set(@Body body: Map<String, String>): Map<String, String>
}

/**
 * Tells the server which language this person picked, so notices (order on the way, shop
 * closed…) are written in it. The app's own copy already follows AppLocale; this keeps the
 * two the same. Quietly retried on the next app start if offline or signed out.
 */
@Singleton
class LanguageSync @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokens: TokenStore,
    retrofit: Retrofit
) {
    private val api = retrofit.create(MeLanguageApi::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = context.getSharedPreferences("boondh_locale", Context.MODE_PRIVATE)

    /** Call after the language changes, and on every app start. */
    fun push() {
        val tag = AppLocale.current(context)
        if (!tokens.hasSession() || prefs.getString(SYNCED, null) == tag) return
        scope.launch {
            runCatching { api.set(mapOf("language" to tag)) }
                .onSuccess { prefs.edit().putString(SYNCED, tag).apply() }
        }
    }

    private companion object { const val SYNCED = "synced_tag" }
}

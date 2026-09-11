package com.neerly.mobile.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.searchDataStore: DataStore<Preferences> by preferencesDataStore("recent_searches")

/**
 * Recent search terms, kept on the device.
 *
 * The gap doc offers a choice between a server-side recents endpoint and
 * client-side storage and calls the client cheaper — it's also the only one
 * that doesn't ship a log of what people search for to a server that has no
 * reason to hold it.
 *
 * Stored as one newline-delimited string rather than a `stringSet` because
 * order is the whole point: most recent first.
 */
@Singleton
class RecentSearchStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val key = stringPreferencesKey("terms")

    val recents: Flow<List<String>> = context.searchDataStore.data.map { prefs ->
        prefs[key].orEmpty()
            .split(DELIMITER)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .take(MAX_RECENTS)
    }

    /** Records [term], de-duplicated case-insensitively and moved to the front. */
    suspend fun record(term: String) {
        val cleaned = term.trim()
        if (cleaned.length < MIN_LENGTH) return
        context.searchDataStore.edit { prefs ->
            val existing = prefs[key].orEmpty().split(DELIMITER).filter { it.isNotBlank() }
            val merged = (listOf(cleaned) + existing.filterNot { it.equals(cleaned, ignoreCase = true) })
                .take(MAX_RECENTS)
            prefs[key] = merged.joinToString(DELIMITER)
        }
    }

    suspend fun remove(term: String) {
        context.searchDataStore.edit { prefs ->
            val kept = prefs[key].orEmpty()
                .split(DELIMITER)
                .filter { it.isNotBlank() && !it.equals(term, ignoreCase = true) }
            prefs[key] = kept.joinToString(DELIMITER)
        }
    }

    suspend fun clear() {
        context.searchDataStore.edit { it[key] = "" }
    }

    private companion object {
        const val DELIMITER = "\n"
        const val MAX_RECENTS = 8
        const val MIN_LENGTH = 2
    }
}

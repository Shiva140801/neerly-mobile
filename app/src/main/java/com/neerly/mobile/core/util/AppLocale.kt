package com.neerly.mobile.core.util

import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * The app's own language, independent of the phone's (English / తెలుగు / हिन्दी). The
 * Language screen's choice used to be thrown away. Android 13+ keeps a per-app locale
 * itself; older phones get it applied to the Activity context on every start.
 *
 * All app copy lives in the res/values strings files (English default, values-te, values-hi)
 * and follows this choice; API calls send it as Accept-Language so server messages match.
 */
object AppLocale {
    val SUPPORTED = listOf("en", "te", "hi")
    private const val PREFS = "boondh_locale"
    private const val KEY = "tag"

    /** The language in use right now, for code with no Context (date names, error copy). */
    @Volatile var activeTag: String = "en"
        private set
    val locale: Locale get() = Locale.forLanguageTag(activeTag)

    /** Set on the first Activity start; lets non-UI code resolve copy in the chosen language. */
    @Volatile var appContext: Context? = null
        private set

    fun current(context: Context): String =
        (context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: "en").also { activeTag = it }

    fun set(context: Context, tag: String) {
        val chosen = tag.takeIf { it in SUPPORTED } ?: "en"
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, chosen).apply()
        activeTag = chosen
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java)?.applicationLocales = LocaleList.forLanguageTags(chosen)
        }
    }

    /** A context in the app's chosen language on any Android version (for non-UI code). */
    fun wrapAlways(base: Context): Context {
        val locale = Locale.forLanguageTag(current(base))
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(config)
    }

    /** For Activity.attachBaseContext on phones older than Android 13. */
    fun wrap(base: Context): Context {
        appContext = base.applicationContext ?: base
        current(base)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val locale = Locale.forLanguageTag(current(base))
        val config = Configuration(base.resources.configuration).apply { setLocale(locale) }
        return base.createConfigurationContext(config)
    }
}

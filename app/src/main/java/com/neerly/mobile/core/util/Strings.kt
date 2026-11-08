package com.neerly.mobile.core.util

import android.content.Context
import androidx.annotation.StringRes
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Text for view models (errors, confirmations) in the language the customer picked in the app —
 * screens use `stringResource`, which follows it already. All copy lives in res/values*
 * (English default; values-te Telugu, values-hi Hindi). Never mix languages in one string.
 */
interface Strings {
    fun get(@StringRes id: Int, vararg args: Any): String
}

@Singleton
class AndroidStrings @Inject constructor(@ApplicationContext private val app: Context) : Strings {
    // The application context keeps the phone's language on Android < 13, so resolve through
    // the app's own choice every time (it can change while the app runs).
    override fun get(id: Int, vararg args: Any): String {
        val ctx = AppLocale.wrapAlways(app)
        return if (args.isEmpty()) ctx.getString(id) else ctx.getString(id, *args)
    }
}

/** A [Strings] for composables: the Activity context already carries the app's language. */
@androidx.compose.runtime.Composable
fun rememberStrings(): Strings {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    return androidx.compose.runtime.remember(ctx) { ContextStrings(ctx) }
}

class ContextStrings(private val ctx: Context) : Strings {
    override fun get(id: Int, vararg args: Any): String = if (args.isEmpty()) ctx.getString(id) else ctx.getString(id, *args)
}

@dagger.Module
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
abstract class StringsModule {
    @dagger.Binds abstract fun bind(impl: AndroidStrings): Strings
}

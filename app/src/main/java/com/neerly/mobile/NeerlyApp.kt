package com.neerly.mobile

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

@HiltAndroidApp
class NeerlyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }
        // Channels must exist before the first push; creating them again is a no-op.
        com.neerly.mobile.push.PushChannel.ensure(this)
        com.neerly.mobile.push.AppForeground.install(this)
    }
}

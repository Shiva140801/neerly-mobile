package com.neerly.mobile.core.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Whether the device currently has a *usable* internet connection.
 *
 * Deliberately requires `NET_CAPABILITY_VALIDATED` and not merely "a network is
 * attached": a phone parked on a captive-portal Wi-Fi or a dead cell tower
 * still has an attached network, and telling the customer they're online when
 * every request will time out is worse than saying nothing.
 *
 * State is derived from the callback's own arguments rather than re-polling
 * `activeNetwork` each time one fires. Re-polling looks simpler and is wrong:
 * when the last network drops, `onLost` arrives while `getActiveNetwork()` can
 * still hand back the network that is going away, so the poll returns "online",
 * and — since nothing else is coming — the flow stays stuck at online forever.
 * That is exactly what happened: killing Wi-Fi mid-session left the banner
 * hidden while every request failed.
 *
 * The callback is registered once for the process lifetime — the app has a
 * single Activity and the flow outlives every screen that reads it.
 */
@Singleton
class ConnectivityObserver @Inject constructor(
    @ApplicationContext context: Context
) {
    private val manager = context.getSystemService(ConnectivityManager::class.java)

    private val lock = Any()
    private var defaultNetwork: Network? = null
    private var defaultIsValidated = false

    private val _isOnline = MutableStateFlow(pollCurrent())
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            synchronized(lock) {
                defaultNetwork = network
                defaultIsValidated = manager?.getNetworkCapabilities(network).isUsable()
            }
            publish()
        }

        override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
            synchronized(lock) {
                defaultNetwork = network
                defaultIsValidated = caps.isUsable()
            }
            publish()
        }

        override fun onLost(network: Network) {
            synchronized(lock) {
                if (defaultNetwork == network || defaultNetwork == null) {
                    defaultNetwork = null
                    defaultIsValidated = false
                }
            }
            publish()
        }

        override fun onUnavailable() {
            synchronized(lock) {
                defaultNetwork = null
                defaultIsValidated = false
            }
            publish()
        }
    }

    init {
        runCatching { manager?.registerDefaultNetworkCallback(callback) }
    }

    /**
     * Re-reads the live state. This is what the "Retry" affordances call: the
     * customer has told us to look again, so a fresh poll is the right thing
     * even though the callback normally keeps us current.
     */
    fun refresh() {
        val online = pollCurrent()
        synchronized(lock) {
            if (!online) {
                defaultNetwork = null
                defaultIsValidated = false
            }
        }
        _isOnline.value = online
    }

    private fun publish() {
        _isOnline.value = synchronized(lock) { defaultNetwork != null && defaultIsValidated }
    }

    private fun pollCurrent(): Boolean {
        val cm = manager ?: return true // no service to ask — don't cry wolf
        return cm.getNetworkCapabilities(cm.activeNetwork).isUsable()
    }

    private fun NetworkCapabilities?.isUsable(): Boolean =
        this != null &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
}

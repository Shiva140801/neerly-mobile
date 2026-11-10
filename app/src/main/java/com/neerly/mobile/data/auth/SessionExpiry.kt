package com.neerly.mobile.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Raised when the server no longer accepts this phone's session (refresh refused,
 * reuse detected). Before, the authenticator cleared the tokens silently and each
 * screen showed its own "Try again" for a request that could never succeed; now the
 * NavHost sees this, goes to Welcome with a cleared back stack and says why.
 */
@Singleton
class SessionExpiry @Inject constructor(private val tokens: TokenStore) {
    private val _expired = MutableStateFlow(false)
    val expired: StateFlow<Boolean> = _expired.asStateFlow()

    /**
     * "Please sign in again" on Welcome — kept on disk so a restart still says it, and a
     * flow so Welcome picks it up even when the refusal lands after it opened (cold start:
     * Splash can reach Welcome while the refresh is still in flight).
     */
    private val _notice = MutableStateFlow(tokens.sessionEndedNotice)
    val notice: StateFlow<Boolean> = _notice.asStateFlow()
    val showNotice: Boolean get() = _notice.value

    fun raise() { tokens.sessionEndedNotice = true; _notice.value = true; _expired.value = true }
    fun handled() { _expired.value = false }
    /** Signed in again: the note has done its job. */
    fun noticeShown() { tokens.sessionEndedNotice = false; _notice.value = false }
}

package com.neerly.mobile.feature.auth

import android.app.Activity
import android.app.Application
import android.os.Bundle
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/** Which sign-in the build uses: real Firebase Phone Auth, or the local backend's dev OTP. */
enum class AuthMode {
    FIREBASE, DEV;
    companion object {
        fun of(value: String): AuthMode = if (value.equals("firebase", ignoreCase = true)) FIREBASE else DEV
    }
}

/** Why Firebase refused — the view model turns these into words the customer understands. */
enum class PhoneAuthError { INVALID_CODE, INVALID_PHONE, TOO_MANY_REQUESTS, QUOTA, NETWORK, SESSION_EXPIRED, UNAVAILABLE }

class PhoneAuthException(val kind: PhoneAuthError, cause: Throwable? = null) : Exception(kind.name, cause)

sealed interface SendOutcome {
    /** The SMS is on its way; the customer types (or one-taps) the code. */
    data object CodeSent : SendOutcome
    /** Firebase proved the number without a code (instant verification); [idToken] is ready. */
    data class Verified(val idToken: String) : SendOutcome
    data class Failed(val error: PhoneAuthError) : SendOutcome
}

/**
 * The phone-number half of sign-in. A singleton, because the phone step and the code
 * step are separate screens (separate view models) that share one verification session.
 */
interface PhoneAuthGateway {
    suspend fun sendCode(phoneE164: String, resend: Boolean): SendOutcome
    /** Checks the typed code; returns the Firebase ID token for /auth/exchange. */
    suspend fun verifyCode(code: String): Result<String>
    /**
     * An ID token Firebase produced on its own after the code was sent (SMS auto-retrieval),
     * or null. Read it with [consumeAutoVerified] so it is used once.
     */
    val autoVerified: StateFlow<String?>
    fun consumeAutoVerified(): String?
    fun signOut()
}

@Singleton
class FirebasePhoneAuthGateway @Inject constructor(
    app: Application
) : PhoneAuthGateway, Application.ActivityLifecycleCallbacks {

    // Firebase needs an Activity for its Play Integrity / reCAPTCHA fallback.
    private var current = WeakReference<Activity>(null)
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private val auto = MutableStateFlow<String?>(null)
    override val autoVerified: StateFlow<String?> = auto.asStateFlow()

    init { app.registerActivityLifecycleCallbacks(this) }

    /** Null when the build has no google-services.json — sign-in then says it's unavailable. */
    private fun auth(): FirebaseAuth? = runCatching { FirebaseAuth.getInstance() }.getOrNull()

    override suspend fun sendCode(phoneE164: String, resend: Boolean): SendOutcome {
        val auth = auth() ?: return SendOutcome.Failed(PhoneAuthError.UNAVAILABLE)
        val activity = current.get() ?: return SendOutcome.Failed(PhoneAuthError.UNAVAILABLE)
        auto.value = null
        return suspendCancellableCoroutine { cont ->
            var answered = false
            fun answer(o: SendOutcome) { if (!answered && cont.isActive) { answered = true; cont.resume(o) } }
            val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id; resendToken = token
                    answer(SendOutcome.CodeSent)
                }
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    // Instant verification (before onCodeSent) or SMS auto-retrieval (after).
                    auth.signInWithCredential(credential).continueWithTask { it.result!!.user!!.getIdToken(true) }
                        .addOnSuccessListener { r ->
                            val token = r.token ?: return@addOnSuccessListener
                            // Kept for the code screen either way; it signs in once with it.
                            auto.value = token
                            answer(SendOutcome.Verified(token))
                        }
                        .addOnFailureListener { e -> answer(SendOutcome.Failed(classify(e))) }
                }
                override fun onVerificationFailed(e: FirebaseException) = answer(SendOutcome.Failed(classify(e)))
            }
            val options = PhoneAuthOptions.newBuilder(auth)
                .setPhoneNumber(phoneE164)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(callbacks)
                .apply { if (resend) resendToken?.let { setForceResendingToken(it) } }
                .build()
            PhoneAuthProvider.verifyPhoneNumber(options)
        }
    }

    override suspend fun verifyCode(code: String): Result<String> {
        val auth = auth() ?: return Result.failure(PhoneAuthException(PhoneAuthError.UNAVAILABLE))
        val id = verificationId ?: return Result.failure(PhoneAuthException(PhoneAuthError.SESSION_EXPIRED))
        return runCatching {
            val result = auth.signInWithCredential(PhoneAuthProvider.getCredential(id, code)).await()
            result.user!!.getIdToken(true).await().token!!
        }.recoverCatching { throw PhoneAuthException(classify(it), it) }
    }

    override fun consumeAutoVerified(): String? = auto.value.also { auto.value = null }

    override fun signOut() {
        verificationId = null; resendToken = null; auto.value = null
        auth()?.signOut()
    }

    private fun classify(e: Throwable): PhoneAuthError = when {
        e is PhoneAuthException -> e.kind
        e is FirebaseTooManyRequestsException -> PhoneAuthError.TOO_MANY_REQUESTS
        e is FirebaseNetworkException -> PhoneAuthError.NETWORK
        e is FirebaseAuthInvalidCredentialsException ->
            if (e.errorCode == "ERROR_INVALID_PHONE_NUMBER") PhoneAuthError.INVALID_PHONE else PhoneAuthError.INVALID_CODE
        e is FirebaseAuthException && e.errorCode == "ERROR_SESSION_EXPIRED" -> PhoneAuthError.SESSION_EXPIRED
        e is FirebaseAuthException && e.errorCode == "ERROR_QUOTA_EXCEEDED" -> PhoneAuthError.QUOTA
        e.message?.contains("quota", ignoreCase = true) == true -> PhoneAuthError.QUOTA
        else -> PhoneAuthError.UNAVAILABLE
    }

    override fun onActivityResumed(activity: Activity) { current = WeakReference(activity) }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) { current = WeakReference(activity) }
    override fun onActivityStarted(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) { if (current.get() === activity) current = WeakReference(null) }
}

/** Task → suspend, without pulling in kotlinx-coroutines-play-services for two calls. */
private suspend fun <T> com.google.android.gms.tasks.Task<T>.await(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { cont.resume(it) }
    addOnFailureListener { cont.resumeWith(Result.failure(it)) }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class PhoneAuthModule {
    @Binds abstract fun gateway(impl: FirebasePhoneAuthGateway): PhoneAuthGateway

    companion object {
        @dagger.Provides fun authMode(): AuthMode = AuthMode.of(com.neerly.mobile.BuildConfig.AUTH_MODE)
    }
}

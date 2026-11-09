package com.neerly.mobile.push

import android.app.KeyguardManager
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.neerly.mobile.MainActivity
import com.neerly.mobile.R
import com.neerly.mobile.data.repo.DriverRepository
import com.neerly.mobile.data.repo.VendorRepository
import com.neerly.mobile.feature.vendor.orders.AlertPlayer
import com.neerly.mobile.feature.vendor.orders.VendorAlertPrefsStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

/** Which ring is live, and which were answered (so a late time-out doesn't post "missed"). */
object RingSession {
    private val answered: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val _stopped = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val stopped: SharedFlow<String> = _stopped.asSharedFlow()

    fun markAnswered(tag: String) { answered += tag }
    fun wasAnswered(tag: String) = tag in answered
    /** Server cancelled (accepted on another phone, withdrawn) or answered from the shade. */
    fun stop(tag: String) { _stopped.tryEmit(tag) }
}

/**
 * Shared behaviour of the two ring screens: shows over the lock screen and turns the screen
 * on, counts down to [PushPayload.expiresAt], loops the sound, and turns into a "missed"
 * card at zero. Opened by the full-screen intent, or by a heads-up button with [EXTRA_ANSWER].
 */
abstract class RingActivityBase : ComponentActivity(), RingActivity {
    @Inject lateinit var notifier: PushNotifier
    @Inject lateinit var speaker: NoteSpeaker

    protected lateinit var payload: PushPayload
    protected lateinit var tag: String
    protected var notifId: Int = 0
    protected var deadlineMs: Long = 0
    protected val working = mutableStateOf(false)
    protected val problem = mutableStateOf<String?>(null)
    private var finished = false

    protected abstract val role: PushRole
    protected abstract fun startSound()
    protected abstract fun stopSound()
    protected abstract fun render()
    /** Accept / Take it. Returns the screen to open on success. */
    protected abstract suspend fun answerYes(orderId: String): String
    protected abstract suspend fun answerNo(orderId: String)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        @Suppress("UNCHECKED_CAST", "DEPRECATION")
        val map = intent.getSerializableExtra(EXTRA_PAYLOAD) as? HashMap<String, String>
        if (map == null) { finish(); return }
        payload = PushPayload.parse(map)
        tag = PushPlanner.tagFor(payload, PushType.of(payload.type))
        notifId = PushPlanner.idFor(tag)
        deadlineMs = payload.expiresAtMs ?: (System.currentTimeMillis() + (PushNotifier.DEFAULT_RING_MS[role] ?: 60_000L))
        if (deadlineMs <= System.currentTimeMillis() || RingSession.wasAnswered(tag)) { finish(); return }

        showOverLockScreen()
        lifecycleScope.launch { RingSession.stopped.collect { if (it == tag) close() } }

        when (intent.getStringExtra(EXTRA_ANSWER)) {
            PushAction.ACCEPT.name, PushAction.TAKE.name -> { render(); yes(); return }
        }
        startSound()
        render()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // A heads-up button pressed while the screen is already up.
        when (intent.getStringExtra(EXTRA_ANSWER)) {
            PushAction.ACCEPT.name, PushAction.TAKE.name -> yes()
        }
    }

    private fun showOverLockScreen() {
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }

    protected fun yes() {
        val orderId = payload["orderId"] ?: return close()
        if (working.value) return
        RingSession.markAnswered(tag)
        stopSound()
        working.value = true
        lifecycleScope.launch {
            runCatching { answerYes(orderId) }
                .onSuccess { link ->
                    notifier.cancel(tag, notifId)
                    notifier.cancelRingTimeout(notifId, payload, tag)
                    openApp(link, unlock = true)
                    close()
                }
                .onFailure {
                    notifier.cancel(tag, notifId)
                    working.value = false
                    problem.value = getString(R.string.push_ring_failed)
                    delay(2_500)
                    close()
                }
        }
    }

    protected fun no() {
        val orderId = payload["orderId"] ?: return close()
        RingSession.markAnswered(tag)
        stopSound()
        lifecycleScope.launch {
            runCatching { answerNo(orderId) }
            notifier.cancel(tag, notifId)
            notifier.cancelRingTimeout(notifId, payload, tag)
            close()
        }
    }

    /** The clock ran out on screen. */
    protected fun timedOut() {
        if (RingSession.wasAnswered(tag)) return
        notifier.postMissed(payload)
        notifier.cancelRingTimeout(notifId, payload, tag)
        close()
    }

    protected fun openApp(link: String, unlock: Boolean) {
        val open = Intent(this, MainActivity::class.java).setAction(Intent.ACTION_VIEW).setData(Uri.parse(link))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val km = getSystemService(KeyguardManager::class.java)
        if (unlock && km?.isKeyguardLocked == true && Build.VERSION.SDK_INT >= 26) {
            // The order screen holds customer details — ask for the unlock first.
            km.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() { startActivity(open) }
            })
        } else startActivity(open)
    }

    protected fun close() {
        if (finished) return
        finished = true
        stopSound()
        finish()
    }

    override fun onDestroy() {
        stopSound()
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PAYLOAD = "push.payload"
        const val EXTRA_ANSWER = "push.answer"
    }
}

/** NPVRing — a new order rings like a phone call for 3 minutes. */
@AndroidEntryPoint
class SellerRingActivity : RingActivityBase() {
    @Inject lateinit var vendors: VendorRepository
    @Inject lateinit var alertPrefs: VendorAlertPrefsStore
    private var player: AlertPlayer? = null

    override val role = PushRole.SELLER

    override fun startSound() {
        // Ringtone-style: loud in normal mode, vibrates on silent, never through DND (A2).
        player = AlertPlayer(applicationContext).also {
            it.start(alertPrefs.load().copy(ringOnSilent = false, repeatUntilAnswered = true))
        }
    }

    override fun stopSound() { player?.stop(); player = null }

    override suspend fun answerYes(orderId: String): String {
        vendors.accept(orderId)
        return "${DeepLinks.SCHEME}seller/order/$orderId"
    }

    /** "Can't take" is never a dead end: it opens the reason sheet (NPVRing note 4). */
    override suspend fun answerNo(orderId: String) {
        openApp("${DeepLinks.SCHEME}seller/new-order/$orderId?decline=1", unlock = true)
    }

    override fun render() {
        setContent {
            var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(Unit) {
                while (true) {
                    now = System.currentTimeMillis()
                    if (now >= deadlineMs) { timedOut(); break }
                    delay(250)
                }
            }
            SellerRingScreen(
                p = payload,
                leftMs = (deadlineMs - now).coerceAtLeast(0),
                totalMs = PushNotifier.DEFAULT_RING_MS.getValue(PushRole.SELLER),
                working = working.value,
                problem = problem.value,
                onAccept = ::yes,
                onDecline = ::no
            )
        }
    }
}

/** NPDRing — a new task rings for 45 seconds, readable at arm's length in the sun. */
@AndroidEntryPoint
class DriverRingActivity : RingActivityBase() {
    @Inject lateinit var drivers: DriverRepository
    private var tone: ToneGenerator? = null

    override val role = PushRole.DRIVER

    override fun startSound() {
        // Short horn-like tone, 3 times, then the task read aloud.
        lifecycleScope.launch {
            tone = runCatching { ToneGenerator(AudioManager.STREAM_RING, 100) }.getOrNull()
            repeat(3) {
                tone?.startTone(ToneGenerator.TONE_PROP_BEEP2, 400)
                delay(650)
            }
            tone?.release(); tone = null
            val pay = payload["pay"] ?: payload["amount"]
            val area = payload["dropArea"] ?: ""
            if (pay != null) speaker.speak(getString(R.string.push_tts_new_task, pay, area))
        }
    }

    override fun stopSound() {
        tone?.release(); tone = null
        speaker.stop()
    }

    override suspend fun answerYes(orderId: String): String {
        drivers.acceptOffer(orderId)
        return "${DeepLinks.SCHEME}driver"
    }

    override suspend fun answerNo(orderId: String) { drivers.passOffer(orderId) }

    override fun render() {
        setContent {
            var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
            LaunchedEffect(Unit) {
                while (true) {
                    now = System.currentTimeMillis()
                    if (now >= deadlineMs) { timedOut(); break }
                    delay(250)
                }
            }
            DriverRingScreen(
                p = payload,
                leftMs = (deadlineMs - now).coerceAtLeast(0),
                totalMs = PushNotifier.DEFAULT_RING_MS.getValue(PushRole.DRIVER),
                working = working.value,
                problem = problem.value,
                onTake = ::yes,
                onPass = ::no
            )
        }
    }
}

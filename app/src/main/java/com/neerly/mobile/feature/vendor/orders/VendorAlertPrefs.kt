package com.neerly.mobile.feature.vendor.orders

import com.neerly.mobile.R
import android.content.Context
import android.media.AudioAttributes
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Board V44 — kept on this phone only, as the board says ("Saved on this phone"). */
data class AlertPrefs(
    val ringOnSilent: Boolean = true,
    val repeatUntilAnswered: Boolean = true,
    val vibrate: Boolean = true,
    val sound: AlertSound = AlertSound.Alarm
)

enum class AlertSound(@androidx.annotation.StringRes val label: Int, val ringType: Int) {
    Alarm(R.string.vend_snd_alarm, RingtoneManager.TYPE_ALARM),
    Ringtone(R.string.vend_snd_ringtone, RingtoneManager.TYPE_RINGTONE),
    Chime(R.string.vend_snd_chime, RingtoneManager.TYPE_NOTIFICATION)
}

@Singleton
class VendorAlertPrefsStore @Inject constructor(@ApplicationContext private val context: Context) {
    private val prefs get() = context.getSharedPreferences("vendor_alert", Context.MODE_PRIVATE)

    fun load() = AlertPrefs(
        ringOnSilent = prefs.getBoolean("silent", true),
        repeatUntilAnswered = prefs.getBoolean("repeat", true),
        vibrate = prefs.getBoolean("vibrate", true),
        sound = runCatching { AlertSound.valueOf(prefs.getString("sound", AlertSound.Alarm.name)!!) }.getOrDefault(AlertSound.Alarm)
    )

    fun save(p: AlertPrefs) = prefs.edit()
        .putBoolean("silent", p.ringOnSilent).putBoolean("repeat", p.repeatUntilAnswered)
        .putBoolean("vibrate", p.vibrate).putString("sound", p.sound.name).apply()

    /** Orders this phone has already rung for, so polling doesn't ring twice. */
    fun alerted(orderId: String): Boolean = prefs.getStringSet("alerted", emptySet())!!.contains(orderId)
    fun markAlerted(orderId: String) {
        val set = prefs.getStringSet("alerted", emptySet())!!.toMutableSet().apply { add(orderId) }
        prefs.edit().putStringSet("alerted", set.toList().takeLast(200).toSet()).apply()
    }
}

/** Plays the alert the way the prefs say; [stop] when the order is answered or times out. */
class AlertPlayer(private val context: Context) {
    private var ringtone: Ringtone? = null

    fun start(p: AlertPrefs) {
        stop()
        val uri = RingtoneManager.getDefaultUri(p.sound.ringType) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        ringtone = RingtoneManager.getRingtone(context, uri)?.apply {
            // Alarm usage plays through silent mode, like the board's "Uses alarm volume".
            audioAttributes = AudioAttributes.Builder()
                .setUsage(if (p.ringOnSilent) AudioAttributes.USAGE_ALARM else AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            if (Build.VERSION.SDK_INT >= 28) isLooping = p.repeatUntilAnswered
            play()
        }
        if (p.vibrate) vibrator()?.let { v ->
            val pattern = longArrayOf(0, 600, 400, 600, 400, 600)
            val repeat = if (p.repeatUntilAnswered) 0 else -1
            if (Build.VERSION.SDK_INT >= 26) v.vibrate(VibrationEffect.createWaveform(pattern, repeat))
            else @Suppress("DEPRECATION") v.vibrate(pattern, repeat)
        }
    }

    fun stop() {
        ringtone?.stop(); ringtone = null
        vibrator()?.cancel()
    }

    @Suppress("DEPRECATION")
    private fun vibrator(): Vibrator? = if (Build.VERSION.SDK_INT >= 31)
        (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
    else context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
}
